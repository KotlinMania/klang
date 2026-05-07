# Type-conversion audit (2026-05-08)

Audience: anyone who modifies klang's hot paths (`mem/`, `bitwise/`,
`fp/`, `int/`, `math/`).

This document records a project-wide audit of unnecessary type conversions
— places where klang was converting between numeric types when the intent
was reachable via a single shorter chain or a different API shape.

The audit was triggered by the `LongArray` substrate work (see
[`why-longarray-storage.md`](./why-longarray-storage.md)). Once the
storage substrate question was settled, the next question was: are we
shuffling types around at the type-system boundary in ways that aren't
buying us anything?

---

## Summary

| Tier | Findings | Action |
|---|---|---|
| 1 — Safe simplifications, zero semantic change | 7 sites in 4 files | Applied in this commit |
| 2 — API-shape candidates, real performance win, more invasive | 2 design questions | Documented for decision |
| 3 — Looks suspicious but is correct | 3 patterns | Documented to forestall future "fixes" |

Six conversions removed in tier 1. Tier 2 captures two design questions that should be discussed before action. Tier 3 documents intentional patterns so they're not "improved" by mistake.

---

## Tier 1 — applied in this commit

These are simplifications that produce identical bit patterns with fewer
intermediate operations. Pure code-quality wins, no behavior change,
verified by the existing test suite (`./gradlew macosArm64Test` — 439/0/0).

### 1. `Long.toInt().toShort()` → `Long.toShort()`

`Long.toShort()` is defined to return the low 16 bits of the `Long`
value, truncating the upper 48 bits. `Long.toInt().toShort()` produces
exactly the same bits — the `.toInt()` step truncates to the low 32 bits
and the subsequent `.toShort()` truncates to the low 16, so the upper
narrowing is wasted work.

Sites fixed:

| File | Line | Context |
|---|---:|---|
| `bitwise/BitShiftEngine.kt` | 1410 | `leftShift(buffer, addr, bits)` 16-bit branch |
| `bitwise/BitShiftEngine.kt` | 1436 | `rightShift(buffer, addr, bits)` 16-bit branch |
| `bitwise/BitShiftEngine.kt` | 1462 | `unsignedRightShift(buffer, addr, bits)` 16-bit branch |
| `bitwise/BitShiftEngine.kt` | 1500 | `store(buffer, addr, value: Long)` 16-bit branch |
| `mem/PackedBuffer.kt` | 102 | `getShort(addr)` aligned-within-one-Long path |

### 2. `Short.toInt().toLong()` → `Short.toLong()`

`Short.toLong()` is the direct sign-extending widening. The intermediate
`.toInt()` does a sign-extend to 32 bits, then `.toLong()` does another
sign-extend to 64. Removing the middle step fuses the two extensions
into one — same semantics, fewer ops.

Sites fixed:

| File | Line | Context |
|---|---:|---|
| `internal/runtime/Runtime.kt` | 125–126 | `sh(ptr, value: Short)` writing low and high bytes |
| `fp/CBF16.kt` | 62 | `isNaN()` exponent extraction |
| `fp/CBF16.kt` | 68 | `isInf()` exponent extraction |

---

## Empirical: tier 1 vs tier 2, measured

A `TypeConversionBenchmark` was added to the benchmark suite covering
all three tiers (`src/commonBenchmark/.../mem/TypeConversionBenchmark.kt`).
The numbers turn most of the discussion into numbers.

Run on macOS arm64, Kotlin/Native 2.3.21, kotlinx-benchmark 0.4.16,
AverageTime mode, 3 warmup × 5 measurement × 1 second iterations,
1024-element sample sweeps:

```
Tier 1 — round-trip vs direct
  longToShortDirect            294.578 ns/op
  longToShortViaInt            296.563 ns/op   ~0.7% slower
  longToByteDirect             294.192 ns/op
  longToByteViaInt             291.873 ns/op   (within noise)
  shortToLongDirect            288.358 ns/op
  shortToLongViaInt            294.229 ns/op   ~2% slower
  byteToLongDirect             292.313 ns/op
  byteToLongViaInt             291.513 ns/op   (within noise)

Tier 2-A — PackedBuffer.setShort signature
  setShortViaShortParameter   2210.953 ns/op
  setShortViaIntParameter     2064.940 ns/op   ~7% faster (CIs disjoint)

Tier 2-B — engine Long-boundary cost
  directIntShiftNoBoundary       301.443 ns/op
  engineShiftViaLongBoundary   51448.200 ns/op   ~170× slower
  directIntUshrNoBoundary        539.217 ns/op
  engineUshrViaLongBoundary    22486.110 ns/op   ~42× slower
```

What the numbers mean:

- **Tier 1 is a code-clarity win, not a speed win.** The compiler
  peephole-optimises the redundant intermediate `Int` on Kotlin/Native
  macOS arm64, so removing the round-trip doesn't materially change
  runtime. The audit's simplifications still hold — they don't
  *regress* anything either — but the value is readability, not
  measurable speed.

- **Tier 2-A is a real ~7% win** on the hot 16-bit-store path. Modest
  but repeatable, with disjoint confidence intervals. Worth landing.

- **Tier 2-B is a 40–170× cliff.** The engine's per-call overhead —
  `ShiftResult` data-class allocation, mode branch, `Long` widen/narrow —
  dwarfs the actual shift work on Kotlin/Native. Any hot path that
  calls `engine.leftShift(value.toLong(), n).value.toInt()` repeatedly
  is paying a multi-order-of-magnitude tax that an `Int`-overload set
  could avoid. This isn't a "shave a few percent" question; it's a
  serious performance issue for shift-heavy code.

The empirical results promote both Tier-2 items from "design questions"
to "next PRs."

---

## Tier 2 — design questions (now empirically justified)

Both proposals would meaningfully simplify call sites or remove
per-call overhead. The benchmark numbers above put them on firm
footing.

### A. `PackedBuffer.setShort(addr, value: Short)` should probably take `Int`

Current shape:

```kotlin
// PackedBuffer.kt
fun setShort(addr: Int, value: Short) {
    ...
    val v = value.toInt() and 0xFFFF        // widen + mask, internal
    ...
}

fun setByte(addr: Int, value: Int) {        // already takes Int!
    ...
    val v = value and 0xFF
    ...
}
```

Caller side (`F16Math.kt`, repeatedly):

```kotlin
GlobalHeap.sh(destAddr, resultBits.toShort())   // narrow Int -> Short ...
// then internally setShort widens Short -> Int again with sign-extend + mask.
```

Net: every 16-bit store does `Int → Short → Int`. The intermediate
`Short` is a type-system stop where no work happens.

**Proposal:** make `setShort(addr: Int, value: Int)` mirror `setByte(addr: Int, value: Int)` — caller passes an `Int` that holds the low 16 bits, the function uses `value and 0xFFFF` internally. Same behaviour, but the round-trip disappears.

**Affects:** `PackedBuffer.setShort`, `GlobalHeap.sh` (the `expect`
declaration in `mem/GlobalHeap.kt`), the two `actual`s in `nativeMain`
and `jsMain`, and ~20 call sites across `F16Math.kt`,
`Float128Math.kt`, `BitShiftEngine.kt`, plus tests.

**Why not in this commit:** API surface change, deserves its own PR with
full call-site sweep. The 16-bit store path is hot in `F16Math` (every
half-precision store); removing the redundant `Int → Short → Int`
through-and-back is likely a measurable win on the `loadWord`-style
benchmarks.

---

### B. `BitShiftEngine` could expose `Int`-overload shifts for 32-bit operands

Current shape:

```kotlin
// BitShiftEngine.kt — only Long-shaped public API for shifts
data class ShiftResult(val value: Long, val carry: Long = 0, val overflow: Boolean = false)
fun leftShift(value: Long, bits: Int): ShiftResult
fun rightShift(value: Long, bits: Int): ShiftResult
fun unsignedRightShift(value: Long, bits: Int): ShiftResult
```

Caller side (very common, especially in `CBF16.kt`, `HexShift.kt`,
`BitwiseOps.kt`, `BitPrimitives.kt`):

```kotlin
val out = engine.leftShift(intValue.toLong(), 7).value.toInt() and 0xFF
//                          ^^^^ widen ^^^^             ^^^^ narrow ^^^^
```

Every shift on a 32-bit operand pays:
- `Int.toLong()` (widening, sign-extending)
- the engine's internal Long arithmetic
- `Long.toInt()` (narrowing, truncating)

For tight loops doing many shifts on 32-bit data, this is a real cost.

**Proposal:** add an `Int`-shaped overload set:

```kotlin
data class IntShiftResult(val value: Int, val carry: Int = 0, val overflow: Boolean = false)
fun leftShift32(value: Int, bits: Int): IntShiftResult
fun rightShift32(value: Int, bits: Int): IntShiftResult
fun unsignedRightShift32(value: Int, bits: Int): IntShiftResult
```

Where the body short-circuits the Long path when `bitWidth == 32`.

Naming a separate suffix (`leftShift32`) avoids overload-resolution
ambiguity for callers and makes the bit-width explicit at the call site
(matching the engine's existing "bit width is part of the engine
identity" design).

**Why not in this commit:** it's a real API-surface addition, needs
benchmark backing to confirm the win, and it interacts with the
`ShiftResult.carry` semantics (which currently uses `Long` — would the
`Int`-shaped result keep `Int` carry, or share the existing `Long`
carry?). Worth a focused PR with a benchmark.

**Estimate of impact:** ~30 call sites across the project would
simplify. The hottest is `Float32Math` / `Float16Math` / `BitwiseOps`
where shifts dominate the inner loops.

---

## Tier 3 — patterns that look wrong but aren't

Documented here so future "cleanup" PRs don't get tempted.

### `.toLong() and 0xFF` instead of `.toInt() and 0xFF`

Pattern (e.g. `Runtime.kt:120`, repeated in many helpers):

```kotlin
engine32.leftShift(lbu(ptr).toLong(), 0).value
//                  ^^^^ Int -> Long widen, intentional
```

`lbu` returns `Int`. The `.toLong()` here is *not* a redundant
round-trip — it's the engine API requirement. The engine takes `Long`
operands, and the widening is sign-safe (positive `Int` widens to
positive `Long`, no sign issue). Tier 2-B above is the proposal that
would eliminate this; without that, the widening is the API.

### `(bits and 0xFF).toUByte()` in `CE8M0`/`CUE4M3`

```kotlin
fun toUByte(): UByte = (bits and 0xFF).toUByte()
```

Where `bits` is the `Int`-typed internal storage. The `and 0xFF` is
documenting intent — even though `Int.toUByte()` would truncate to the
low 8 bits anyway, the explicit mask makes the byte-width assumption
visible at the call site. The cost is one AND with a constant, vanishing
in any modern codegen. Keep as-is; it's documentation, not overhead.

### `HeapUInt128.toIntArray()` allocations in `Float128Math`

Pattern in `Float128Math.{negateBits, absBits}`:

```kotlin
fun negateBits(bits: HeapUInt128): HeapUInt128 {
    val limbs = bits.toIntArray()                              // allocates
    limbs[SwAR128.LIMB_COUNT - 1] = limbs.last() xor (1 shl 15)
    return HeapUInt128.fromLimbsUnsafe(limbs)
}
```

`HeapUInt128.toIntArray()` reads from the global heap and allocates an
`IntArray` snapshot. This *is* an allocation per call, but it's
intentional — `negateBits` and `absBits` are read-modify-write
operations on heap-backed storage. The snapshot lets us mutate locally
and reconstruct without touching the original. An in-place variant
would be a different API contract (mutate-in-place vs. functional).

Keep as-is; this is the functional contract.

---

## How the audit was done

For reproducibility:

```bash
cd /Volumes/stuff/Projects/kotlinmania/klang

# Round-trip same-line conversion chains
grep -rEn '\.toInt\(\)\.toShort\(\)|\.toInt\(\)\.toByte\(\)|\.toInt\(\)\.toLong\(\)|\.toLong\(\)\.toInt\(\)|\.toShort\(\)\.toInt\(\)|\.toByte\(\)\.toInt\(\)|\.toUByte\(\)\.toInt\(\)|\.toInt\(\)\.toUByte\(\)' \
    src/commonMain --include='*.kt'

# Triple chains (any three .toX().toY().toZ())
grep -rEn '\.to[A-Z][a-z]+\(\)\.to[A-Z][a-z]+\(\)\.to[A-Z][a-z]+\(\)' \
    src/commonMain --include='*.kt'

# Widen+narrow at engine boundary
grep -rEn '\.toLong\(\),.*\)\.value\.toInt\(\)' \
    src/commonMain --include='*.kt'

# Narrow-then-widen at API boundaries
grep -rEn '\.toShort\(\)\)' src/commonMain --include='*.kt' \
    | grep -E 'GlobalHeap\.sh|setShort'
```

The grep recipes plus a manual scan of the hot files (`bitwise/`,
`fp/`, `mem/`, `int/`, `math/`) produced this audit in roughly an
hour. Worth re-running periodically — especially after any `expect` /
`actual` API change in the heap or engine.

---

## Cross-references

- [`why-longarray-storage.md`](./why-longarray-storage.md) — the
  storage-substrate decision this audit is downstream of.
- [`philosophy-and-design.md`](./philosophy-and-design.md) — the
  forbidden-practices rules. Type-shuffling at API boundaries doesn't
  violate any rule, but it does waste the discipline the rules are
  meant to enforce.
- The benchmark suite in
  `src/commonBenchmark/kotlin/io/github/kotlinmania/klang/` — the
  empirical lever for evaluating Tier 2 proposals.

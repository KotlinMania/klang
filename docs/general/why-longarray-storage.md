# Why klang stores everything in `LongArray`

Status: ground truth, with empirical backing as of 2026-05-08.
Audience: anyone who looks at klang and wonders why the heap is built on `LongArray` rather than `ByteArray` / `UByteArray`.

This document captures, in detail, the reasoning behind klang's storage substrate. It exists because the choice has been re-litigated multiple times by people who reasonably ask "why don't we just use `UByteArray` / `ByteArray`?" — and because the answer is non-obvious until you've read the stdlib source. The aim is that anyone reading this document gets the same answer the author would, without having to re-do the investigation.

---

## TL;DR

klang's `GlobalHeap` is backed by `PackedBuffer`, which holds a `LongArray`. Bytes are an *addressing convention*, not a storage shape. `lb`/`lbu`/`lh`/`lw`/`ld` and friends are derived views that compute `idx = addr ushr 3` and `shift = (addr and 7) shl 3` over the underlying `LongArray`.

The choice is forced by the Kotlin language and stdlib, not by performance optimisation alone — though the performance margin is significant (≈ 2× over `ByteArray + and 0xFF` masking on Kotlin/Native macOS arm64).

The three reasons, in increasing order of how much code-reading they require to confirm:

1. **`Byte` and `Short` have no `shl`, `shr`, or `ushr` operators in stdlib.** You cannot shift a `Byte` directly. You have to convert it to `Int` first, and `Byte.toInt()` is the *sign-extending* widening conversion. So a naive `byte shl 8` corrupts the result for any byte with the high bit set (50% of all possible byte values).
2. **`UByteArray` looks like the answer but isn't.** `UByteArray.get(i)` is *not* `inline` (the value class implements `Collection<UByte>`, which forces a real method). `UByte.toInt()` is *literally* `data.toInt() and 0xFF` — exactly the masking work it appears to save you, plus a function-call boundary on every read.
3. **`LongArray` has none of these problems.** `Long.shl/shr/ushr` are stdlib operators returning `Long` (no type promotion, no sign-extension exposure unless you explicitly opt in via `shr` instead of `ushr`), and a single `Long` carries 8 bytes — so a 4-byte little-endian read is one indexed array access plus a couple of shifts and a mask, never four byte loads plus four masks plus three shifts plus three ORs.

The benchmark numbers below confirm: on the same workload, `LongArray + ushr/and` is **1.97× faster than `ByteArray + and 0xFF`** and **2.23× faster than `UByteArray`**.

---

## Why we looked

The investigation started with a single pair of benchmark numbers in the migrated `UByteArrayBenchmark` (kotlinx-benchmark, macOS arm64, Kotlin/Native 2.3.21):

```
loadWordSweepSigned     4.265 ± 0.013  µs/op   ← ByteArray + and 0xFF
loadWordSweepUnsigned   4.846 ± 0.017  µs/op   ← UByteArray (stdlib)
```

That's the opposite of what every "use `UByteArray`, no mask needed" piece of advice predicts. Two paths to the same `Int` bit pattern produced different machine code, and the *cleaner-looking* one was the slower one. Confidence intervals didn't overlap. The result was robust across runs.

The question — "why is `UByteArray` slower than `ByteArray + and 0xFF` on Kotlin/Native?" — has a 100% concrete answer in the v2.3.21 stdlib source, which we mirror at `/Volumes/stuff/Projects/kotlinmania/kotlin.coroutines-cpp/tmp/kotlin/`. What follows is the line-by-line tour.

---

## Where the information lives in the Kotlin v2.3.21 source

All paths below are relative to the v2.3.21 mirror. The exact file paths are recorded so the next person reading this document can verify the claims directly without re-cloning.

### 1. `Byte`, `Short`, `Int`, `Long` declarations

**File:** `libraries/stdlib/src/kotlin/Primitives.kt`

This is the `expect` declaration of the four signed integer primitives. Bit-shift methods (`shl`, `shr`, `ushr`, `inv`, the infix `and`/`or`/`xor`) are declared **inside the `Int` class body** (lines 1051, 1060, 1069, 1085) and inside the `Long` class body (lines 1465, 1474, 1483, 1499) — and **nowhere else**.

A direct grep against the file:

```
$ grep -n 'shl\|shr\|ushr\|inv()\|fun.*and\|fun.*or\|xor' libraries/stdlib/src/kotlin/Primitives.kt
1051:    public infix fun shl(bitCount: Int): Int      ← Int.shl
1060:    public infix fun shr(bitCount: Int): Int      ← Int.shr
1069:    public infix fun ushr(bitCount: Int): Int     ← Int.ushr
1071:    public infix fun and(other: Int): Int         ← Int.and
1075:    public infix fun or(other: Int): Int          ← Int.or
1079:    public infix fun xor(other: Int): Int         ← Int.xor
1085:    public fun inv(): Int                          ← Int.inv
1465:    public infix fun shl(bitCount: Int): Long     ← Long.shl
1474:    public infix fun shr(bitCount: Int): Long     ← Long.shr
1483:    public infix fun ushr(bitCount: Int): Long    ← Long.ushr
1499:    public fun inv(): Long                         ← Long.inv
…
```

The `Byte` class spans roughly lines 12–381. Within that range, **there are no `shl`, `shr`, `ushr`, `inv`, `and`, `or`, or `xor` methods**. The same is true of `Short` (≈ lines 382–720). All bitwise operations on `Byte`/`Short` are external — and require promotion to `Int` first.

### 2. The "experimental" bitwise extensions on `Byte`/`Short`

**File:** `libraries/stdlib/src/kotlin/experimental/bitwiseOperations.kt`

The full file is 47 lines. It defines `and`, `or`, `xor`, `inv` on `Byte` and on `Short`. Notably:

```kotlin
public inline infix fun Byte.and(other: Byte): Byte =
    (this.toInt() and other.toInt()).toByte()

public inline fun Byte.inv(): Byte =
    (this.toInt().inv()).toByte()
```

Two observations:

1. There is **no `shl`, `shr`, or `ushr` extension** on `Byte` here either. The full set of bitwise operations available on `Byte` in stdlib is `and`/`or`/`xor`/`inv`. To shift, you must `toInt()`.
2. Even the `and`/`or`/`xor`/`inv` extensions internally promote to `Int` via `this.toInt()`. The promotion is sign-extending (see next section). For `and`/`or`/`xor` specifically, the sign extension cancels out across both operands, so the result narrowed back to `Byte` is correct. For shifts, no such cancellation — and no shift extension is even provided.

### 3. `Byte.toInt()` is the sign-extending widening conversion

`Byte.toInt()` is declared as part of the `Number` superclass contract and is *the* numeric-widening primitive. The platform-specific actuals in `kotlin-native/runtime/src/main/kotlin/kotlin/Primitives.kt` (and the JS/JVM equivalents) implement it as the standard sign-extending conversion: bit 7 of the `Byte` is replicated into bits 8–31 of the `Int`. This is the well-known C/Java behaviour for `(int)signedByte`.

Concretely:

```
val b: Byte = 0x80.toByte()   // -128 as a Byte (binary 10000000)
b.toInt()                     // -128 as Int = 0xFFFFFF80   ← sign-extended
b.toInt() shl 8               // 0xFFFF8000                  ← all upper bits flooded
(b.toInt() and 0xFF) shl 8    // 0x00008000                  ← actual intent
```

So a naive `byte shl 8` does the wrong thing for every byte with the high bit set — that's `0x80..0xFF`, which is **half** of all possible byte values. Every algorithm ported from C — where `unsigned char` zero-extends by definition, by language spec — silently corrupts unless the porter inserts `and 0xFF` masks at every boundary.

This is why klang's earlier convention rule (in `philosophy-and-design.md`) reads:

> FORBIDDEN: Raw Kotlin bitwise operators outside BitShiftEngine.
> FORBIDDEN: Hard-coded masks (0xFF, 0xFFFF, etc.).

It's not a stylistic rule. It's the only way to defend against the implicit sign-extension — every shift either goes through `BitShiftEngine` (which knows the bit width and applies the right mask), or you've written a bug.

### 4. `UByte` is a thin wrapper over `Byte`

**File:** `libraries/stdlib/unsigned/src/kotlin/UByte.kt`

```kotlin
@SinceKotlin("1.5")
@JvmInline
public value class UByte @kotlin.internal.IntrinsicConstEvaluation @PublishedApi
internal constructor(@PublishedApi internal val data: Byte) : Comparable<UByte> {
```

`UByte` is a `@JvmInline value class` whose only field is `internal val data: Byte`. At runtime on Native and JS the wrapper is erased; the storage *is* a `Byte`. The crucial line is `UByte.toInt()` at line 331:

```kotlin
public inline fun toInt(): Int = data.toInt() and 0xFF
```

This is **exactly the masking** that the "signed" path does explicitly. There is no magic. `UByte.toInt()` performs the same `Byte.toInt()` (sign-extending) followed by `and 0xFF` (mask off the sign-extension bits). The bit-pattern result is identical to `(byte.toInt() and 0xFF)`. So at the leaf-level operation, there is no work saved.

`Byte.toUByte()` at line 414:

```kotlin
public inline fun Byte.toUByte(): UByte = UByte(this)
```

— which is just the inline-class wrap.

### 5. `UByteArray.get` is **not** `inline`

**File:** `libraries/stdlib/unsigned/src/kotlin/UByteArray.kt`

```kotlin
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@JvmInline
public value class UByteArray
@PublishedApi
internal constructor(@PublishedApi internal val storage: ByteArray) : Collection<UByte> {

    public operator fun get(index: Int): UByte = storage[index].toUByte()
```

Two facts kill the performance:

1. **`get` is not `inline`.** It cannot be: `UByteArray` implements `Collection<UByte>`, and methods that participate in virtual dispatch can't be inline. So every `arr[i]` on a `UByteArray` is a real function call — a stack frame, an argument register set up, a return — for a single byte read. On Kotlin/Native that's measurable.

2. **The internal `storage` is `@PublishedApi internal`.** From outside the `kotlin` module, regular code can't reach `arr.storage[i]` directly. So callers have no way to bypass the function-call boundary while staying within stdlib types.

Combine the two and you get this expansion of an "unsigned" word read:

```
arr[addr].toInt()
=> UByteArray.get(addr).toInt()                      // function call (UByteArray.get)
=> (storage[addr].toUByte()).toInt()                  // UByteArray.get body
=> (UByte(storage[addr])).toInt()                     // Byte.toUByte body
=> UByte(storage[addr]).data.toInt() and 0xFF         // UByte.toInt body
```

After inline-class erasure on Native, the inner `UByte(byte).data` is just `byte`, so semantically this collapses to:

```
storage[addr].toInt() and 0xFF
```

— **exactly what the signed path does explicitly**, plus a function-call boundary at the `UByteArray.get` level. Same fundamental work, plus overhead. The benchmark confirms this is a measurable ≈ 14% penalty on Kotlin/Native macOS arm64 in tight loops.

### 6. `@JvmInline` is JVM-only

**File:** `compiler/fir/checkers/checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/jvm/checkers/declaration/FirJvmInlineApplicabilityChecker.kt`

The diagnostic that fires when you write `value class X(val v: T)` without `@JvmInline` is `VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION`. It's defined and reported only by the **JVM-side** FIR checker (file path includes `checkers.jvm/`). On Native and JS, plain `value class X(val v: T)` is legal — no annotation required.

Klang has **no JVM target** (Native + JS only). So when klang declares its own narrow types (e.g. `C_UInt8`, `KUByteArray`), they should be plain `value class`, not `@JvmInline value class`. The annotation is JVM-flavored decoration we don't need.

---

## What this means at the storage level

If you use `ByteArray` as the substrate for a memory model:

- Every `arr[i]` returns a `Byte`. `Byte.toInt()` sign-extends. So every read needs `and 0xFF` to recover the C-equivalent unsigned-byte value.
- Every shift requires conversion to `Int`. `Byte`/`Short` have no shift operators of their own.
- 4-byte little-endian word load = 4 array reads + 4 sign-extends + 4 AND-masks + 3 shifts + 3 ORs = 14 operations against the array, plus the implicit type promotions.

If you use `UByteArray` as the substrate:

- `arr[i]` is a function call (non-`inline` `get`). Every byte read is a stack frame.
- `UByte.toInt()` is `data.toInt() and 0xFF` — same masking work the `ByteArray` path does explicitly.
- Same 14 logical operations, plus a function-call boundary on each of the 4 array reads.

If you use `LongArray` as the substrate (klang's choice):

- `arr[i]` returns a `Long`. `Long.shl/shr/ushr` are stdlib operators returning `Long`. **No type promotion involved.**
- One `Long` is 8 bytes. A 4-byte little-endian word read at byte address `addr` is:

  ```
  val idx   = addr ushr 3              // which Long slot
  val shift = (addr and 7) shl 3       // bit offset within the slot
  val word  = ((arr[idx] ushr shift) and 0xFFFF_FFFFL).toInt()
  ```

  i.e. one array read, one shift, one mask, one narrowing — *if* the word doesn't straddle a slot boundary. If it does, two array reads plus a stitch step. Both cases are dramatically less work than the byte-shape options.

- No `Byte` ever appears. The sign-extension hazard is gone by construction.

---

## Empirical: three storage shapes, same work

The three benchmarks live in `src/commonBenchmark/kotlin/io/github/kotlinmania/klang/mem/UByteArrayBenchmark.kt`. Each does the same logical work — sweep a 15,360-byte buffer in 4-byte windows, accumulate via XOR — varying only the storage container.

Run on macOS arm64, Kotlin/Native 2.3.21, kotlinx-benchmark 0.4.16, AverageTime mode, 3 warmup × 5 measurement × 1 second iterations:

```
loadWordSweepLongArray   2.172 ± 0.008  µs/op   ← LongArray + Long.ushr/and
loadWordSweepSigned      4.274 ± 0.018  µs/op   ← ByteArray + and 0xFF
loadWordSweepUnsigned    4.846 ± 0.040  µs/op   ← stdlib UByteArray
```

| Variant | Speedup over `ByteArray + mask` |
|---|---:|
| `LongArray + Long.ushr/and` | **1.97×** |
| `ByteArray + and 0xFF` | 1.00× |
| `UByteArray` | 0.88× |

Confidence intervals do not overlap. The ranking is robust.

---

## Why this is the hard floor, not a peephole optimisation

Three things make this measurable difference structural rather than incidental:

1. **Stdlib-level API gap.** `Byte`/`Short` have *no* shift operators in stdlib, and no straightforward way to add efficient ones via extension functions because the compiler can't intrinsify user extensions the way it intrinsifies primitive operators. So byte-level shifts will always involve a `toInt()` round-trip.

2. **Inline-class boundary on a non-`inline` getter.** `UByteArray.get` cannot be `inline` while implementing `Collection<UByte>`. JetBrains would have to break the `Collection` contract to fix this, which they won't do (binary compatibility, ABI stability). It is unlikely to change in any near-future Kotlin release.

3. **Native code generation quality.** On Kotlin/Native arm64, the LLVM backend produces tight code for sequences expressible as `Long` loads + shifts (single `LDR` + `LSR` + `AND` instructions). Sequences that go through inline-class wraps and function-call boundaries do *not* always get peephole-optimised away — at least not in 2.3.21. The benchmark catches the difference.

The implication: this is a property of Kotlin (the language and runtime), not of klang. Future Kotlin versions might shift the numbers, but the architecture klang chose — `LongArray` storage, byte addressability via shift-and-mask — is robust to those changes. Worst case the gap shrinks; best case the gap widens. Either way, klang doesn't move.

---

## What klang does as a result

### `PackedBuffer.kt` is the storage primitive

**File:** `src/commonMain/kotlin/io/github/kotlinmania/klang/mem/PackedBuffer.kt`

`PackedBuffer` wraps a `LongArray`. Its `getByte`/`setByte`/`getShort`/`setShort`/`getInt`/`setInt`/`getLong`/`setLong` accessors compute the right slot index and bit offset and use plain Kotlin shift operators. There is no `Byte` field anywhere in the file.

### `GlobalHeap` is `PackedBuffer`-backed on every target

**Files:**
- `src/commonMain/kotlin/io/github/kotlinmania/klang/mem/GlobalHeap.kt` — `expect object` API
- `src/nativeMain/kotlin/io/github/kotlinmania/klang/mem/GlobalHeapNative.kt` — `actual` for Native
- `src/jsMain/kotlin/io/github/kotlinmania/klang/mem/GlobalHeapJs.kt` — `actual` for JS

Both actuals delegate storage to `PackedBuffer`. The doc comment at the top of each `actual` file explicitly cites the rationale: *"Uses pure Long arithmetic with ushr/shl to avoid Kotlin's Byte sign extension. No BitShiftEngine needed — all operations are native Kotlin shift operators."* That's pre-existing internal documentation; this document is the deeper rationale that the inline comments allude to.

### The hot math/SIMD layer is byte-free

`Float32Math`, `Float16Math`, `Float64Math`, `Float128Math`, `ArrayBitShifts`, `BitShiftEngine`, the SwAR types — none of them store data in `ByteArray` form. Operands are passed as `Int`/`Long` raw bit patterns. The narrow types (`CFloat16`, `CBF16`, `CE8M0`, `CUE4M3`) hold their bits as `Int` or as a `Byte`-typed field where the arithmetic is delegated to a kernel that takes `Int` bit patterns.

### Where `ByteArray` still shows up legitimately

These are **not** the storage substrate; they are user-interop boundaries:

- `Runtime.memWrite(ptr, data: ByteArray, …)` and `Runtime.memRead(ptr, data: ByteArray, …)` accept user-provided `ByteArray`s. The internal storage is still `LongArray`-backed; the byte array is the source/sink for I/O with user code (string encoding, file reads, network buffers). The `memWrite`/`memRead` loops translate byte-by-byte into the heap.
- Docstring examples that show `"Hello".toByteArray()` for string input.

These are I/O boundaries, by design. Anything that needs to compute on the data converts at the boundary into the heap.

---

## How to verify these claims yourself

The Kotlin source mirror is at:

```
/Volumes/stuff/Projects/kotlinmania/kotlin.coroutines-cpp/tmp/kotlin/
```

It is a shallow clone of `JetBrains/kotlin` at tag `v2.3.21` (specifically the release tag `build-2.3.21-release-298`). To verify the claims in this document directly:

```bash
cd /Volumes/stuff/Projects/kotlinmania/kotlin.coroutines-cpp/tmp/kotlin

# (1) Byte/Short have no shift operators
grep -nE 'shl|shr|ushr|inv\(\)' libraries/stdlib/src/kotlin/Primitives.kt | head -20
# all matches are inside the Int and Long classes (lines 1051+, 1465+).

# (2) Bytewise extensions in kotlin.experimental
cat libraries/stdlib/src/kotlin/experimental/bitwiseOperations.kt
# 47 lines total, no shl/shr/ushr.

# (3) UByte.toInt is data.toInt() and 0xFF
grep -nA1 'inline fun toInt' libraries/stdlib/unsigned/src/kotlin/UByte.kt
# line 331: public inline fun toInt(): Int = data.toInt() and 0xFF

# (4) UByteArray.get is not inline
grep -nA1 'operator fun get' libraries/stdlib/unsigned/src/kotlin/UByteArray.kt
# line 29: public operator fun get(index: Int): UByte = storage[index].toUByte()
# (no `inline` keyword)

# (5) @JvmInline is JVM-only
grep -rEn 'VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION' compiler/fir/checkers
# all hits are under compiler/fir/checkers/checkers.jvm/...
```

To re-run the empirical comparison:

```bash
cd /Volumes/stuff/Projects/kotlinmania/klang
./gradlew macosArm64BenchmarkBenchmark | grep -E 'loadWordSweep'
```

Expected output (within ~5% on the same hardware class):

```
i.g.k.k.mem.UByteArrayBenchmark.loadWordSweepLongArray   avgt  5  2.172 ± 0.008  us/op
i.g.k.k.mem.UByteArrayBenchmark.loadWordSweepSigned      avgt  5  4.274 ± 0.018  us/op
i.g.k.k.mem.UByteArrayBenchmark.loadWordSweepUnsigned    avgt  5  4.846 ± 0.040  us/op
```

If the numbers ever invert or compress significantly, that's a signal Kotlin's codegen has changed — re-read this document, re-verify the stdlib source for the version we're on, and decide whether the architecture still holds.

---

## A word on Mark Adler / zlib / 16-bit C

A specific class of code that motivated klang's design is high-precision and bit-manipulation-heavy C — Mark Adler's zlib, the various CRC-32 / Adler-32 implementations, IEEE-754 soft-float kernels, the mass of 8-bit and 16-bit numeric algorithms that pre-date 32-bit-as-default conventions. These rely on:

- `unsigned char` zero-extending to `unsigned int` on widening (C standard).
- Defined wraparound on unsigned overflow.
- Specific bit-manipulation semantics that don't tolerate sign extension creeping in from a signed source byte.

A Kotlin port that uses `ByteArray` and relies on Kotlin's default `Byte.toInt()` will produce silently wrong results on roughly half the input space. Compression ports break. Checksums diverge. Cryptographic kernels produce wrong test vectors. The bug surface is exactly the bytes that have the high bit set — common in any binary input.

klang's response is structural: don't use `Byte` for storage at all, ever, in numeric code. The storage is `LongArray`; bytes are addressing offsets into wider words; arithmetic is done on `Int` / `Long` bit patterns; bit widths are explicit at every shift via `BitShiftEngine` or the `<format>Math` kernels.

This is also why the project's "no raw Kotlin bitwise operators outside the engine" rule isn't pedantry. It's a load-bearing structural defence.

---

## What changed concretely vs. what is documentation of an already-correct state

Because the storage substrate was already `LongArray` before this document was written, the practical impact of the investigation that produced this doc is:

1. The benchmark suite now includes a `loadWordSweepLongArray` variant that empirically demonstrates the 2× speedup over the byte-shape alternatives. Anyone proposing "let's modernise to `UByteArray`" gets a number to argue against.
2. This document captures the *why* with citations into stdlib source so the next engineer doesn't have to redo the investigation.
3. The architecture is now an explicit, documented decision rather than an implicit one. Future changes to `GlobalHeap`, `PackedBuffer`, or `BitShiftEngine` can cite this document as their precedent.

There is no large code migration associated with this document. klang is already on `LongArray` for everything that matters. The scattered remaining `ByteArray` references are user-interop boundaries (`memRead`/`memWrite` for string and file I/O) and docstring examples — both of which are the right place for `ByteArray` to appear.

---

## Cross-references

- **`docs/general/philosophy-and-design.md`** — the broader design rules. Specifically the *Forbidden practices* section about raw Kotlin bitwise operators and hard-coded masks. This document is the long-form rationale for that rule.
- **`docs/components/bitshift-engine/bitshift-engine.md`** — the engine itself, with NATIVE vs ARITHMETIC mode.
- **`src/commonMain/kotlin/io/github/kotlinmania/klang/mem/PackedBuffer.kt`** — the storage primitive.
- **`src/commonBenchmark/kotlin/io/github/kotlinmania/klang/mem/UByteArrayBenchmark.kt`** — the benchmark that produced the empirical numbers.

---

*Last verified against Kotlin v2.3.21 stdlib and Kotlin/Native macOS arm64 codegen on 2026-05-08.*

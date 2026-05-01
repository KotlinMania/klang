# Copilot Instructions — KLang

## What Is KLang

Pure Kotlin multiplatform library that replicates C's bitwise operations, memory model, and numeric behavior for exact C-to-Kotlin code porting. No JVM target — JS (browser) and Native (macOS, Linux, Windows) only.

## Build & Test

```bash
# Build all targets
./gradlew build

# Build macOS ARM64 release (convenience)
./gradlew buildMac

# Run macOS ARM64 debug executable
./gradlew run

# Build JS for browser (copies to docs/)
./gradlew jsWeb

# Run all tests
./gradlew allTests

# Run a single test class (use --tests filter)
./gradlew macosArm64Test --tests "ai.solace.klang.mem.KMallocTest"
./gradlew jsTest --tests "ai.solace.klang.fp.CDoubleTest"

# Run only JS tests
./gradlew jsTest

# Run only macOS ARM64 native tests
./gradlew macosArm64Test
```

Set `KLANG_NO_ENABLE_NATIVE=1` to skip native targets (faster JS-only builds).

## Architecture

### Layered Design

```
┌──────────────────────────────────┐
│   FP Types & Integer Types       │  CDouble, CFloat128, C_UInt128, etc.
├──────────────────────────────────┤
│   Bitwise Engine                 │  BitShiftEngine, ArrayBitShifts
├──────────────────────────────────┤
│   Memory (foundation)            │  GlobalHeap, KMalloc, CLib, CPointer
└──────────────────────────────────┘
```

Lower layers must not depend on higher layers.

### Single-Heap Memory Model

All C-style memory lives in a single `GlobalHeap` backed by a `PackedBuffer` (LongArray). Pointers are `Int` byte offsets. Memory operations use abbreviated MIPS-style names: `lw()` (load word), `sw()` (store word), `lh()` (load half), `lb()` (load byte), `ld()` (load double).

### BitShift Dual Modes

`BitShiftEngine` supports two modes:
- **NATIVE**: Fast path using Kotlin's `shl`/`shr`/`ushr` (use only after validation)
- **ARITHMETIC**: Pure multiply/divide operations that exactly replicate C behavior across all platforms

**Do not use raw Kotlin bitwise operators (`shl`, `shr`, `and`, `or`) outside of BitShiftEngine core files.** Route all bit manipulation through `BitShiftEngine` or `BitwiseOps` to ensure cross-platform determinism. Files that are exceptions are marked with `@native-bitshift-allowed`.

### Multiplatform (expect/actual)

Platform differences are isolated to two areas:
- `GlobalHeap` — `src/jsMain/.../GlobalHeapJs.kt` and `src/nativeMain/.../GlobalHeapNative.kt`
- `ZlibLogger` — platform-specific logging (`logToFile`, `getEnv`, `currentTimestamp`)

Everything else is in `commonMain`. Both JS and Native implementations use identical pure-Kotlin logic (no unsafe or platform-specific APIs).

## Key Conventions

### Type Naming

- **C-compatible types** use `C` or `C_` prefix: `CDouble`, `CFloat128`, `C_Int128`, `C_UInt128`
- **Memory types** use `K` prefix: `KMalloc`, `KStack`
- **Constants** are `UPPER_CASE` in companion objects: `ZERO_BITS`, `NAN_BITS`, `INF_BITS`

### Construction Pattern

Floating-point and integer types are **immutable** with **private constructors**. Use companion object factory methods:

```kotlin
val d = CDouble.fromDouble(10.0)
val f = CFloat128.fromBits(hi, lo)
// NOT: CDouble(bits)  // private constructor
```

### Tests

Tests use `kotlin.test` annotations and assertions. Memory-dependent tests must initialize the heap:

```kotlin
private fun setup() {
    GlobalHeap.init(1 shl 20)  // 1MB
    KMalloc.init(1 shl 18)     // 256KB
}

@Test fun myTest() {
    setup()
    // ...
}
```

Benchmark tests use the suffix `BenchmarkTest.kt`.

### Documentation (KDoc)

Public APIs require full KDoc with: one-line summary, `@param`/`@return`/`@throws`, a compilable usage example, complexity notation (O), `@see` cross-references, and `@since` version. See `docs/general/kdoc-strategy.md` for the full standard.

### File Suppressions

`@file:Suppress(...)` is used for files with special requirements (complex formatting in algorithm files, conservative visibility for future API expansion). Follow existing patterns when adding suppressions.

## Project References

- Detailed docs: `docs/` (architecture, components, general guides)
- C reference implementations for validation: `tools/`
- Entry point for native executable: `ai.solace.klang.poc.main`

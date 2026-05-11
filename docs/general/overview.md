KLang Walkthrough — Overview
===========================

Purpose
- Enable exact C code porting to pure Kotlin multiplatform by providing bit-exact, deterministic low-level operations
- Make Kotlin behave like C for systems programming while staying multiplatform and pure Kotlin (not cinterop)
- Solve C-to-Kotlin migration failures caused by platform-specific bitwise behavior and rounding differences

Why KLang Exists
- Multiple large-scale C-to-Kotlin ports failed due to subtle behavioral differences
- 16-bit C code, double-double algorithms, cryptography, and HPC code couldn't be reliably ported
- Kotlin's native bitwise operators have platform-specific behavior that breaks C algorithm ports
- Need for pure Kotlin implementation that exactly replicates C semantics across all platforms

Key Ideas
- Pure Kotlin, not cinterop: All C behavior is implemented in idiomatic Kotlin multiplatform code
- Single heap: All C-style memory lives in one expandable heap (GlobalHeap) backed by a ByteArray. Pointers are Int byte offsets.
- Deterministic operations: Typed loads/stores are little-endian and identical across Kotlin/JS and Kotlin/Native targets. (klang ships JS + Native only — no JVM target, no Android target. See [`CLAUDE.md`](../../CLAUDE.md).)
- Allocator: KMalloc provides malloc/calloc/realloc/free, with coalescing and size-class bins.
- Bit shifts: BitShiftEngine enforces C-exact behavior. Raw Kotlin bitwise operators forbidden outside engine.
- C-compatible types: All C types (C_UInt8, C_UInt128, etc.) have exact semantic equivalents with heap storage.
- Floating parity: CDouble is IEEE-754 binary64; CLongDouble exposes intent (DOUBLE64, EXTENDED80, IEEE128).
- CLib: A minimal libc surface (strlen/strcmp/mem* etc.) runs over the heap with fast word-at-a-time loops.

Repo Highlights
- Heap: src/commonMain/kotlin/io/github/kotlinmania/klang/mem/GlobalArrayHeap.kt
- Allocator: src/commonMain/kotlin/io/github/kotlinmania/klang/mem/KMalloc.kt
- Bit shifts: src/commonMain/kotlin/io/github/kotlinmania/klang/bitwise/*, esp. BitShiftEngine.kt, ArrayBitShifts.kt
- 128‑bit: src/commonMain/kotlin/io/github/kotlinmania/klang/int/hpc/HeapUInt128.kt
- Floating: src/commonMain/kotlin/io/github/kotlinmania/klang/fp/CDouble.kt, CLongDouble.kt, CFloat128.kt
- libc: src/commonMain/kotlin/io/github/kotlinmania/klang/mem/CLib.kt, CString.kt, FastMem.kt, FastStringMem.kt
- POC bench: src/nativeMain/kotlin/io/github/kotlinmania/klang/poc/ActorArrayBitShiftPOC.kt


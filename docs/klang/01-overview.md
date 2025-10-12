KLang Walkthrough — Overview
===========================

Purpose
- Make Kotlin behave like C for low‑level work (bit‑exact, deterministic) while staying multiplatform and pure Kotlin.

Key Ideas
- Single heap: All C‑style memory lives in one expandable heap (GlobalHeap) backed by a ByteArray. Pointers are Int byte offsets.
- Deterministic operations: Typed loads/stores are little‑endian and identical across targets.
- Allocator: KMalloc provides malloc/calloc/realloc/free, with coalescing and size‑class bins.
- Bit shifts: A BitShiftEngine dispatches between NATIVE and ARITHMETIC implementations; ArrayBitShifts uses it for 16‑bit limbs.
- Floating parity: CDouble is IEEE‑754 binary64; CLongDouble exposes intent (DOUBLE64, EXTENDED80, IEEE128).
- CLib: A minimal libc surface (strlen/strcmp/mem* etc.) runs over the heap with fast word‑at‑a‑time loops.

Repo Highlights
- Heap: src/commonMain/kotlin/ai/solace/klang/mem/GlobalArrayHeap.kt
- Allocator: src/commonMain/kotlin/ai/solace/klang/mem/KMalloc.kt
- Bit shifts: src/commonMain/kotlin/ai/solace/klang/bitwise/*, esp. BitShiftEngine.kt, ArrayBitShifts.kt
- 128‑bit: src/commonMain/kotlin/ai/solace/klang/int/hpc/HeapUInt128.kt
- Floating: src/commonMain/kotlin/ai/solace/klang/fp/CDouble.kt, CLongDouble.kt, CFloat128.kt
- libc: src/commonMain/kotlin/ai/solace/klang/mem/CLib.kt, CString.kt, FastMem.kt, FastStringMem.kt
- POC bench: src/nativeMain/kotlin/ai/solace/klang/poc/ActorArrayBitShiftPOC.kt


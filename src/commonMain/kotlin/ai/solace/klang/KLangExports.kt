@file:Suppress("unused")
package ai.solace.klang

/**
 * KLang: Kotlin Language Numeric Core
 *
 * A C-aligned library that provides bit-exact C semantics in pure Kotlin.
 * Enables porting C code to Kotlin multiplatform without using cinterop.
 *
 * ## Purpose
 *
 * KLang solves critical portability issues when working with numeric code:
 * - Kotlin's bitwise operators only work on Int/Long, breaking on Byte/Short
 * - Floating point operations may round differently than C
 * - No native support for extended precision types (int128, float128)
 * - Missing C-style memory management and pointer semantics
 *
 * ## Key Features
 *
 * - C-compatible types: CFloat32, CDouble, C_UInt128, C_Int128, etc.
 * - Bit-exact arithmetic: Matches C behavior precisely across all platforms
 * - Memory management: Heap, malloc/free, pointers without native interop
 * - Zero-copy design: Operations work directly on heap memory
 * - Multiplatform: Works on JVM, JS, Native without platform-specific code
 *
 * ## Usage
 *
 * Import the public API to access all KLang types:
 *
 * ```kotlin
 * import ai.solace.klang.api.CFloat32
 * import ai.solace.klang.api.GlobalHeap
 *
 * val x = CFloat32.fromFloat(3.14f)
 * val ptr = GlobalHeap.mallocBytes(100)
 * ```
 *
 * ## Architecture
 *
 * - api/: Public API surface (stable)
 * - bitwise/: Bit shift engine and arithmetic operations
 * - fp/: Floating point types
 * - int/: Extended precision integers
 * - mem/: Memory management and heap
 * - internal/: Implementation details (unstable)
 *
 * @see ai.solace.klang.api.PublicAPI For the complete public API
 */

package ai.solace.klang.api

/**
 * Public API surface for KLang.
 *
 * This file defines the stable, public API that external consumers should use.
 * All other types in the library are considered internal implementation details
 * and may change without notice.
 *
 * ## Core Type System
 *
 * KLang provides C-compatible numeric types that guarantee bit-exact semantics
 * across all Kotlin platforms (JVM, JS, Native). These types are essential for:
 * - Porting C/C++ code to Kotlin
 * - Implementing binary protocols and file formats
 * - Cross-platform cryptographic and compression algorithms
 * - High-performance computing with IEEE-754 compliance
 *
 * ## API Categories
 *
 * ### Floating Point Types
 * - [CFloat32]: 32-bit IEEE-754 float with C-exact semantics
 * - [CDouble]: 64-bit IEEE-754 double with C-exact semantics
 * - [CFloat16]: 16-bit IEEE-754 half-precision float
 * - [CBF16]: Google Brain Float16 format
 * - [CFloat128]: 128-bit extended precision float (quad precision)
 * - [CLongDouble]: Platform-specific long double (80/128-bit)
 *
 * ### Integer Types
 * - [C_UInt128]: 128-bit unsigned integer
 * - [C_Int128]: 128-bit signed integer
 *
 * ### Memory Management
 * - [CPointer]: Type-safe pointer abstraction
 * - [GlobalHeap]: Heap memory allocation
 * - [KMalloc]: malloc/free family implementation
 * - [CLib]: C standard library string/memory functions
 *
 * ### Bit Manipulation
 * - [BitShiftEngine]: Safe, C-compatible bitwise operations
 * - [BitwiseOps]: Additional bitwise utilities
 *
 * ## Usage Example
 *
 * ```kotlin
 * import ai.solace.klang.api.*
 *
 * // Create C-compatible floats
 * val a = CFloat32.fromFloat(3.14f)
 * val b = CFloat32.fromFloat(2.71f)
 * val result = a + b
 *
 * // Allocate memory
 * val ptr = GlobalHeap.mallocBytes(100)
 * GlobalHeap.sb(ptr.ptr, 42)
 * val value = GlobalHeap.lbu(ptr.ptr)
 * GlobalHeap.free(ptr)
 *
 * // 128-bit integer operations
 * val x = C_UInt128.fromLongs(0, 1)
 * val y = C_UInt128.fromLongs(0, 2)
 * val sum = x + y
 * ```
 *
 * @see ai.solace.klang.fp Floating point types
 * @see ai.solace.klang.int Integer types
 * @see ai.solace.klang.mem Memory management
 * @see ai.solace.klang.bitwise Bit manipulation
 */

// ============================================================================
// Floating Point Types
// ============================================================================

/**
 * C-compatible 32-bit IEEE-754 single-precision floating point.
 *
 * Guarantees bit-exact arithmetic matching C's float type across all platforms.
 */
typealias CFloat32 = ai.solace.klang.bitwise.CFloat32

/**
 * C-compatible 64-bit IEEE-754 double-precision floating point.
 *
 * Guarantees bit-exact arithmetic matching C's double type across all platforms.
 */
typealias CDouble = ai.solace.klang.fp.CDouble

/**
 * C-compatible 16-bit IEEE-754 half-precision floating point.
 */
typealias CFloat16 = ai.solace.klang.fp.CFloat16

/**
 * Google Brain Float16 format (BF16).
 *
 * 16-bit floating point format optimized for machine learning.
 */
typealias CBF16 = ai.solace.klang.fp.CBF16

/**
 * 128-bit IEEE-754 quadruple-precision floating point.
 */
typealias CFloat128 = ai.solace.klang.fp.CFloat128

/**
 * Platform-specific long double (80-bit on x86, 128-bit on others).
 */
typealias CLongDouble = ai.solace.klang.fp.CLongDouble

// ============================================================================
// Integer Types
// ============================================================================

/**
 * C-compatible 128-bit unsigned integer (__uint128 in GCC/Clang).
 */
typealias C_UInt128 = ai.solace.klang.int.C_UInt128

/**
 * C-compatible 128-bit signed integer (__int128 in GCC/Clang).
 */
typealias C_Int128 = ai.solace.klang.int.C_Int128

// ============================================================================
// Memory Management
// ============================================================================

/**
 * Type-safe C pointer abstraction.
 *
 * Wraps an integer address to provide type-safe memory access.
 */
typealias CPointer<T> = ai.solace.klang.internal.runtime.CPointer<T>

/**
 * Global heap memory manager.
 *
 * Provides malloc/free and memory access operations.
 */
typealias GlobalHeap = ai.solace.klang.mem.GlobalHeap

/**
 * Memory allocation subsystem (malloc/calloc/realloc/free).
 */
typealias KMalloc = ai.solace.klang.mem.KMalloc

/**
 * C standard library string and memory functions.
 *
 * Provides strlen, strcpy, memcpy, memset, etc.
 */
typealias CLib = ai.solace.klang.mem.CLib

/**
 * C-style null-terminated strings on the heap.
 */
typealias CString = ai.solace.klang.mem.CString

// ============================================================================
// Scalar Variables
// ============================================================================

/**
 * C-compatible byte variable on the heap.
 */
typealias CByteVar = ai.solace.klang.mem.CByteVar

/**
 * C-compatible short variable on the heap.
 */
typealias CShortVar = ai.solace.klang.mem.CShortVar

/**
 * C-compatible int variable on the heap.
 */
typealias CIntVar = ai.solace.klang.mem.CIntVar

/**
 * C-compatible long variable on the heap.
 */
typealias CLongVar = ai.solace.klang.mem.CLongVar

/**
 * C-compatible float variable on the heap.
 */
typealias CFloatVar = ai.solace.klang.mem.CFloatVar

/**
 * C-compatible double variable on the heap.
 */
typealias CDoubleVar = ai.solace.klang.mem.CDoubleVar

// ============================================================================
// Bit Manipulation
// ============================================================================

/**
 * Arithmetic-mode bit shifting engine.
 *
 * Provides C-compatible bit shift operations that work correctly with
 * Kotlin's type promotion rules.
 */
typealias BitShiftEngine = ai.solace.klang.bitwise.BitShiftEngine

/**
 * Bit shift configuration (arithmetic vs native mode).
 */
typealias BitShiftConfig = ai.solace.klang.bitwise.BitShiftConfig

/**
 * Additional bitwise operation utilities.
 */
typealias BitwiseOps = ai.solace.klang.bitwise.BitwiseOps

// ============================================================================
// Helper Objects
// ============================================================================

/**
 * Bit manipulation primitives.
 */
typealias BitPrimitives = ai.solace.klang.bitwise.BitPrimitives

/**
 * Float32 math operations.
 */
typealias Float32Math = ai.solace.klang.bitwise.Float32Math

/**
 * Float64 math operations.
 */
typealias Float64Math = ai.solace.klang.bitwise.Float64Math

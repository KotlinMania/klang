@file:Suppress("unused")
package io.github.kotlinmania.klang

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
 * - C-compatible types: CFloat32, CFloat64, C_UInt128, C_Int128, etc.
 * - Bit-exact arithmetic: Matches C behavior precisely across all platforms
 * - Memory management: Heap, malloc/free, pointers without native interop
 * - Zero-copy design: Operations work directly on heap memory
 * - Multiplatform: Works on JVM, JS, Native without platform-specific code
 *
 * ## Usage
 *
 * Import types directly from their respective packages:
 *
 * ```kotlin
 * import io.github.kotlinmania.klang.fp.CFloat32
 * import io.github.kotlinmania.klang.fp.CFloat64
 * import io.github.kotlinmania.klang.int.C_UInt128
 * import io.github.kotlinmania.klang.mem.GlobalHeap
 * import io.github.kotlinmania.klang.bitwise.BitShiftEngine
 *
 * // Create C-compatible floats
 * val x = CFloat32.fromFloat(3.14f)
 * val y = CFloat64.fromDouble(2.718)
 *
 * // Allocate memory
 * val ptr = GlobalHeap.mallocBytes(100)
 * GlobalHeap.sb(ptr.ptr, 42)
 * val value = GlobalHeap.lbu(ptr.ptr)
 * GlobalHeap.free(ptr)
 *
 * // 128-bit integer operations
 * val a = C_UInt128.fromLongs(0, 1)
 * val b = C_UInt128.fromLongs(0, 2)
 * val sum = a + b
 * ```
 *
 * ## Package Structure
 *
 * - [io.github.kotlinmania.klang.bitwise]: Bit shift engine and bitwise operations
 * - [io.github.kotlinmania.klang.fp]: Floating point types (CFloat64, CFloat16, CFloat128, etc.)
 * - [io.github.kotlinmania.klang.int]: Extended precision integers (C_UInt128, C_Int128)
 * - [io.github.kotlinmania.klang.mem]: Memory management, heap, malloc/free, pointers
 * - [io.github.kotlinmania.klang.common]: Common utilities and logging
 * - [io.github.kotlinmania.klang.internal]: Internal implementation (unstable, do not use directly)
 */

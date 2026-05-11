package io.github.kotlinmania.klang.internal

import io.github.kotlinmania.klang.internal.runtime.BytePointer
import io.github.kotlinmania.klang.internal.runtime.CPointer
import io.github.kotlinmania.klang.internal.runtime.DoublePointer
import io.github.kotlinmania.klang.internal.runtime.FloatPointer
import io.github.kotlinmania.klang.internal.runtime.IntPointer
import io.github.kotlinmania.klang.internal.runtime.LongPointer
import io.github.kotlinmania.klang.internal.runtime.ShortPointer

/**
 * Typed pointer extensions for the per-primitive concrete pointer classes
 * ([BytePointer], [ShortPointer], [IntPointer], [LongPointer], [FloatPointer],
 * [DoublePointer]). Receivers are distinct Kotlin types so the JVM-erased
 * signatures of `index` / `load` / `store` don't collide.
 *
 * Provides Kotlin-idiomatic pointer arithmetic, memory access, and allocation
 * operations that mirror C's pointer semantics while maintaining type safety.
 */

/**
 * Advances a byte pointer by [i] bytes.
 *
 * @param i The number of bytes to advance.
 * @return A new pointer at offset i bytes.
 */
inline fun BytePointer.index(i: Int): BytePointer = BytePointer(this.ptr + i)

/**
 * Advances a short pointer by [i] elements (i * 2 bytes).
 *
 * @param i The number of short elements to advance.
 * @return A new pointer at offset i * sizeof(short) bytes.
 */
inline fun ShortPointer.index(i: Int): ShortPointer = ShortPointer(this.ptr + i * 2)

/**
 * Advances an int pointer by [i] elements (i * 4 bytes).
 *
 * @param i The number of int elements to advance.
 * @return A new pointer at offset i * sizeof(int) bytes.
 */
inline fun IntPointer.index(i: Int): IntPointer = IntPointer(this.ptr + i * 4)

/**
 * Advances a long pointer by [i] elements (i * 8 bytes).
 *
 * @param i The number of long elements to advance.
 * @return A new pointer at offset i * sizeof(long) bytes.
 */
inline fun LongPointer.index(i: Int): LongPointer = LongPointer(this.ptr + i * 8)

/**
 * Advances a float pointer by [i] elements (i * 4 bytes).
 *
 * @param i The number of float elements to advance.
 * @return A new pointer at offset i * sizeof(float) bytes.
 */
inline fun FloatPointer.index(i: Int): FloatPointer = FloatPointer(this.ptr + i * 4)

/**
 * Advances a double pointer by [i] elements (i * 8 bytes).
 *
 * @param i The number of double elements to advance.
 * @return A new pointer at offset i * sizeof(double) bytes.
 */
inline fun DoublePointer.index(i: Int): DoublePointer = DoublePointer(this.ptr + i * 8)

/**
 * Loads a byte value from memory at this pointer's address.
 *
 * @return The byte value stored at this address.
 */
fun BytePointer.load(): Byte = io.github.kotlinmania.klang.mem.GlobalHeap.lb(ptr)

/**
 * Stores a byte value to memory at this pointer's address.
 *
 * @param v The byte value to store.
 */
fun BytePointer.store(v: Byte) = io.github.kotlinmania.klang.mem.GlobalHeap.sb(ptr, v)

/**
 * Loads a short value from memory at this pointer's address.
 *
 * @return The short value stored at this address.
 */
fun ShortPointer.load(): Short = io.github.kotlinmania.klang.mem.GlobalHeap.lh(ptr)

/**
 * Stores a short value to memory at this pointer's address.
 *
 * @param v The short value to store.
 */
fun ShortPointer.store(v: Short) = io.github.kotlinmania.klang.mem.GlobalHeap.sh(ptr, v)

/**
 * Loads an int value from memory at this pointer's address.
 *
 * @return The int value stored at this address.
 */
fun IntPointer.load(): Int = io.github.kotlinmania.klang.mem.GlobalHeap.lw(ptr)

/**
 * Stores an int value to memory at this pointer's address.
 *
 * @param v The int value to store.
 */
fun IntPointer.store(v: Int) = io.github.kotlinmania.klang.mem.GlobalHeap.sw(ptr, v)

/**
 * Loads a long value from memory at this pointer's address.
 *
 * @return The long value stored at this address.
 */
fun LongPointer.load(): Long = io.github.kotlinmania.klang.mem.GlobalHeap.ld(ptr)

/**
 * Stores a long value to memory at this pointer's address.
 *
 * @param v The long value to store.
 */
fun LongPointer.store(v: Long) = io.github.kotlinmania.klang.mem.GlobalHeap.sd(ptr, v)

/**
 * Loads a float value from memory at this pointer's address.
 *
 * @return The float value stored at this address.
 */
fun FloatPointer.load(): Float = io.github.kotlinmania.klang.mem.GlobalHeap.lwf(ptr)

/**
 * Stores a float value to memory at this pointer's address.
 *
 * @param v The float value to store.
 */
fun FloatPointer.store(v: Float) = io.github.kotlinmania.klang.mem.GlobalHeap.swf(ptr, v)

/**
 * Loads a double value from memory at this pointer's address.
 *
 * @return The double value stored at this address.
 */
fun DoublePointer.load(): Double = io.github.kotlinmania.klang.mem.GlobalHeap.ldf(ptr)

/**
 * Stores a double value to memory at this pointer's address.
 *
 * @param v The double value to store.
 */
fun DoublePointer.store(v: Double) = io.github.kotlinmania.klang.mem.GlobalHeap.sdf(ptr, v)

/**
 * Allocates [n] bytes of uninitialized memory.
 *
 * @param n The number of bytes to allocate.
 * @return A pointer to the allocated memory, or null on failure.
 */
fun mallocBytes(n: Int): BytePointer = BytePointer(io.github.kotlinmania.klang.mem.GlobalHeap.malloc(n))

/**
 * Allocates [n] bytes of zero-initialized memory.
 *
 * @param n The number of bytes to allocate.
 * @return A pointer to the allocated memory, or null on failure.
 */
fun callocBytes(n: Int): BytePointer = BytePointer(io.github.kotlinmania.klang.mem.GlobalHeap.calloc(n, 1))

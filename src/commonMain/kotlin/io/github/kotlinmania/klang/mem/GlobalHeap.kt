package io.github.kotlinmania.klang.mem

/**
 * GlobalHeap: A single, deterministic heap for all C-like memory operations.
 *
 * KLang's foundation provides C-like memory semantics across all Kotlin multiplatform targets.
 * All "pointers" are [Int] byte offsets into the heap, enabling:
 * - Porting C programs that expect contiguous memory
 * - Zero-copy operations (modify data in-place)
 * - Deterministic serialization (dump/restore entire heap state)
 * - Predictable performance (no GC pauses)
 *
 * Storage delegates to [PackedBuffer] (`LongArray`-backed pure Kotlin). All targets
 * (JS, Native, Android) get the same `Long`-arithmetic path — no platform-specific
 * intrinsics, so the implementation lives in commonMain rather than as three
 * identical expect/actual copies.
 *
 * ## Memory Model
 *
 * ```
 * GlobalHeap Memory Layout:
 * ┌─────────────────────────────────────────────┐
 * │  Used Memory (0 to hp)  │  Free Space       │
 * └─────────────────────────────────────────────┘
 *                           ↑
 *                          hp (heap pointer)
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * GlobalHeap.init(1024 * 1024)  // 1MB heap
 * val addr = GlobalHeap.malloc(128)
 * GlobalHeap.sw(addr, 42)
 * val value = GlobalHeap.lw(addr)  // returns 42
 * ```
 *
 * ## Thread Safety
 *
 * **Not thread-safe**. External synchronization required for concurrent access.
 *
 * @see KMalloc For free-list allocator with malloc/free semantics
 * @see CScalars For type-safe scalar variables on the heap
 * @since 0.1.0
 */
object GlobalHeap {
    private var buffer: PackedBuffer = PackedBuffer(0)
    private var hp: Int = 0

    /** Total heap size in bytes. */
    val size: Int get() = buffer.capacity

    /** Number of bytes currently allocated (used). */
    val used: Int get() = hp

    /** Initialize or reinitialize the heap with specified size. */
    fun init(bytes: Int) {
        require(bytes >= 0) { "Heap size must be non-negative" }
        buffer = PackedBuffer(bytes)
        hp = 0
    }

    /** Reset heap pointer to 0, effectively freeing all allocations. */
    fun reset() { hp = 0 }

    /** Dispose of the heap, releasing the backing memory. */
    fun dispose() {
        buffer = PackedBuffer(0)
        hp = 0
    }

    /** Allocate uninitialized memory (like C's malloc). */
    fun malloc(bytes: Int): Int {
        require(bytes >= 0) { "Cannot allocate negative bytes" }
        val base = hp
        val end = base + bytes
        ensure(end)
        hp = end
        return base
    }

    /** Allocate zero-initialized memory (like C's calloc). */
    fun calloc(count: Int, elemSize: Int): Int {
        require(count >= 0 && elemSize >= 0) { "Count and elemSize must be non-negative" }
        val total = count * elemSize
        val p = malloc(total)
        memset(p, 0, total)
        return p
    }

    /** Free memory (no-op in bump allocator). */
    fun free(ptr: Int) { /* no-op bump allocator */ }

    private fun ensure(minSize: Int) {
        if (minSize <= buffer.capacity) return
        var newSize = buffer.capacity.coerceAtLeast(1024)
        while (newSize < minSize) {
            newSize = newSize + (newSize ushr 1)  // 1.5x growth
        }
        val newBuffer = PackedBuffer(newSize)
        for (i in 0 until (buffer.capacity ushr 3)) {
            if (i < newBuffer.data.size && i < buffer.data.size) {
                newBuffer.data[i] = buffer.data[i]
            }
        }
        buffer = newBuffer
    }

    /** Ensure heap capacity, growing if necessary. */
    fun ensureCapacity(minSize: Int) = ensure(minSize)

    // ========== Typed Load/Store Operations (Little-Endian) ==========

    /** Load byte (signed). */
    fun lb(addr: Int): Byte = buffer.getByte(addr).toByte()

    /** Store byte. */
    fun sb(addr: Int, value: Byte) = buffer.setByte(addr, value.toInt())

    /** Load byte (unsigned, zero-extended to Int). */
    fun lbu(addr: Int): Int = buffer.getByte(addr)

    /** Load half-word (16-bit signed short, little-endian). */
    fun lh(addr: Int): Short = buffer.getShort(addr)

    /** Store half-word (16-bit short, little-endian). */
    fun sh(addr: Int, value: Short) = buffer.setShort(addr, value)

    /** Load word (32-bit signed int, little-endian). */
    fun lw(addr: Int): Int = buffer.getInt(addr)

    /** Store word (32-bit int, little-endian). */
    fun sw(addr: Int, value: Int) = buffer.setInt(addr, value)

    /** Load doubleword (64-bit signed long, little-endian). */
    fun ld(addr: Int): Long = buffer.getLong(addr)

    /** Store doubleword (64-bit long, little-endian). */
    fun sd(addr: Int, value: Long) = buffer.setLong(addr, value)

    /** Load float (32-bit). */
    fun lwf(addr: Int): Float = buffer.getFloat(addr)

    /** Store float (32-bit). */
    fun swf(addr: Int, value: Float) = buffer.setFloat(addr, value)

    /** Load double (64-bit). */
    fun ldf(addr: Int): Double = buffer.getDouble(addr)

    /** Store double (64-bit). */
    fun sdf(addr: Int, value: Double) = buffer.setDouble(addr, value)

    /** memcpy: undefined for overlap. */
    fun memcpy(dst: Int, src: Int, bytes: Int) = buffer.copy(dst, src, bytes)

    /** memmove: overlap-safe. */
    fun memmove(dst: Int, src: Int, bytes: Int) = buffer.move(dst, src, bytes)

    /** memset: fill memory with byte value. */
    fun memset(addr: Int, value: Int, bytes: Int) = buffer.fill(addr, value, bytes)
}

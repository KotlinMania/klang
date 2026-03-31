package ai.solace.klang.mem

/**
 * JS implementation of GlobalHeap using PackedBuffer (LongArray-backed).
 *
 * Uses pure Long arithmetic with ushr/shl to avoid Kotlin's Byte sign extension.
 * No BitShiftEngine needed - all operations are native Kotlin shift operators.
 */
actual object GlobalHeap {
    private var buffer: PackedBuffer = PackedBuffer(0)
    private var hp: Int = 0

    actual val size: Int get() = buffer.capacity
    actual val used: Int get() = hp

    actual fun init(bytes: Int) {
        require(bytes >= 0) { "Heap size must be non-negative" }
        buffer = PackedBuffer(bytes)
        hp = 0
    }

    actual fun reset() { hp = 0 }

    actual fun dispose() {
        buffer = PackedBuffer(0)
        hp = 0
    }

    actual fun malloc(bytes: Int): Int {
        require(bytes >= 0) { "Cannot allocate negative bytes" }
        val base = hp
        val end = base + bytes
        ensure(end)
        hp = end
        return base
    }

    actual fun calloc(count: Int, elemSize: Int): Int {
        require(count >= 0 && elemSize >= 0) { "Count and elemSize must be non-negative" }
        val total = count * elemSize
        val p = malloc(total)
        memset(p, 0, total)
        return p
    }

    actual fun free(ptr: Int) { /* no-op bump allocator */ }

    private fun ensure(minSize: Int) {
        if (minSize <= buffer.capacity) return
        var newSize = buffer.capacity.coerceAtLeast(1024)
        while (newSize < minSize) {
            newSize = newSize + (newSize ushr 1)  // 1.5x growth
        }
        val newBuffer = PackedBuffer(newSize)
        // Copy old data
        for (i in 0 until (buffer.capacity ushr 3)) {
            if (i < newBuffer.data.size && i < buffer.data.size) {
                newBuffer.data[i] = buffer.data[i]
            }
        }
        buffer = newBuffer
    }

    actual fun ensureCapacity(minSize: Int) = ensure(minSize)

    // ========== Typed Load/Store (Little-Endian) ==========

    actual fun lb(addr: Int): Byte = buffer.getByte(addr).toByte()
    actual fun sb(addr: Int, value: Byte) = buffer.setByte(addr, value.toInt())
    actual fun lbu(addr: Int): Int = buffer.getByte(addr)

    actual fun lh(addr: Int): Short = buffer.getShort(addr)
    actual fun sh(addr: Int, value: Short) = buffer.setShort(addr, value)

    actual fun lw(addr: Int): Int = buffer.getInt(addr)
    actual fun sw(addr: Int, value: Int) = buffer.setInt(addr, value)

    actual fun ld(addr: Int): Long = buffer.getLong(addr)
    actual fun sd(addr: Int, value: Long) = buffer.setLong(addr, value)

    actual fun lwf(addr: Int): Float = buffer.getFloat(addr)
    actual fun swf(addr: Int, value: Float) = buffer.setFloat(addr, value)

    actual fun ldf(addr: Int): Double = buffer.getDouble(addr)
    actual fun sdf(addr: Int, value: Double) = buffer.setDouble(addr, value)

    actual fun memcpy(dst: Int, src: Int, bytes: Int) = buffer.copy(dst, src, bytes)
    actual fun memmove(dst: Int, src: Int, bytes: Int) = buffer.move(dst, src, bytes)
    actual fun memset(addr: Int, value: Int, bytes: Int) = buffer.fill(addr, value, bytes)
}

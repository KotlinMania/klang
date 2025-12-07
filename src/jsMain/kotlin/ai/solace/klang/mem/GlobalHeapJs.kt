package ai.solace.klang.mem

/**
 * JS implementation of GlobalHeap using ByteArray with manual byte assembly.
 * JavaScript has no native typed pointer access, so we assemble multi-byte
 * values from individual bytes.
 */
actual object GlobalHeap {
    private var mem: ByteArray = ByteArray(0)
    private var hp: Int = 0

    actual val size: Int get() = mem.size
    actual val used: Int get() = hp

    actual fun init(bytes: Int) {
        require(bytes >= 0) { "Heap size must be non-negative" }
        mem = ByteArray(bytes)
        hp = 0
    }

    actual fun reset() { hp = 0 }

    actual fun dispose() {
        mem = ByteArray(0)
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
        if (minSize <= mem.size) return
        var newSize = mem.size.coerceAtLeast(1024)
        while (newSize < minSize) {
            newSize = newSize + (newSize ushr 1)  // 1.5x growth
        }
        val next = ByteArray(newSize)
        mem.copyInto(next, 0, 0, mem.size)
        mem = next
    }

    actual fun ensureCapacity(minSize: Int) = ensure(minSize)

    // ========== Typed Load/Store (Little-Endian) ==========

    actual fun lb(addr: Int): Byte = mem[addr]
    actual fun sb(addr: Int, value: Byte) { mem[addr] = value }
    actual fun lbu(addr: Int): Int = mem[addr].toInt() and 0xFF

    actual fun lh(addr: Int): Short {
        return ((mem[addr].toInt() and 0xFF) or
                ((mem[addr + 1].toInt() and 0xFF) shl 8)).toShort()
    }

    actual fun sh(addr: Int, value: Short) {
        val v = value.toInt()
        mem[addr] = v.toByte()
        mem[addr + 1] = (v shr 8).toByte()
    }

    actual fun lw(addr: Int): Int {
        return (mem[addr].toInt() and 0xFF) or
               ((mem[addr + 1].toInt() and 0xFF) shl 8) or
               ((mem[addr + 2].toInt() and 0xFF) shl 16) or
               ((mem[addr + 3].toInt() and 0xFF) shl 24)
    }

    actual fun sw(addr: Int, value: Int) {
        mem[addr] = value.toByte()
        mem[addr + 1] = (value shr 8).toByte()
        mem[addr + 2] = (value shr 16).toByte()
        mem[addr + 3] = (value shr 24).toByte()
    }

    actual fun ld(addr: Int): Long {
        return (mem[addr].toLong() and 0xFF) or
               ((mem[addr + 1].toLong() and 0xFF) shl 8) or
               ((mem[addr + 2].toLong() and 0xFF) shl 16) or
               ((mem[addr + 3].toLong() and 0xFF) shl 24) or
               ((mem[addr + 4].toLong() and 0xFF) shl 32) or
               ((mem[addr + 5].toLong() and 0xFF) shl 40) or
               ((mem[addr + 6].toLong() and 0xFF) shl 48) or
               ((mem[addr + 7].toLong() and 0xFF) shl 56)
    }

    actual fun sd(addr: Int, value: Long) {
        mem[addr] = value.toByte()
        mem[addr + 1] = (value shr 8).toByte()
        mem[addr + 2] = (value shr 16).toByte()
        mem[addr + 3] = (value shr 24).toByte()
        mem[addr + 4] = (value shr 32).toByte()
        mem[addr + 5] = (value shr 40).toByte()
        mem[addr + 6] = (value shr 48).toByte()
        mem[addr + 7] = (value shr 56).toByte()
    }

    actual fun lwf(addr: Int): Float = Float.fromBits(lw(addr))
    actual fun swf(addr: Int, value: Float) = sw(addr, value.toRawBits())

    actual fun ldf(addr: Int): Double = Double.fromBits(ld(addr))
    actual fun sdf(addr: Int, value: Double) = sd(addr, value.toRawBits())

    actual fun memcpy(dst: Int, src: Int, bytes: Int) {
        // Simple byte copy for JS
        for (i in 0 until bytes) {
            mem[dst + i] = mem[src + i]
        }
    }

    actual fun memmove(dst: Int, src: Int, bytes: Int) {
        if (dst < src) {
            for (i in 0 until bytes) mem[dst + i] = mem[src + i]
        } else {
            for (i in bytes - 1 downTo 0) mem[dst + i] = mem[src + i]
        }
    }

    actual fun memset(addr: Int, value: Int, bytes: Int) {
        val b = value.toByte()
        for (i in 0 until bytes) mem[addr + i] = b
    }
}

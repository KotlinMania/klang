package ai.solace.klang.mem

/**
 * GlobalHeap: simple, fast, portable heap modeled as a single ByteArray.
 * Pointers are byte offsets (Int). Arrays double as pointer targets: index == address.
 *
 * Design goals:
 * - Pure Kotlin, multiplatform-safe (JS/Native/…)
 * - No JVM deps; no native interop required
 * - Deterministic little-endian layout for C parity
 */
object GlobalHeap {
    private var mem: ByteArray = ByteArray(0)
    private var hp: Int = 0 // heap pointer (bump allocator)

    val size: Int get() = mem.size
    val used: Int get() = hp

    fun init(bytes: Int) {
        mem = ByteArray(bytes)
        hp = 0
    }

    fun reset() { hp = 0 }

    fun dispose() { mem = ByteArray(0); hp = 0 }

    fun malloc(bytes: Int): Int {
        val base = hp
        val end = base + bytes
        ensure(end)
        hp = end
        return base
    }

    fun calloc(count: Int, elemSize: Int): Int {
        val total = count * elemSize
        val p = malloc(total)
        memset(p, 0, total)
        return p
    }

    fun free(@Suppress("UNUSED_PARAMETER") ptr: Int) { /* no-op bump allocator */ }

    // Ensure capacity
    private fun ensure(minSize: Int) {
        if (minSize <= mem.size) return
        var newSize = mem.size.coerceAtLeast(1024)
        while (newSize < minSize) newSize = newSize + (newSize ushr 1) // 1.5x
        val next = ByteArray(newSize)
        mem.copyInto(next, 0, 0, mem.size)
        mem = next
    }

    /** Public ensure for allocators that need to grow the backing heap preserving contents. */
    fun ensureCapacity(minSize: Int) = ensure(minSize)

    // Typed loads/stores (little-endian)
    fun lb(addr: Int): Byte = mem[addr]
    fun sb(addr: Int, value: Byte) { mem[addr] = value }

    fun lbu(addr: Int): Int = lb(addr).toInt() and 0xFF

    fun lh(addr: Int): Short {
        val b0 = lbu(addr)
        val b1 = lbu(addr + 1)
        return ((b0) or (b1 shl 8)).toShort()
    }
    fun sh(addr: Int, value: Short) {
        val v = value.toInt() and 0xFFFF
        sb(addr + 0, (v and 0xFF).toByte())
        sb(addr + 1, ((v ushr 8) and 0xFF).toByte())
    }

    fun lw(addr: Int): Int {
        val b0 = lbu(addr)
        val b1 = lbu(addr + 1)
        val b2 = lbu(addr + 2)
        val b3 = lbu(addr + 3)
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }
    fun sw(addr: Int, value: Int) {
        sb(addr + 0, (value and 0xFF).toByte())
        sb(addr + 1, ((value ushr 8) and 0xFF).toByte())
        sb(addr + 2, ((value ushr 16) and 0xFF).toByte())
        sb(addr + 3, ((value ushr 24) and 0xFF).toByte())
    }

    fun ld(addr: Int): Long {
        var r = 0L
        for (i in 7 downTo 0) r = (r shl 8) or lbu(addr + i).toLong()
        return r
    }
    fun sd(addr: Int, value: Long) {
        var v = value
        for (i in 0 until 8) { sb(addr + i, (v and 0xFF).toByte()); v = v ushr 8 }
    }

    fun lwf(addr: Int): Float = Float.fromBits(lw(addr))
    fun swf(addr: Int, value: Float) = sw(addr, value.toRawBits())

    fun ldf(addr: Int): Double = Double.fromBits(ld(addr))
    fun sdf(addr: Int, value: Double) = sd(addr, value.toRawBits())

    /** memcpy: undefined for overlap; fast word-at-a-time. */
    fun memcpy(dst: Int, src: Int, bytes: Int) = FastMem.memcpy(dst, src, bytes)

    /** memmove: overlap-safe, fast word-at-a-time. */
    fun memmove(dst: Int, src: Int, bytes: Int) = FastMem.memmove(dst, src, bytes)

    fun memset(addr: Int, value: Int, bytes: Int) = FastMem.memset(addr, value, bytes)
}

// (Pointer extension utilities live in default-package PointerExtensions.kt to avoid package visibility issues)

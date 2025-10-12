package ai.solace.klang.int.hpc

import ai.solace.klang.bitwise.SwAR128
import ai.solace.klang.mem.GlobalHeap
import ai.solace.klang.mem.KMalloc

/** Heap-backed unsigned 128-bit integer (8 little-endian 16-bit limbs). */
class HeapUInt128 private constructor(val addr: Int) : Comparable<HeapUInt128> {
    fun toIntArray(): IntArray = readLimbs(addr)
    fun toHexString(): String = SwAR128.toBigEndianHex(SwAR128.UInt128(toIntArray()))
    override fun toString(): String = toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeapUInt128) return false
        val a = this.toIntArray()
        val b = other.toIntArray()
        return a.contentEquals(b)
    }

    override fun hashCode(): Int = toIntArray().contentHashCode()

    override fun compareTo(other: HeapUInt128): Int =
        SwAR128.compareUnsigned(SwAR128.UInt128(this.toIntArray()), SwAR128.UInt128(other.toIntArray()))

    operator fun plus(other: HeapUInt128): HeapUInt128 {
        val a = toIntArray()
        val b = other.toIntArray()
        val out = IntArray(SwAR128.LIMB_COUNT)
        val carry = SwAR128.addInto(a, b, out)
        require(carry == 0) { "UInt128 addition overflow" }
        val res = alloc()
        writeLimbs(res.addr, out)
        return res
    }

    operator fun minus(other: HeapUInt128): HeapUInt128 {
        val a = toIntArray()
        val b = other.toIntArray()
        val out = IntArray(SwAR128.LIMB_COUNT)
        val borrow = SwAR128.subInto(a, b, out)
        require(borrow == 0) { "UInt128 subtraction underflow" }
        val res = alloc()
        writeLimbs(res.addr, out)
        return res
    }

    fun shiftLeft(bits: Int): HeapUInt128 {
        val r = SwAR128.shiftLeft(SwAR128.UInt128(toIntArray()), bits)
        require(r.spill == 0uL) { "Shift left overflow beyond 128 bits" }
        val res = alloc()
        writeLimbs(res.addr, r.value.limbs)
        return res
    }

    fun shiftRight(bits: Int): HeapUInt128 {
        val r = SwAR128.shiftRight(SwAR128.UInt128(toIntArray()), bits)
        val res = alloc()
        writeLimbs(res.addr, r.value.limbs)
        return res
    }

    companion object {
        fun alloc(): HeapUInt128 = HeapUInt128(KMalloc.malloc(SwAR128.LIMB_COUNT * 2))
        fun zero(): HeapUInt128 = alloc().also { writeZero(it.addr) }
        fun one(): HeapUInt128 = alloc().also {
            writeZero(it.addr)
            GlobalHeap.sb(it.addr, 1)
        }

        fun fromIntArray(limbs: IntArray): HeapUInt128 {
            require(limbs.size == SwAR128.LIMB_COUNT)
            val h = alloc()
            writeLimbs(h.addr, limbs)
            return h
        }

        fun fromULong(value: ULong): HeapUInt128 = fromIntArray(SwAR128.fromULong(value).limbs)

        // Compatibility helper for code migrated from LimbUInt128
        internal fun fromLimbsUnsafe(limbs: IntArray): HeapUInt128 = fromIntArray(limbs)
    }
}

private fun readLimbs(addr: Int): IntArray = IntArray(SwAR128.LIMB_COUNT) { i ->
    GlobalHeap.lbu(addr + i * 2) or (GlobalHeap.lbu(addr + i * 2 + 1) shl 8)
}

private fun writeLimbs(addr: Int, limbs: IntArray) {
    require(limbs.size == SwAR128.LIMB_COUNT)
    var base = addr
    for (i in 0 until SwAR128.LIMB_COUNT) {
        val v = limbs[i] and 0xFFFF
        GlobalHeap.sb(base, (v and 0xFF).toByte())
        GlobalHeap.sb(base + 1, ((v ushr 8) and 0xFF).toByte())
        base += 2
    }
}

private fun writeZero(addr: Int) {
    GlobalHeap.memset(addr, 0, SwAR128.LIMB_COUNT * 2)
}

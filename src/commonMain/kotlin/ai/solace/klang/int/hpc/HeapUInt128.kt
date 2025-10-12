package ai.solace.klang.int.hpc

import ai.solace.klang.bitwise.SwAR128
import ai.solace.klang.mem.GlobalHeap
import ai.solace.klang.mem.KMalloc

/** 
 * Heap-backed unsigned 128-bit integer (8 little-endian 16-bit limbs).
 * All operations work directly on heap memory without IntArray copies (zero-copy).
 */
class HeapUInt128 private constructor(val addr: Int) : Comparable<HeapUInt128> {
    
    /** 
     * Convert to IntArray for compatibility with SwAR128 array-based operations.
     * Note: This creates a copy and should be avoided in performance-critical code.
     */
    fun toIntArray(): IntArray = readLimbs(addr)
    
    fun toHexString(): String = SwAR128.toBigEndianHex(SwAR128.UInt128(toIntArray()))
    override fun toString(): String = toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeapUInt128) return false
        return SwAR128.compareHeap(this.addr, other.addr) == 0
    }

    override fun hashCode(): Int {
        // Hash based on heap contents
        var result = 1
        for (i in 0 until SwAR128.LIMB_COUNT) {
            val limb = GlobalHeap.lbu(addr + i * 2) or (GlobalHeap.lbu(addr + i * 2 + 1) shl 8)
            result = 31 * result + limb
        }
        return result
    }

    override fun compareTo(other: HeapUInt128): Int = SwAR128.compareHeap(this.addr, other.addr)

    /** Addition: result = this + other (zero-copy) */
    operator fun plus(other: HeapUInt128): HeapUInt128 {
        val res = alloc()
        val carry = SwAR128.addHeap(this.addr, other.addr, res.addr)
        require(carry == 0) { "UInt128 addition overflow" }
        return res
    }

    /** Subtraction: result = this - other (zero-copy) */
    operator fun minus(other: HeapUInt128): HeapUInt128 {
        val res = alloc()
        val borrow = SwAR128.subHeap(this.addr, other.addr, res.addr)
        require(borrow == 0) { "UInt128 subtraction underflow" }
        return res
    }

    /** Shift left by specified bits (zero-copy) */
    fun shiftLeft(bits: Int): HeapUInt128 {
        val res = alloc()
        val spill = SwAR128.shiftLeftHeap(this.addr, res.addr, bits)
        require(spill == 0uL) { "Shift left overflow beyond 128 bits" }
        return res
    }

    /** Shift right by specified bits (zero-copy) */
    fun shiftRight(bits: Int): HeapUInt128 {
        val res = alloc()
        SwAR128.shiftRightHeap(this.addr, res.addr, bits)
        return res
    }

    companion object {
        /** Allocate a new HeapUInt128 (16 bytes for 8 limbs) */
        fun alloc(): HeapUInt128 = HeapUInt128(KMalloc.malloc(SwAR128.LIMB_COUNT * 2))
        
        /** Create a zero-initialized 128-bit integer */
        fun zero(): HeapUInt128 = alloc().also { SwAR128.zeroHeap(it.addr) }
        
        /** Create a 128-bit integer with value 1 */
        fun one(): HeapUInt128 = alloc().also {
            SwAR128.zeroHeap(it.addr)
            GlobalHeap.sb(it.addr, 1)
        }

        /** Create from IntArray (copies data to heap) */
        fun fromIntArray(limbs: IntArray): HeapUInt128 {
            require(limbs.size == SwAR128.LIMB_COUNT)
            val h = alloc()
            writeLimbs(h.addr, limbs)
            return h
        }

        /** Create from ULong (zero-extended to 128 bits) */
        fun fromULong(value: ULong): HeapUInt128 = fromIntArray(SwAR128.fromULong(value).limbs)

        // Compatibility helper for code migrated from LimbUInt128
        internal fun fromLimbsUnsafe(limbs: IntArray): HeapUInt128 = fromIntArray(limbs)
    }
}

/** Read 8 limbs from heap into IntArray (creates a copy) */
private fun readLimbs(addr: Int): IntArray = IntArray(SwAR128.LIMB_COUNT) { i ->
    GlobalHeap.lbu(addr + i * 2) or (GlobalHeap.lbu(addr + i * 2 + 1) shl 8)
}

/** Write 8 limbs from IntArray to heap */
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

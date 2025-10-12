package ai.solace.klang.mem

/** Bit-level helpers on top of GlobalHeap. Little-endian bit numbering. */
object BitTwiddle {
    /** Get a single bit at (addr, bitOffset). */
    fun getBit(addr: Int, bitOffset: Int): Int {
        val byteIndex = addr + (bitOffset ushr 3)
        val bitInByte = bitOffset and 7
        val b = GlobalHeap.lbu(byteIndex)
        return (b ushr bitInByte) and 1
    }

    /** Set a single bit at (addr, bitOffset) to 0/1. */
    fun setBit(addr: Int, bitOffset: Int, value: Int) {
        val byteIndex = addr + (bitOffset ushr 3)
        val bitInByte = bitOffset and 7
        val b = GlobalHeap.lbu(byteIndex)
        val mask = 1 shl bitInByte
        val newB = if ((value and 1) != 0) (b or mask) else (b and mask.inv())
        GlobalHeap.sb(byteIndex, (newB and 0xFF).toByte())
    }

    /** Extract up to 64 bits starting at (addr, bitOffset). Little-endian, returns unsigned in low bits. */
    fun getBitsLE(addr: Int, bitOffset: Int, bitCount: Int): Long {
        require(bitCount in 1..64)
        var out = 0L
        var shift = 0
        var bitsLeft = bitCount
        var cursorBit = bitOffset
        while (bitsLeft > 0) {
            val v = getBit(addr, cursorBit)
            if (v != 0) out = out or (1L shl shift)
            shift++
            cursorBit++
            bitsLeft--
        }
        return out
    }

    /** Insert up to 64 bits into memory at (addr, bitOffset) from low bits of [value]. */
    fun setBitsLE(addr: Int, bitOffset: Int, bitCount: Int, value: Long) {
        require(bitCount in 1..64)
        var v = value
        var cursorBit = bitOffset
        var bitsLeft = bitCount
        while (bitsLeft > 0) {
            setBit(addr, cursorBit, (v and 1L).toInt())
            v = v ushr 1
            cursorBit++
            bitsLeft--
        }
    }
}


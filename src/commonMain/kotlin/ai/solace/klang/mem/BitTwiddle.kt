package ai.solace.klang.mem

/**
 * BitTwiddle: Bit-level manipulation operations on [GlobalHeap] memory.
 *
 * Provides fine-grained bit access and manipulation at arbitrary bit offsets within
 * heap memory. Uses little-endian bit numbering where bit 0 is the LSB of byte 0.
 *
 * ## Bit Numbering
 *
 * Little-endian bit layout within bytes:
 * ```
 * Byte at address A:
 * ┌───┬───┬───┬───┬───┬───┬───┬───┐
 * │ 7 │ 6 │ 5 │ 4 │ 3 │ 2 │ 1 │ 0 │  Bit positions
 * └───┴───┴───┴───┴───┴───┴───┴───┘
 * MSB                           LSB
 * ```
 *
 * ## Use Cases
 *
 * - **Bit fields**: Packing multiple flags/values into minimal space
 * - **Compression**: Working with arbitrary-width encoded values
 * - **Network protocols**: Parsing bit-packed protocol headers
 * - **Cryptography**: Bit-level operations on cipher state
 * - **Hardware interfaces**: Accessing packed register fields
 *
 * ## Performance Note
 *
 * These operations involve byte reads/writes for each bit access. For bulk bit
 * operations, consider working at the byte or word level when possible.
 *
 * @see GlobalHeap For underlying memory operations
 */
object BitTwiddle {
    /**
     * Reads a single bit from memory.
     *
     * @param addr The base byte address in heap memory.
     * @param bitOffset The bit offset from addr (0 = LSB of first byte).
     * @return 0 or 1.
     */
    fun getBit(addr: Int, bitOffset: Int): Int {
        val byteIndex = addr + (bitOffset ushr 3)
        val bitInByte = bitOffset and 7
        val b = GlobalHeap.lbu(byteIndex)
        return (b ushr bitInByte) and 1
    }

    /**
     * Writes a single bit to memory.
     *
     * @param addr The base byte address in heap memory.
     * @param bitOffset The bit offset from addr (0 = LSB of first byte).
     * @param value 0 or 1 (other values are masked).
     */
    fun setBit(addr: Int, bitOffset: Int, value: Int) {
        val byteIndex = addr + (bitOffset ushr 3)
        val bitInByte = bitOffset and 7
        val b = GlobalHeap.lbu(byteIndex)
        val mask = 1 shl bitInByte
        val newB = if ((value and 1) != 0) (b or mask) else (b and mask.inv())
        GlobalHeap.sb(byteIndex, (newB and 0xFF).toByte())
    }

    /**
     * Extracts up to 64 bits from memory at an arbitrary bit offset.
     *
     * Reads bits in little-endian order, packing them into the low bits of a Long.
     * Useful for decoding bit-packed fields that don't align to byte boundaries.
     *
     * @param addr The base byte address in heap memory.
     * @param bitOffset The starting bit offset from addr.
     * @param bitCount The number of bits to extract (1-64).
     * @return The extracted bits as an unsigned value in the low bits of a Long.
     * @throws IllegalArgumentException if bitCount is not in 1..64.
     */
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


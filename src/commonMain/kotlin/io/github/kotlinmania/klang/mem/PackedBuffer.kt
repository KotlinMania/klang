package io.github.kotlinmania.klang.mem

/**
 * PackedBuffer: a `LongArray`-backed memory buffer with direct bit manipulation.
 *
 * This is klang's storage substrate — `GlobalHeap` is built on this, and any
 * other klang component that holds bulk memory should use it (or use
 * `GlobalHeap` directly).
 *
 * ## Why `LongArray` and not `ByteArray` / `UByteArray`?
 *
 * Short answer: Kotlin/Native produces tighter code for `Long` shifts than for
 * any byte-shape alternative, *and* `Byte`/`Short` have no shift operators in
 * stdlib so byte-level work always pays a `Byte.toInt() and 0xFF` round-trip
 * overhead anyway.
 *
 * Long answer: see [`docs/general/why-longarray-storage.md`](../../../../../../../docs/general/why-longarray-storage.md).
 * That document captures the full investigation including:
 *
 *   - exact line numbers in the v2.3.21 stdlib source proving `Byte`/`Short`
 *     have no shift operators (`libraries/stdlib/src/kotlin/Primitives.kt`)
 *   - the `kotlin.experimental` bitwise extensions that promote to `Int`
 *     internally (`libraries/stdlib/src/kotlin/experimental/bitwiseOperations.kt`)
 *   - why `UByteArray.get` is *not* `inline` and pays a function-call boundary
 *     on every access (`libraries/stdlib/unsigned/src/kotlin/UByteArray.kt:29`)
 *   - that `UByte.toInt()` is literally `data.toInt() and 0xFF` — same masking
 *     work the "signed" path does explicitly
 *     (`libraries/stdlib/unsigned/src/kotlin/UByte.kt:331`)
 *   - the empirical benchmark putting `LongArray` at 1.97× faster than
 *     `ByteArray + and 0xFF` and 2.23× faster than `UByteArray` for the
 *     same 4-byte little-endian word load workload, with non-overlapping
 *     95 % CIs (`src/commonBenchmark/.../UByteArrayBenchmark.kt`)
 *
 * ## Quick rationale
 *
 * - `Long.shl/shr/ushr` are stdlib operators returning `Long` — no type
 *   promotion, no implicit sign-extension exposure.
 * - One `Long` carries 8 bytes; a 4-byte LE word read is a slot index, a
 *   shift, and a mask — three ops against an `Int`, not 14 ops against
 *   four byte slots.
 * - No `Byte` is ever materialised. The `Byte.toInt()` sign-extension hazard
 *   that breaks naive ports of zlib / CRC / 8- and 16-bit C code is gone by
 *   construction.
 *
 * ## Memory Layout
 *
 * ```
 * LongArray index:  [0]                    [1]                    ...
 * Byte addresses:   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 ...
 * Bits:            [b0|b1|b2|b3|b4|b5|b6|b7][b8|b9|... ]
 * ```
 *
 * Little-endian within each Long: byte 0 is in bits 0-7, byte 7 is in bits 56-63.
 *
 * @param sizeBytes The size of the buffer in bytes (will be rounded up to multiple of 8)
 */
class PackedBuffer(sizeBytes: Int) {
    /** Number of Longs needed to hold sizeBytes */
    private val longCount = (sizeBytes + 7) ushr 3

    /** The backing LongArray */
    val data: LongArray = LongArray(longCount)

    /** Actual capacity in bytes (rounded up to multiple of 8) */
    val capacity: Int = longCount shl 3

    // ========== Byte Operations ==========

    /**
     * Get unsigned byte at address.
     * @return Value in range 0..255
     */
    fun getByte(addr: Int): Int {
        val idx = addr ushr 3           // addr / 8
        val shift = (addr and 7) shl 3  // (addr % 8) * 8
        return ((data[idx] ushr shift) and 0xFF).toInt()
    }

    /**
     * Set byte at address.
     * @param value Only low 8 bits are used
     */
    fun setByte(addr: Int, value: Int) {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3
        val mask = (0xFFL shl shift).inv()
        data[idx] = (data[idx] and mask) or ((value.toLong() and 0xFF) shl shift)
    }

    // ========== 16-bit (Short/Half) Operations ==========

    /**
     * Get signed 16-bit value at address (little-endian).
     * Address should be 2-byte aligned for best performance.
     */
    fun getShort(addr: Int): Short {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3

        return if (shift <= 48) {
            // Entirely within one Long
            ((data[idx] ushr shift) and 0xFFFF).toShort()
        } else {
            // Spans two Longs
            val lo = (data[idx] ushr shift).toInt() and 0xFF
            val hi = (data[idx + 1].toInt() and 0xFF) shl 8
            (lo or hi).toShort()
        }
    }

    /**
     * Set 16-bit value at address (little-endian).
     */
    fun setShort(addr: Int, value: Short) {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3
        val v = value.toInt() and 0xFFFF

        if (shift <= 48) {
            // Entirely within one Long
            val mask = (0xFFFFL shl shift).inv()
            data[idx] = (data[idx] and mask) or (v.toLong() shl shift)
        } else {
            // Spans two Longs - write byte by byte
            setByte(addr, v and 0xFF)
            setByte(addr + 1, (v ushr 8) and 0xFF)
        }
    }

    // ========== 32-bit (Int/Word) Operations ==========

    /**
     * Get signed 32-bit value at address (little-endian).
     * Address should be 4-byte aligned for best performance.
     */
    fun getInt(addr: Int): Int {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3

        return if (shift <= 32) {
            // Entirely within one Long
            ((data[idx] ushr shift) and 0xFFFFFFFFL).toInt()
        } else {
            // Spans two Longs
            val bitsFromFirst = 64 - shift
            val bitsFromSecond = 32 - bitsFromFirst
            val lo = (data[idx] ushr shift).toInt()
            val hi = (data[idx + 1].toInt() and ((1 shl bitsFromSecond) - 1)) shl bitsFromFirst
            lo or hi
        }
    }

    /**
     * Set 32-bit value at address (little-endian).
     */
    fun setInt(addr: Int, value: Int) {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3

        if (shift <= 32) {
            // Entirely within one Long
            val mask = (0xFFFFFFFFL shl shift).inv()
            data[idx] = (data[idx] and mask) or ((value.toLong() and 0xFFFFFFFFL) shl shift)
        } else {
            // Spans two Longs
            val bitsInFirst = 64 - shift
            val bitsInSecond = 32 - bitsInFirst

            // Write to first Long
            val mask1 = (-1L ushr bitsInFirst).inv()  // Clear upper bits
            data[idx] = (data[idx] and mask1) or ((value.toLong() and 0xFFFFFFFFL) shl shift)

            // Write to second Long
            val mask2 = (-1L shl bitsInSecond)  // Clear lower bits
            data[idx + 1] = (data[idx + 1] and mask2) or ((value.toLong() and 0xFFFFFFFFL) ushr bitsInFirst)
        }
    }

    // ========== 64-bit (Long/Doubleword) Operations ==========

    /**
     * Get 64-bit value at address (little-endian).
     * Address should be 8-byte aligned for best performance.
     */
    fun getLong(addr: Int): Long {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3

        return if (shift == 0) {
            // Aligned - direct access
            data[idx]
        } else {
            // Unaligned - spans two Longs
            val lo = data[idx] ushr shift
            val hi = data[idx + 1] shl (64 - shift)
            lo or hi
        }
    }

    /**
     * Set 64-bit value at address (little-endian).
     */
    fun setLong(addr: Int, value: Long) {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3

        if (shift == 0) {
            // Aligned - direct access
            data[idx] = value
        } else {
            // Unaligned - spans two Longs
            val bitsInFirst = 64 - shift

            // Write to first Long (preserve lower bits, set upper bits)
            val mask1 = (1L shl shift) - 1  // Lower 'shift' bits
            data[idx] = (data[idx] and mask1) or (value shl shift)

            // Write to second Long (set lower bits, preserve upper bits)
            val mask2 = (-1L shl (64 - bitsInFirst))  // Upper bits
            data[idx + 1] = (data[idx + 1] and mask2) or (value ushr bitsInFirst)
        }
    }

    // ========== Float/Double Operations ==========

    fun getFloat(addr: Int): Float = Float.fromBits(getInt(addr))
    fun setFloat(addr: Int, value: Float) = setInt(addr, value.toRawBits())

    fun getDouble(addr: Int): Double = Double.fromBits(getLong(addr))
    fun setDouble(addr: Int, value: Double) = setLong(addr, value.toRawBits())

    // ========== Bulk Operations ==========

    /**
     * Fill region with byte value.
     */
    fun fill(addr: Int, value: Int, count: Int) {
        if (count <= 0) return

        val byteVal = value.toLong() and 0xFF
        // Replicate byte across all 8 positions in a Long
        val wordVal = byteVal or (byteVal shl 8) or (byteVal shl 16) or (byteVal shl 24) or
                (byteVal shl 32) or (byteVal shl 40) or (byteVal shl 48) or (byteVal shl 56)

        var pos = addr
        var remaining = count

        // Handle unaligned start
        while (remaining > 0 && (pos and 7) != 0) {
            setByte(pos++, value)
            remaining--
        }

        // Bulk fill aligned Longs
        val idx = pos ushr 3
        val fullLongs = remaining ushr 3
        for (i in 0 until fullLongs) {
            data[idx + i] = wordVal
        }
        pos += fullLongs shl 3
        remaining -= fullLongs shl 3

        // Handle unaligned tail
        while (remaining-- > 0) {
            setByte(pos++, value)
        }
    }

    /**
     * Copy from src region to dst region (non-overlapping).
     */
    fun copy(dstAddr: Int, srcAddr: Int, count: Int) {
        if (count <= 0 || dstAddr == srcAddr) return

        // Simple byte-by-byte for now (can optimize for aligned cases)
        for (i in 0 until count) {
            setByte(dstAddr + i, getByte(srcAddr + i))
        }
    }

    /**
     * Copy with overlap support.
     */
    fun move(dstAddr: Int, srcAddr: Int, count: Int) {
        if (count <= 0 || dstAddr == srcAddr) return

        if (dstAddr < srcAddr || dstAddr >= srcAddr + count) {
            // No overlap or dst before src - copy forward
            for (i in 0 until count) {
                setByte(dstAddr + i, getByte(srcAddr + i))
            }
        } else {
            // Overlap with dst after src - copy backward
            for (i in count - 1 downTo 0) {
                setByte(dstAddr + i, getByte(srcAddr + i))
            }
        }
    }

    /**
     * Clear entire buffer to zero.
     */
    fun clear() {
        data.fill(0L)
    }
}

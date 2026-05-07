package ai.solace.klang.fp

import ai.solace.klang.bitwise.CUE4M3Math

/**
 * CUE4M3: unsigned 8-bit float with 4 exponent bits (bias = 7) and 3 mantissa
 * bits. Layout (high to low): EEEE MMM (no sign bit).
 *
 * Raw decoded value:
 *   x == 0           → 0.0
 *   x == 0x7F        → 0.0 (special)
 *   exp == 0         → subnormal: man × 2^(−9)
 *   otherwise        → (1 + man/8) × 2^(exp − 7)
 *
 * The [toFloat] accessor returns the raw value scaled by 0.5 (the
 * half-magnitude variant); use [decodeRaw] when you need the unscaled value.
 * All bit work routes through [CUE4M3Math] in the bitwise package.
 *
 * Storage uses [Int] for overflow headroom during intermediate calculations
 * (matching the [CFloat16] convention); only the low 8 bits are meaningful.
 */
class CUE4M3 private constructor(private val bits: Int) {

    /** Raw 8-bit storage as an Int (low 8 bits used). */
    fun toBits(): Int = bits and 0xFF

    /** Raw 8-bit storage as a UByte. */
    fun toUByte(): UByte = (bits and 0xFF).toUByte()

    /** Decode to float32 in the half-magnitude variant (raw × 0.5). */
    fun toFloat(): Float = CUE4M3Math.decode(bits)

    /** Decode to float32 without the half-magnitude scaling (raw value). */
    fun decodeRaw(): Float = CUE4M3Math.decodeRaw(bits)

    override fun toString(): String = "CUE4M3(0x${toBits().toString(16).padStart(2, '0')}, ${toFloat()})"

    override fun equals(other: Any?): Boolean = other is CUE4M3 && other.toBits() == toBits()
    override fun hashCode(): Int = toBits()

    companion object {
        /** The all-zero CUE4M3, which decodes to 0.0. */
        val ZERO: CUE4M3 = CUE4M3(0)

        /** Construct from raw 8-bit storage. */
        fun fromBits(bits: UByte): CUE4M3 = CUE4M3(bits.toInt())

        /** Construct from raw 8-bit storage given as Int (low 8 bits used). */
        fun fromBits(bits: Int): CUE4M3 = CUE4M3(bits and 0xFF)

        /**
         * Encode a float32 value into CUE4M3 (round-to-nearest, ties broken by mantissa LSB):
         *  - Negative or NaN inputs encode to 0
         *  - Saturates at 448.0 (the maximum representable raw magnitude)
         *  - Returns 0x7E for overflow (max finite encoding)
         */
        fun fromFloat(f: Float): CUE4M3 = CUE4M3(CUE4M3Math.encodeBits(f))
    }
}

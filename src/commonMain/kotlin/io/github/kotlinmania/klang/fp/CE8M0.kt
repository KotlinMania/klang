package io.github.kotlinmania.klang.fp

import io.github.kotlinmania.klang.bitwise.CE8M0Math

/**
 * CE8M0: 8-bit exponent-only floating-point scaling factor.
 *
 * Layout: 8 bits of unsigned biased exponent, no sign, no mantissa.
 * Value semantics:
 *   x == 0     → 2^(-127), encoded as a denormal float
 *   otherwise  → 2^(x − 127)
 *
 * NaN handling is intentionally disabled.
 *
 * This is a pure scaling format — arithmetic is not defined. Convert to
 * float32 via [toFloat] (or [toFloatHalf] for the half-magnitude variant
 * where the decoded value is 2^(x − 128) instead of 2^(x − 127)) and apply
 * the scale at the call site. All bit work routes through [CE8M0Math] in
 * the bitwise package.
 *
 * Storage uses [Int] for overflow headroom during intermediate calculations
 * (matching the [CFloat16] convention); only the low 8 bits are meaningful.
 */
class CE8M0 private constructor(private val bits: Int) {

    /** Raw 8-bit storage as an Int (low 8 bits used). */
    fun toBits(): Int = bits and 0xFF

    /** Raw 8-bit storage as a UByte. */
    fun toUByte(): UByte = (bits and 0xFF).toUByte()

    /** Decode to float32: 2^(x − 127), or 2^(−127) when x == 0. */
    fun toFloat(): Float = Float.fromBits(CE8M0Math.toFp32Bits(bits))

    /**
     * Decode to the half-magnitude variant: 2^(x − 128) instead of 2^(x − 127).
     * Equivalent to `toFloat() * 0.5` for x ≤ 254; at x == 255 the half form
     * deliberately stays finite (2^127) where [toFloat] saturates to +Infinity.
     */
    fun toFloatHalf(): Float = Float.fromBits(CE8M0Math.toFp32HalfBits(bits))

    override fun toString(): String = "CE8M0(0x${toBits().toString(16).padStart(2, '0')}, ${toFloat()})"

    override fun equals(other: Any?): Boolean = other is CE8M0 && other.toBits() == toBits()
    override fun hashCode(): Int = toBits()

    companion object {
        /** The all-zero CE8M0, which decodes to 2^(−127). */
        val ZERO: CE8M0 = CE8M0(0)

        /** Construct from raw 8-bit storage. */
        fun fromBits(bits: UByte): CE8M0 = CE8M0(bits.toInt())

        /** Construct from raw 8-bit storage given as Int (low 8 bits used). */
        fun fromBits(bits: Int): CE8M0 = CE8M0(bits and 0xFF)
    }
}

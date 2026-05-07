package io.github.kotlinmania.klang.bitwise

/**
 * CE8M0Math — bit kernel for the 8-bit exponent-only scaling format.
 *
 * Format: 8 bits of unsigned biased exponent, no sign, no mantissa.
 * Value semantics:
 *   x == 0     → 2^(-127), encoded as a denormal float
 *   otherwise  → 2^(x − 127)
 *
 * NaN handling is intentionally disabled.
 *
 * @native-bitshift-allowed Kernel layer for CE8M0; raw shifts permitted.
 */
object CE8M0Math {
    /** Bit position of the IEEE-754 binary32 exponent field (mantissa is 23 bits). */
    private const val FP32_EXP_SHIFT = 23

    /**
     * IEEE-754 binary32 bit pattern for 2^(-127), expressed as a denormal:
     * sign = 0, exp = 0, mantissa = 0x400000 (i.e. 0.5 × 2^(-126) = 2^(-127)).
     */
    private const val DENORMAL_TWO_TO_MINUS_127 = 0x00400000

    /**
     * IEEE-754 binary32 bit pattern for 2^(-128) as a denormal:
     * sign = 0, exp = 0, mantissa = 0x200000 (i.e. 0.25 × 2^(-126) = 2^(-128)).
     */
    private const val DENORMAL_TWO_TO_MINUS_128 = 0x00200000

    /** Mask isolating the low 8 bits of an Int. */
    private const val BYTE_MASK = 0xFF

    /**
     * Convert a CE8M0 byte (0..255) to the fp32 raw bit pattern of 2^(x − 127).
     *
     * @param bits the 8-bit CE8M0 storage as an Int (low 8 bits used)
     * @return raw IEEE-754 binary32 bit pattern
     */
    fun toFp32Bits(bits: Int): Int {
        val xi = bits and BYTE_MASK
        return if (xi == 0) DENORMAL_TWO_TO_MINUS_127 else (xi shl FP32_EXP_SHIFT)
    }

    /**
     * Convert a CE8M0 byte to the fp32 raw bit pattern of 2^(x − 128) — the
     * half-magnitude variant. Unlike [toFp32Bits] it does not overflow to
     * infinity at the top of the range; instead the highest encoding decodes
     * to 2^127.
     *
     * @param bits the 8-bit CE8M0 storage as an Int (low 8 bits used)
     * @return raw IEEE-754 binary32 bit pattern
     */
    fun toFp32HalfBits(bits: Int): Int {
        val xi = bits and BYTE_MASK
        return if (xi < 2) (DENORMAL_TWO_TO_MINUS_128 shl xi) else ((xi - 1) shl FP32_EXP_SHIFT)
    }
}

package ai.solace.klang.bitwise

/**
 * CUE4M3Math — bit kernel for the unsigned 8-bit float with 4 exponent bits
 * (bias = 7) and 3 mantissa bits.
 *
 * Layout (high to low): EEEE MMM (no sign bit)
 *   bits 7..3: 4-bit exponent E (biased by 7)
 *   bits 2..0: 3-bit mantissa M
 *
 * Raw decoded value:
 *   x == 0           → 0.0
 *   x == 0x7F        → 0.0 (special)
 *   exp == 0         → subnormal: man × 2^(−9)
 *   otherwise        → (1 + man/8) × 2^(exp − 7)
 *
 * [decode] returns the raw value scaled by 0.5 (the half-magnitude variant);
 * [decodeRaw] returns the unscaled value.
 *
 * Encoding ([encodeBits]) clamps non-positive and NaN inputs to 0, saturates
 * positive inputs at 448.0, and returns 0x7E for overflow (the max finite
 * encoding).
 *
 * @native-bitshift-allowed Kernel layer for CUE4M3; raw shifts permitted.
 */
object CUE4M3Math {
    // ---- UE4M3 layout constants ----
    private const val MAN_BITS = 3
    private const val MAN_MASK = 0x7
    private const val EXP_MASK = 0xF
    private const val EXP_BIAS = 7
    private const val EXP_MAX = 14

    /** All-zero encoding. */
    const val ZERO_ENCODING = 0
    /** Saturation encoding ((EXP_BIAS + EXP_MAX) << MAN_BITS) | (MAN_MASK − 1). */
    const val SATURATED_ENCODING = 0x7E
    /** Special "all ones except low bit" encoding that decodes to zero. */
    const val SPECIAL_ZERO_ENCODING = 0x7F

    // ---- IEEE-754 binary32 layout ----
    private const val FP32_EXP_SHIFT = 23
    private const val FP32_MAN_SHIFT_FOR_UE4M3 = 20
    private const val FP32_ROUND_BIT_SHIFT = 19
    private const val FP32_EXP_MASK = 0xFF
    private const val FP32_EXP_BIAS = 127

    // ---- Float helpers ----
    private const val SUBNORMAL_SCALE = 1.0f / 512.0f      // 2^(-9)
    private const val MAN_DIVISOR = 8.0f                   // 2^MAN_BITS
    private const val SUBNORMAL_ENCODE_SCALE = 512.0f      // 2^(MAN_BITS + EXP_BIAS - 1)
    private const val MAX_RAW_VALUE = 448.0f               // saturation magnitude before doubling
    private const val BYTE_MASK = 0xFF

    /**
     * Decode a CUE4M3 byte to a raw fp32 value (no half-magnitude scaling applied).
     */
    fun decodeRaw(bits: Int): Float {
        val xi = bits and BYTE_MASK
        if (xi == ZERO_ENCODING || xi == SPECIAL_ZERO_ENCODING) return 0.0f
        val exp = (xi ushr MAN_BITS) and EXP_MASK
        val man = xi and MAN_MASK
        return if (exp == 0) {
            // subnormal: man × 2^(−9)
            man.toFloat() * SUBNORMAL_SCALE
        } else {
            // normalized: (1 + man/8) × 2^(exp − 7)
            (1.0f + man.toFloat() / MAN_DIVISOR) * twoToThe(exp - EXP_BIAS)
        }
    }

    /**
     * Decode a CUE4M3 byte to fp32 in the half-magnitude variant (raw value × 0.5).
     */
    fun decode(bits: Int): Float = decodeRaw(bits) * 0.5f

    /**
     * Encode a fp32 value into a CUE4M3 byte using round-to-nearest semantics:
     *  - Negative or NaN inputs → 0
     *  - Saturates at 448.0 (max representable raw magnitude)
     *  - Returns 0x7E for overflow
     */
    fun encodeBits(f: Float): Int {
        if (!(f > 0.0f)) return ZERO_ENCODING
        val saturated = if (f > MAX_RAW_VALUE) MAX_RAW_VALUE else f

        val raw = saturated.toRawBits()
        val fp32Exp = ((raw ushr FP32_EXP_SHIFT) and FP32_EXP_MASK) - FP32_EXP_BIAS
        val fp32Man = (raw ushr FP32_MAN_SHIFT_FOR_UE4M3) and MAN_MASK
        var ue4m3Exp = fp32Exp + EXP_BIAS

        if (ue4m3Exp <= 0) {
            // subnormal: value = man × 2^(−9), man = round(x × 2^9)
            var man = (saturated * SUBNORMAL_ENCODE_SCALE + 0.5f).toInt()
            if (man > MAN_MASK) man = MAN_MASK
            if (man < 1) return ZERO_ENCODING
            return man and BYTE_MASK
        }
        if (ue4m3Exp > EXP_MAX) return SATURATED_ENCODING

        val roundBit = (raw ushr FP32_ROUND_BIT_SHIFT) and 1
        var ue4m3Man = fp32Man + roundBit
        if (ue4m3Man > MAN_MASK) {
            ue4m3Man = 0
            ue4m3Exp++
            if (ue4m3Exp > EXP_MAX) return SATURATED_ENCODING
        }
        return ((ue4m3Exp shl MAN_BITS) or ue4m3Man) and BYTE_MASK
    }

    /**
     * Build a fp32 power-of-two for an unbiased exponent in the valid range.
     * For UE4M3 the unbiased exponent is in [−7, 7] so this is always exact.
     */
    private fun twoToThe(unbiasedExp: Int): Float {
        return if (unbiasedExp in -126..127) {
            Float.fromBits((unbiasedExp + FP32_EXP_BIAS) shl FP32_EXP_SHIFT)
        } else {
            // Defensive fallback (never expected for valid UE4M3 inputs).
            var result = 1.0f
            if (unbiasedExp > 0) repeat(unbiasedExp) { result *= 2.0f }
            else repeat(-unbiasedExp) { result *= 0.5f }
            result
        }
    }
}

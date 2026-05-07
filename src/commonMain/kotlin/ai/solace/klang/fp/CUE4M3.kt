package ai.solace.klang.fp

/**
 * CUE4M3: unsigned 8-bit float with 4 exponent bits (bias = 7) and 3 mantissa bits.
 *
 * Layout (high to low): EEEE MMM (no sign bit)
 *   bits 7..4: 4-bit exponent E (biased by 7)
 *   bits 2..0: 3-bit mantissa M
 *
 * Decoded value (before the kvalues_mxfp4 doubling convention):
 *   x == 0           -> 0.0
 *   x == 0x7F        -> 0.0 (special)
 *   exp == 0         -> subnormal: man * 2^(-9)
 *   otherwise        -> (1 + man/8) * 2^(exp - 7)
 *
 * The `toFloat()` accessor returns value * 0.5 to match the kvalues_mxfp4 convention
 * where kvalues = 2 * E2M1_float. Use [decodeRaw] for the pre-doubled value.
 */
class CUE4M3 private constructor(private val bits: UByte) {

    /** Raw 8-bit storage. */
    fun toBits(): UByte = bits

    /**
     * Decode to float32 with kvalues_mxfp4 doubling convention applied (raw * 0.5).
     */
    fun toFloat(): Float = decodeRaw() * 0.5f

    /**
     * Decode to float32 without the kvalues doubling.
     */
    fun decodeRaw(): Float {
        val xi = bits.toInt() and 0xFF
        if (xi == 0 || xi == 0x7F) return 0.0f
        val exp = (xi ushr 3) and 0xF
        val man = xi and 0x7
        return if (exp == 0) {
            // subnormal: man * 2^(-9)
            man.toFloat() * (1.0f / 512.0f)
        } else {
            // normalized: (1 + man/8) * 2^(exp - 7)
            (1.0f + man.toFloat() / 8.0f) * twoToThe(exp - 7)
        }
    }

    override fun toString(): String = "CUE4M3(0x${bits.toInt().and(0xFF).toString(16).padStart(2, '0')}, ${toFloat()})"

    override fun equals(other: Any?): Boolean = other is CUE4M3 && other.bits == bits
    override fun hashCode(): Int = bits.toInt()

    companion object {
        /** Construct from raw 8-bit storage. */
        fun fromBits(bits: UByte): CUE4M3 = CUE4M3(bits)

        /** Construct from raw 8-bit storage given as Int (low 8 bits used). */
        fun fromBits(bits: Int): CUE4M3 = CUE4M3((bits and 0xFF).toUByte())

        /**
         * Encode a float32 value into CUE4M3 (round-to-nearest, ties broken by mantissa LSB).
         *
         * Behaviour matches `ggml_fp32_to_ue4m3` from upstream:
         *  - Negative or NaN inputs encode to 0
         *  - Saturates at 448.0 (the maximum representable magnitude before doubling)
         *  - Returns 0x7E for overflow (max finite encoding)
         */
        fun fromFloat(f: Float): CUE4M3 {
            if (!(f > 0.0f)) return CUE4M3(0u)
            val saturated = if (f > 448.0f) 448.0f else f

            val raw = saturated.toRawBits()
            val fp32Exp = ((raw ushr 23) and 0xFF) - 127
            val fp32Man = (raw ushr 20) and 0x7
            var ue4m3Exp = fp32Exp + 7
            if (ue4m3Exp <= 0) {
                // subnormal: value = man * 2^(-9), man = round(x * 2^9)
                var man = (saturated * 512.0f + 0.5f).toInt()
                if (man > 7) man = 7
                if (man < 1) return CUE4M3(0u)
                return CUE4M3(man.toUByte())
            }
            if (ue4m3Exp >= 15) return CUE4M3(0x7Eu)

            val roundBit = (raw ushr 19) and 1
            var ue4m3Man = fp32Man + roundBit
            if (ue4m3Man > 7) {
                ue4m3Man = 0
                ue4m3Exp++
                if (ue4m3Exp >= 15) return CUE4M3(0x7Eu)
            }
            return CUE4M3(((ue4m3Exp shl 3) or ue4m3Man).toUByte())
        }

        /**
         * Fast 2^n constructor for small integer n covering the valid float32 exponent range.
         * For UE4M3 the exponent (after biasing) is in [-7, 7] so this is always exact.
         */
        private fun twoToThe(n: Int): Float {
            return if (n in -126..127) {
                Float.fromBits((n + 127) shl 23)
            } else {
                // Out of range fallback (defensive; should not occur for valid UE4M3 inputs).
                var result = 1.0f
                if (n > 0) repeat(n) { result *= 2.0f }
                else repeat(-n) { result *= 0.5f }
                result
            }
        }
    }
}

/** Convenience: equivalent to CUE4M3.fromBits(x).toFloat(). */
fun ue4m3ToFp32(x: UByte): Float = CUE4M3.fromBits(x).toFloat()

/** Convenience: equivalent to CUE4M3.fromFloat(x).toBits(). */
fun fp32ToUe4m3(x: Float): UByte = CUE4M3.fromFloat(x).toBits()

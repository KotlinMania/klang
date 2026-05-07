package ai.solace.klang.fp

import ai.solace.klang.bitwise.BitShiftConfig
import ai.solace.klang.bitwise.BitShiftEngine

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
 * The [toFloat] accessor returns value * 0.5 to match the kvalues_mxfp4 convention
 * where kvalues = 2 * E2M1_float. Use [decodeRaw] for the pre-doubled value.
 *
 * All bitwise operations route through [BitShiftEngine] per klang house rule
 * (no raw shifts / hard-coded masks outside the engine).
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
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        val xi = engine.bitwiseAnd(bits.toLong(), engine.getMask(8)).toInt()
        if (xi == ZERO_ENCODING || xi == SPECIAL_ZERO_ENCODING) return 0.0f
        val exp = engine.bitwiseAnd(
            engine.unsignedRightShift(xi.toLong(), MAN_BITS).value,
            engine.getMask(EXP_BITS)
        ).toInt()
        val man = engine.bitwiseAnd(xi.toLong(), engine.getMask(MAN_BITS)).toInt()
        return if (exp == 0) {
            // subnormal: man * 2^(-9) = man / 512
            man.toFloat() * SUBNORMAL_SCALE
        } else {
            // normalized: (1 + man/8) * 2^(exp - 7)
            (1.0f + man.toFloat() / MAN_DIVISOR) * twoToThe(exp - EXP_BIAS)
        }
    }

    override fun toString(): String {
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        val xi = engine.bitwiseAnd(bits.toLong(), engine.getMask(8))
        return "CUE4M3(0x${xi.toString(16).padStart(2, '0')}, ${toFloat()})"
    }

    override fun equals(other: Any?): Boolean = other is CUE4M3 && other.bits == bits
    override fun hashCode(): Int {
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        return engine.bitwiseAnd(bits.toLong(), engine.getMask(8)).toInt()
    }

    companion object {
        // ---- Layout constants ----
        /** Number of mantissa bits in the UE4M3 layout. */
        private const val MAN_BITS: Int = 3
        /** Number of exponent bits in the UE4M3 layout. */
        private const val EXP_BITS: Int = 4
        /** Exponent bias for UE4M3. */
        private const val EXP_BIAS: Int = 7
        /** Maximum encodable biased exponent before saturation (inclusive). */
        private const val EXP_MAX: Int = 14
        /** Saturation encoding ((EXP_BIAS + EXP_MAX) << MAN_BITS) | (MAN_DIVISOR-2). */
        private const val SATURATED_ENCODING: Int = 0x7E
        /** Special "all ones except low bit" encoding that decodes to zero. */
        private const val SPECIAL_ZERO_ENCODING: Int = 0x7F
        /** All-zero encoding. */
        private const val ZERO_ENCODING: Int = 0
        /** Saturation magnitude before doubling. */
        private const val MAX_RAW_VALUE: Float = 448.0f

        // ---- Float helpers ----
        /** 2^(-9) = 1/512, used for subnormal scaling. */
        private const val SUBNORMAL_SCALE: Float = 1.0f / 512.0f
        /** Mantissa divisor (2^MAN_BITS). */
        private const val MAN_DIVISOR: Float = 8.0f
        /** Subnormal encoder factor: 2^(MAN_BITS + EXP_BIAS - 1) = 512.0. */
        private const val SUBNORMAL_ENCODE_SCALE: Float = 512.0f

        // ---- IEEE 754 binary32 layout constants used during encoding ----
        /** Bit position of the binary32 exponent field. */
        private const val FP32_EXP_SHIFT: Int = 23
        /** Bit position of the LSB of the UE4M3 mantissa within a fp32 mantissa. */
        private const val FP32_MAN_SHIFT_FOR_UE4M3: Int = 20
        /** Bit position of the UE4M3 round bit within a fp32 mantissa. */
        private const val FP32_ROUND_BIT_SHIFT: Int = 19
        /** Width of the binary32 exponent field. */
        private const val FP32_EXP_BITS: Int = 8
        /** Bias of the binary32 exponent. */
        private const val FP32_EXP_BIAS: Int = 127

        /** Construct from raw 8-bit storage. */
        fun fromBits(bits: UByte): CUE4M3 = CUE4M3(bits)

        /** Construct from raw 8-bit storage given as Int (low 8 bits used). */
        fun fromBits(bits: Int): CUE4M3 {
            val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
            val masked = engine.bitwiseAnd(bits.toLong(), engine.getMask(8))
            return CUE4M3(masked.toUByte())
        }

        /**
         * Encode a float32 value into CUE4M3 (round-to-nearest, ties broken by mantissa LSB).
         *
         * Behaviour matches `ggml_fp32_to_ue4m3` from upstream:
         *  - Negative or NaN inputs encode to 0
         *  - Saturates at 448.0 (the maximum representable magnitude before doubling)
         *  - Returns 0x7E for overflow (max finite encoding)
         */
        fun fromFloat(f: Float): CUE4M3 {
            if (!(f > 0.0f)) return CUE4M3(ZERO_ENCODING.toUByte())
            val saturated = if (f > MAX_RAW_VALUE) MAX_RAW_VALUE else f

            val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
            val raw = saturated.toRawBits().toLong()
            val fp32Exp = engine.bitwiseAnd(
                engine.unsignedRightShift(raw, FP32_EXP_SHIFT).value,
                engine.getMask(FP32_EXP_BITS)
            ).toInt() - FP32_EXP_BIAS
            val fp32Man = engine.bitwiseAnd(
                engine.unsignedRightShift(raw, FP32_MAN_SHIFT_FOR_UE4M3).value,
                engine.getMask(MAN_BITS)
            ).toInt()
            var ue4m3Exp = fp32Exp + EXP_BIAS
            if (ue4m3Exp <= 0) {
                // subnormal: value = man * 2^(-9), man = round(x * 2^9)
                var man = (saturated * SUBNORMAL_ENCODE_SCALE + 0.5f).toInt()
                val maxSubnormal = engine.getMask(MAN_BITS).toInt()
                if (man > maxSubnormal) man = maxSubnormal
                if (man < 1) return CUE4M3(ZERO_ENCODING.toUByte())
                return CUE4M3(man.toUByte())
            }
            if (ue4m3Exp > EXP_MAX) return CUE4M3(SATURATED_ENCODING.toUByte())

            val roundBit = engine.bitwiseAnd(
                engine.unsignedRightShift(raw, FP32_ROUND_BIT_SHIFT).value,
                engine.getMask(1)
            ).toInt()
            var ue4m3Man = fp32Man + roundBit
            val maxMan = engine.getMask(MAN_BITS).toInt()
            if (ue4m3Man > maxMan) {
                ue4m3Man = 0
                ue4m3Exp++
                if (ue4m3Exp > EXP_MAX) return CUE4M3(SATURATED_ENCODING.toUByte())
            }
            val expField = engine.leftShift(ue4m3Exp.toLong(), MAN_BITS).value
            val combined = engine.bitwiseOr(expField, ue4m3Man.toLong())
            return CUE4M3(combined.toUByte())
        }

        /**
         * Fast 2^n constructor for small integer n covering the valid float32 exponent range.
         * For UE4M3 the exponent (after biasing) is in [-7, 7] so this is always exact.
         *
         * Builds the IEEE-754 binary32 bit pattern (n + 127) << 23 via [BitShiftEngine].
         */
        private fun twoToThe(n: Int): Float {
            return if (n in -126..127) {
                val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
                Float.fromBits(engine.leftShift((n + FP32_EXP_BIAS).toLong(), FP32_EXP_SHIFT).value.toInt())
            } else {
                // Out-of-range fallback (defensive; should not occur for valid UE4M3 inputs).
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

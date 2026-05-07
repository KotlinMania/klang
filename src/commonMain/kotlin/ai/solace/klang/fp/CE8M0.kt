package ai.solace.klang.fp

import ai.solace.klang.bitwise.BitShiftConfig
import ai.solace.klang.bitwise.BitShiftEngine

/**
 * CE8M0: 8-bit exponent-only floating point format used as the per-block
 * scaling factor in MX (Microscaling) and MXFP4 quantization.
 *
 * Layout: 8 bits of unsigned biased exponent, no sign, no mantissa.
 * Value semantics:
 *   x == 0     -> 2^(-127), encoded as a denormal float
 *   otherwise  -> 2^(x - 127)
 *
 * NaN handling is intentionally disabled (matches upstream MX spec usage).
 *
 * This is a pure scaling format: arithmetic is not defined. Use it to convert
 * to/from float32 and apply the scale at the call site.
 *
 * All bitwise operations route through [BitShiftEngine] per klang house rule
 * (no raw shifts / hard-coded masks outside the engine).
 */
class CE8M0 private constructor(private val bits: UByte) {

    /** Raw 8-bit storage. */
    fun toBits(): UByte = bits

    /**
     * Decode to float32.
     *
     * For x == 0 we emit the denormal bit pattern 0x00400000 = 0.5 * 2^(-126) = 2^(-127).
     * For x > 0 we shift x into the exponent field, producing 2^(x - 127).
     */
    fun toFloat(): Float {
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        val xi = engine.bitwiseAnd(bits.toLong(), engine.getMask(8))
        val raw: Long = if (xi == 0L) {
            // 2^(-127) expressed as a denormal: sign=0, exp=0, mantissa=0x400000
            DENORMAL_TWO_TO_MINUS_127
        } else {
            engine.leftShift(xi, EXP_FIELD_SHIFT).value
        }
        return Float.fromBits(raw.toInt())
    }

    /**
     * Decode to float32 divided by 2 (kvalues_mxfp4 convention where kvalues = 2 * E2M1_float).
     *
     * For x < 2 we emit precomputed denormal patterns (0x00200000 = 2^(-128)).
     * For x >= 2 we use a normalized exponent adjustment: 0.5 * 2^(x-127) = 2^(x-128).
     */
    fun toFloatHalf(): Float {
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        val xi = engine.bitwiseAnd(bits.toLong(), engine.getMask(8))
        val raw: Long = if (xi < 2L) {
            // 0x00200000 = 2^(-128), 0x00400000 = 2^(-127)
            engine.leftShift(DENORMAL_TWO_TO_MINUS_128, xi.toInt()).value
        } else {
            engine.leftShift(xi - 1L, EXP_FIELD_SHIFT).value
        }
        return Float.fromBits(raw.toInt())
    }

    override fun toString(): String {
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        val xi = engine.bitwiseAnd(bits.toLong(), engine.getMask(8))
        return "CE8M0(0x${xi.toString(16).padStart(2, '0')}, ${toFloat()})"
    }

    override fun equals(other: Any?): Boolean = other is CE8M0 && other.bits == bits
    override fun hashCode(): Int {
        val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
        return engine.bitwiseAnd(bits.toLong(), engine.getMask(8)).toInt()
    }

    companion object {
        /** Bit position of the IEEE-754 binary32 exponent field (mantissa is 23 bits). */
        private const val EXP_FIELD_SHIFT: Int = 23

        /**
         * IEEE-754 binary32 bit pattern for 2^(-127), expressed as a denormal:
         * sign = 0, exp = 0, mantissa = 0x400000 (i.e. 0.5 * 2^(-126) = 2^(-127)).
         */
        private const val DENORMAL_TWO_TO_MINUS_127: Long = 0x00400000L

        /**
         * IEEE-754 binary32 bit pattern for 2^(-128) as a denormal:
         * sign = 0, exp = 0, mantissa = 0x200000 (i.e. 0.25 * 2^(-126) = 2^(-128)).
         */
        private const val DENORMAL_TWO_TO_MINUS_128: Long = 0x00200000L

        /** Construct from raw 8-bit storage. */
        fun fromBits(bits: UByte): CE8M0 = CE8M0(bits)

        /** Construct from raw 8-bit storage given as Int (low 8 bits used). */
        fun fromBits(bits: Int): CE8M0 {
            val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)
            val masked = engine.bitwiseAnd(bits.toLong(), engine.getMask(8))
            return CE8M0(masked.toUByte())
        }
    }
}

/** Convenience: equivalent to CE8M0.fromBits(x).toFloat(). */
fun ce8m0ToFp32(x: UByte): Float = CE8M0.fromBits(x).toFloat()

/** Convenience: equivalent to CE8M0.fromBits(x).toFloatHalf(). */
fun ce8m0ToFp32Half(x: UByte): Float = CE8M0.fromBits(x).toFloatHalf()

package ai.solace.klang.fp

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
        val xi = bits.toInt() and 0xFF
        val raw: Int = if (xi == 0) {
            // 2^(-127) expressed as a denormal: sign=0, exp=0, mantissa=0x400000
            0x00400000
        } else {
            xi shl 23
        }
        return Float.fromBits(raw)
    }

    /**
     * Decode to float32 divided by 2 (kvalues_mxfp4 convention where kvalues = 2 * E2M1_float).
     *
     * For x < 2 we emit precomputed denormal patterns (0x00200000 = 2^(-128)).
     * For x >= 2 we use a normalized exponent adjustment: 0.5 * 2^(x-127) = 2^(x-128).
     */
    fun toFloatHalf(): Float {
        val xi = bits.toInt() and 0xFF
        val raw: Int = if (xi < 2) {
            // 0x00200000 = 2^(-128), 0x00400000 = 2^(-127)
            0x00200000 shl xi
        } else {
            (xi - 1) shl 23
        }
        return Float.fromBits(raw)
    }

    override fun toString(): String = "CE8M0(0x${bits.toInt().and(0xFF).toString(16).padStart(2, '0')}, ${toFloat()})"

    override fun equals(other: Any?): Boolean = other is CE8M0 && other.bits == bits
    override fun hashCode(): Int = bits.toInt()

    companion object {
        /** Construct from raw 8-bit storage. */
        fun fromBits(bits: UByte): CE8M0 = CE8M0(bits)

        /** Construct from raw 8-bit storage given as Int (low 8 bits used). */
        fun fromBits(bits: Int): CE8M0 = CE8M0((bits and 0xFF).toUByte())
    }
}

/** Convenience: equivalent to CE8M0.fromBits(x).toFloat(). */
fun ce8m0ToFp32(x: UByte): Float = CE8M0.fromBits(x).toFloat()

/** Convenience: equivalent to CE8M0.fromBits(x).toFloatHalf(). */
fun ce8m0ToFp32Half(x: UByte): Float = CE8M0.fromBits(x).toFloatHalf()

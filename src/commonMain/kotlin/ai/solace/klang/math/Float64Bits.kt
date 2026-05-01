/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Pure IEEE-754 binary64 bit kernels for the basic-math surface
 * (sqrt, floor, ceil, trunc, round, frexp, ldexp, modf).
 *
 * No `kotlin.math.*` calls; no native arithmetic shortcuts. Every result is
 * derived from the input bit pattern via bitfield extraction and integer
 * arithmetic on the components, then re-packed.
 *
 * Format: 1 sign + 11 exponent (bias 1023) + 52 mantissa, with implicit
 * leading 1 for normals.
 */

package ai.solace.klang.math

object Float64Bits {

    private const val SIGN_MASK   = Long.MIN_VALUE                  // 0x8000_0000_0000_0000
    private const val EXP_MASK    = 0x7FF0_0000_0000_0000L
    private const val MANT_MASK   = 0x000F_FFFF_FFFF_FFFFL          // 52 bits
    private const val IMPLICIT_BIT = 0x0010_0000_0000_0000L         // bit 52
    private const val BIAS = 1023
    private const val MANT_BITS = 52
    private const val EXP_MAX = 0x7FF                               // 2047

    private const val POS_ZERO = 0x0000_0000_0000_0000L
    private const val NEG_ZERO = SIGN_MASK
    private const val POS_ONE  = 0x3FF0_0000_0000_0000L             // 1.0
    private const val NEG_ONE  = SIGN_MASK or POS_ONE               // -1.0
    private const val QNAN_BITS = 0x7FF8_0000_0000_0000L

    // ---- low-level field accessors (bit-only) ----

    private fun rawExp(bits: Long): Int = ((bits ushr MANT_BITS) and 0x7FFL).toInt()
    private fun rawMant(bits: Long): Long = bits and MANT_MASK
    private fun signBit(bits: Long): Long = bits and SIGN_MASK

    private fun isNaN(bits: Long): Boolean =
        rawExp(bits) == EXP_MAX && rawMant(bits) != 0L

    private fun isInf(bits: Long): Boolean =
        rawExp(bits) == EXP_MAX && rawMant(bits) == 0L

    private fun isZero(bits: Long): Boolean =
        (bits and 0x7FFF_FFFF_FFFF_FFFFL) == 0L

    // =====================================================================
    // Rounding family — pure bit kernels.
    //
    // Strategy for x finite, non-zero, non-NaN, non-Inf:
    //   Let e = unbiased exponent of |x| = rawExp - BIAS.
    //   The mantissa carries 52 fraction bits. The integer/fraction split
    //   inside the significand sits at fractional bit position
    //       fracBits = 52 - e        (only valid when 0 <= e < 52)
    //   - If e < 0           : |x| < 1   → result is 0 or ±1 per mode.
    //   - If e >= 52         : already integral (no fractional bits) → x as-is.
    //   - Otherwise          : keep top (52 - fracBits) mantissa bits, drop
    //                          fracBits low bits, then optionally add 1 ULP
    //                          at the integer position per rounding mode.
    // =====================================================================

    /** Round toward -∞. */
    fun floorBits(bits: Long): Long {
        if (isNaN(bits)) return QNAN_BITS
        if (isInf(bits) || isZero(bits)) return bits
        val e = rawExp(bits) - BIAS
        if (e >= MANT_BITS) return bits                 // already integral
        if (e < 0) {
            // |x| < 1 : negatives → -1, positives → +0
            return if (signBit(bits) != 0L) NEG_ONE else POS_ZERO
        }
        val fracBits = MANT_BITS - e                    // 1..52
        val mask = (1L shl fracBits) - 1L               // low fracBits set
        val truncated = bits and mask.inv()
        val droppedNonZero = (bits and mask) != 0L
        return if (signBit(bits) != 0L && droppedNonZero) {
            // negative with nonzero fractional part → step toward -∞
            // Add 1 at the integer position: value of |x| grows by 1 ULP at fracBits.
            addUlpAtFracBits(truncated, fracBits)
        } else {
            truncated
        }
    }

    /** Round toward +∞. */
    fun ceilBits(bits: Long): Long {
        if (isNaN(bits)) return QNAN_BITS
        if (isInf(bits) || isZero(bits)) return bits
        val e = rawExp(bits) - BIAS
        if (e >= MANT_BITS) return bits
        if (e < 0) {
            // |x| < 1 : positives → +1, negatives → -0
            return if (signBit(bits) != 0L) NEG_ZERO else POS_ONE
        }
        val fracBits = MANT_BITS - e
        val mask = (1L shl fracBits) - 1L
        val truncated = bits and mask.inv()
        val droppedNonZero = (bits and mask) != 0L
        return if (signBit(bits) == 0L && droppedNonZero) {
            addUlpAtFracBits(truncated, fracBits)
        } else {
            truncated
        }
    }

    /** Round toward zero. */
    fun truncBits(bits: Long): Long {
        if (isNaN(bits)) return QNAN_BITS
        if (isInf(bits) || isZero(bits)) return bits
        val e = rawExp(bits) - BIAS
        if (e >= MANT_BITS) return bits
        if (e < 0) {
            // |x| < 1 → ±0 with sign of input
            return signBit(bits) // sign | 0 mantissa | 0 exp = ±0
        }
        val fracBits = MANT_BITS - e
        val mask = (1L shl fracBits) - 1L
        return bits and mask.inv()
    }

    /**
     * Round half away from zero (C99 `round`).
     *
     * For |x| at or above 2^52, no fractional information remains so x is
     * already integral. Otherwise we look at the bit immediately below the
     * integer position (the "guard" bit) — if it is 1, we step the magnitude
     * up by 1 ULP at the integer position.
     */
    fun roundBits(bits: Long): Long {
        if (isNaN(bits)) return QNAN_BITS
        if (isInf(bits) || isZero(bits)) return bits
        val e = rawExp(bits) - BIAS
        if (e >= MANT_BITS) return bits
        if (e < -1) {
            // |x| < 0.5 → ±0
            return signBit(bits)
        }
        if (e == -1) {
            // 0.5 <= |x| < 1.0 → round half away from zero → ±1
            return if (signBit(bits) != 0L) NEG_ONE else POS_ONE
        }
        val fracBits = MANT_BITS - e                    // 1..52
        val mask = (1L shl fracBits) - 1L
        val guardBit = 1L shl (fracBits - 1)
        val truncated = bits and mask.inv()
        val halfOrMore = (bits and guardBit) != 0L
        return if (halfOrMore) addUlpAtFracBits(truncated, fracBits) else truncated
    }

    /**
     * Round half to even (banker's rounding, IEEE-754 default `roundeven`).
     *
     * Same as roundBits but ties (frac bits == 0b1000…0) round to even.
     */
    fun roundEvenBits(bits: Long): Long {
        if (isNaN(bits)) return QNAN_BITS
        if (isInf(bits) || isZero(bits)) return bits
        val e = rawExp(bits) - BIAS
        if (e >= MANT_BITS) return bits
        if (e < -1) return signBit(bits)
        if (e == -1) {
            // 0.5 <= |x| < 1 ; tie at exactly 0.5 rounds to 0 (even); >0.5 → ±1.
            val mantNonZero = rawMant(bits) != 0L
            return if (mantNonZero) {
                if (signBit(bits) != 0L) NEG_ONE else POS_ONE
            } else {
                signBit(bits) // ±0
            }
        }
        val fracBits = MANT_BITS - e
        val mask = (1L shl fracBits) - 1L
        val guardBit = 1L shl (fracBits - 1)
        val belowGuardMask = guardBit - 1L
        val truncated = bits and mask.inv()
        val guardSet = (bits and guardBit) != 0L
        val belowGuardNonZero = (bits and belowGuardMask) != 0L
        // The integer-position LSB is the lowest bit of `truncated` at fracBits.
        val intLsbSet = (bits and (1L shl fracBits)) != 0L
        val roundUp = guardSet && (belowGuardNonZero || intLsbSet)
        return if (roundUp) addUlpAtFracBits(truncated, fracBits) else truncated
    }

    /**
     * Add 1 ULP at the integer position for a value with fractional bits already
     * cleared. If the mantissa carries, propagate into the exponent (handles the
     * 0.999… → 1.0 boundary by exact bit arithmetic).
     */
    private fun addUlpAtFracBits(truncatedBits: Long, fracBits: Int): Long {
        // Strip sign for unsigned arithmetic on magnitude.
        val sign = truncatedBits and SIGN_MASK
        val magnitude = truncatedBits and SIGN_MASK.inv()
        val step = 1L shl fracBits
        val newMag = magnitude + step
        // newMag may cross into the exponent field — that's exactly what we want.
        // If the result overflows into the inf encoding, we still produce a valid
        // IEEE-754 result (±Inf), matching the behavior of native rounding when
        // the input is so close to overflow that the rounded value is no longer
        // representable. In practice this only happens for |x| ≈ DBL_MAX with
        // fraction bits set, which floor/ceil/round can never produce since
        // DBL_MAX is integral (e=1023 >= 52).
        return sign or newMag
    }

    // =====================================================================
    // frexp / ldexp / modf — also pure bit operations.
    // =====================================================================

    /**
     * Decompose x into (mantissa, exponent) with x = mantissa * 2^exponent
     * and 0.5 <= |mantissa| < 1.0 for finite non-zero x.
     *
     * Returns the mantissa in the lower 64 bits of the returned Long pair via
     * a packed result: `mantissaBits` and `exponent`. We use a Pair<Long,Int>
     * so the caller can unpack.
     */
    fun frexpBits(bits: Long): Pair<Long, Int> {
        if (isZero(bits) || isNaN(bits) || isInf(bits)) {
            // Per C: frexp(±0) = (±0, 0); frexp(±Inf) = (±Inf, 0); frexp(NaN) = (NaN, 0)
            val outBits = if (isNaN(bits)) QNAN_BITS or signBit(bits) else bits
            return outBits to 0
        }
        val sign = signBit(bits)
        val rawE = rawExp(bits)
        val mant = rawMant(bits)
        return if (rawE == 0) {
            // Subnormal: shift mantissa left until implicit bit appears.
            var m = mant
            var shift = 0
            while ((m and IMPLICIT_BIT) == 0L) {
                m = m shl 1
                shift++
            }
            val normalizedMant = m and MANT_MASK
            val unbiasedOriginal = 1 - BIAS - shift     // exponent of original subnormal
            val outE = unbiasedOriginal + 1             // mantissa now in [0.5,1)
            // Place mantissa with biased exp = BIAS - 1 (unbiased -1).
            val outBits = sign or ((BIAS - 1).toLong() shl MANT_BITS) or normalizedMant
            outBits to outE
        } else {
            val outE = rawE - BIAS + 1
            val outBits = sign or ((BIAS - 1).toLong() shl MANT_BITS) or mant
            outBits to outE
        }
    }

    /**
     * x * 2^exp via direct exponent-field manipulation (no native pow, no
     * native multiply). Handles overflow → ±Inf, underflow → ±0, and normal
     * ↔ subnormal transitions by limb-wise mantissa shifts.
     */
    fun ldexpBits(bits: Long, exp: Int): Long {
        if (isZero(bits) || isNaN(bits) || isInf(bits) || exp == 0) return bits
        val sign = signBit(bits)
        var rawE = rawExp(bits)
        var mant = rawMant(bits)

        // Extract a normalized (mant, e) pair where mant has the implicit bit at MANT_BITS
        // and e is the unbiased exponent of that leading 1.
        var unbiasedE: Int
        var significand: Long
        if (rawE == 0) {
            // Subnormal: normalize.
            var m = mant
            var shift = 0
            while ((m and IMPLICIT_BIT) == 0L) {
                m = m shl 1
                shift++
            }
            significand = m
            unbiasedE = 1 - BIAS - shift
        } else {
            significand = mant or IMPLICIT_BIT
            unbiasedE = rawE - BIAS
        }

        // Apply the scale.
        var newE = unbiasedE + exp

        // Overflow → ±Inf.
        if (newE > BIAS) {
            return sign or EXP_MASK
        }

        // Subnormal range: 1 - BIAS - shift = newE  =>  shift = 1 - BIAS - newE
        // If shift > MANT_BITS the value rounds to ±0.
        if (newE < 1 - BIAS) {
            val shift = (1 - BIAS) - newE
            if (shift > MANT_BITS) {
                return sign // ±0
            }
            // Build a subnormal: shift the significand right by `shift` bits,
            // discarding low bits (round-toward-zero — ldexp by integer power
            // of two should be exact when not crossing the subnormal boundary
            // outward, but inward-shifts may lose bits).
            val newMant = significand ushr shift
            return sign or newMant
        }

        // Normal range.
        val newRawExp = (newE + BIAS).toLong()
        val newMant = significand and MANT_MASK
        return sign or (newRawExp shl MANT_BITS) or newMant
    }

    /**
     * Decompose x into (integer part, fractional part) — both with the sign of x.
     */
    fun modfBits(bits: Long): Pair<Long, Long> {
        if (isNaN(bits)) return QNAN_BITS to QNAN_BITS
        if (isInf(bits)) return bits to (signBit(bits)) // ±Inf, ±0
        if (isZero(bits)) return bits to bits

        val intBits = truncBits(bits)
        // fractional = x - intBits, computed at IEEE precision. We can derive
        // it bit-exactly by subtraction; but to stay strictly within bit-twiddle
        // territory we compute |x| - |int| in the significand domain when both
        // are in normal range. For simplicity and correctness, use the
        // canonical identity: fractional carries the bits below the integer
        // boundary, scaled back into a normalized FP representation.
        val e = rawExp(bits) - BIAS
        if (e >= MANT_BITS) return intBits to signBit(bits) // already integral, fractional = ±0
        if (e < 0) return signBit(bits) to bits             // |x| < 1, integer = ±0
        val fracBits = MANT_BITS - e
        val mask = (1L shl fracBits) - 1L
        val fracMantBits = bits and mask
        if (fracMantBits == 0L) {
            return intBits to signBit(bits) // exact integer, fractional = ±0
        }
        // Renormalize the fractional mantissa back to [0.5, 1.0) * 2^k form,
        // with k the unbiased exponent of the leading 1 bit of fracMantBits.
        // Find the leading bit position.
        var m = fracMantBits
        var leadPos = 0       // 0..(fracBits-1); position of highest set bit
        // count from top down
        var probe = fracBits - 1
        while (probe >= 0 && (m and (1L shl probe)) == 0L) probe--
        leadPos = probe       // guaranteed >= 0 since fracMantBits != 0
        // The leading 1 bit currently sits at bit `leadPos` of m. We want it
        // at bit MANT_BITS (implicit-bit position) for normalization.
        val shiftLeft = MANT_BITS - leadPos
        val normalized = (m shl shiftLeft) and MANT_MASK
        // The unbiased exponent for the fractional part: leadPos - fracBits
        // (e.g., leadPos = fracBits-1 means leading bit is just below the
        // integer position, so |frac| in [0.5, 1.0), exponent = -1).
        val fracExp = leadPos - fracBits
        // Build either a normal (rawExp = fracExp + BIAS) or a subnormal.
        return if (fracExp + BIAS >= 1) {
            val rawExpField = (fracExp + BIAS).toLong()
            val fracOut = signBit(bits) or (rawExpField shl MANT_BITS) or normalized
            intBits to fracOut
        } else {
            // Subnormal: shift right so leading bit lands appropriately.
            val sub = fracMantBits // already in proper alignment for subnormal mantissa
            val fracOut = signBit(bits) or sub
            intBits to fracOut
        }
    }

    // =====================================================================
    // sqrt — Newton-Raphson on bits, no kotlin.math.sqrt.
    //
    // We use the same shift-and-add integer mantissa algorithm as
    // Float32Math.sqrtBits, scaled up to 53 mantissa bits (52 + implicit).
    // =====================================================================

    private const val CANONICAL_NAN_64 = QNAN_BITS

    fun sqrtBits(aBits: Long): Long {
        val sign = aBits and SIGN_MASK
        val exp = ((aBits ushr MANT_BITS) and 0x7FFL).toInt()
        val frac = aBits and MANT_MASK

        // NaN / Inf ladder.
        if (exp == EXP_MAX) {
            return if (frac != 0L) {
                CANONICAL_NAN_64
            } else {
                if (sign != 0L) CANONICAL_NAN_64 else aBits // -Inf → NaN ; +Inf → +Inf
            }
        }
        // ±0 preserves sign.
        if ((aBits and 0x7FFF_FFFF_FFFF_FFFFL) == 0L) return aBits
        // Negative finite → NaN.
        if (sign != 0L) return CANONICAL_NAN_64

        // Extract unbiased exponent + significand.
        var xExp = exp - BIAS
        var xMant: Long
        val one = IMPLICIT_BIT  // 1 << 52 represents "1.0" in our fixed-point view

        if (exp == 0) {
            // Subnormal: normalize mantissa until the implicit bit appears.
            xExp += 1
            var m = frac
            while (m < one) {
                m = m shl 1
                xExp -= 1
            }
            xMant = m
        } else {
            xMant = frac or one
        }

        // Make exponent even.
        if ((xExp and 1) != 0) {
            xExp -= 1
            xMant = xMant shl 1
        }

        // Shift-and-add square root for mantissa as 1.xxx fixed point.
        var y = one
        var r = xMant - one
        var current = one ushr 1
        while (current != 0L) {
            r = r shl 1
            val tmp = (y shl 1) + current
            if (r >= tmp) {
                r -= tmp
                y += current
            }
            current = current ushr 1
        }
        // Extra iteration for rounding decision.
        val lsb = (y and 1L) != 0L
        var rb = false
        r = r shl 2
        val tmp2 = (y shl 2) + 1L
        if (r >= tmp2) {
            r -= tmp2
            rb = true
        }
        val outExp = ((xExp shr 1) + BIAS).toLong() and 0x7FFL
        var outMant = (y - one) and MANT_MASK
        var outBits = (outExp shl MANT_BITS) or outMant
        if (rb && (lsb || r != 0L)) {
            outBits += 1
        }
        return outBits
    }
}

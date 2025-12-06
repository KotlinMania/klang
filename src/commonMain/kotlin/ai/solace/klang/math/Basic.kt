/*
 * Copyright (c) 2024 KLang Contributors
 * 
 * Portions derived from FreeBSD libm:
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 * Copyright (c) 2003-2004 David Schultz <das@FreeBSD.ORG>
 * 
 * Permission to use, copy, modify, and distribute this software is freely
 * granted, provided that this notice is preserved.
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

/**
 * Basic mathematical operations using IEEE-754 bit manipulation.
 * 
 * These functions operate directly on the binary representation of floating-point
 * values, providing deterministic cross-platform behavior without arithmetic operations.
 * 
 * ## Platform Support
 * - JavaScript (ES2015+)
 * - Native (macOS ARM64/x64, Linux x64/ARM64, Windows x64)
 * 
 * ## References
 * - FreeBSD libm: `lib/msun/src/s_fabs.c`, `s_copysign.c`
 * - IEEE-754 binary64 format: 1 sign + 11 exponent + 52 mantissa bits
 * 
 * @since 0.2.0
 */
object KMath {
    
    /**
     * Absolute value via bit manipulation.
     * 
     * Clears the sign bit (bit 63) of the IEEE-754 representation without
     * affecting the magnitude. This provides exact results for all values
     * including special cases.
     * 
     * ## Algorithm
     * ```
     * Clear bit 63: bits & 0x7FFF_FFFF_FFFF_FFFF
     * ```
     * 
     * ## Special Cases
     * - `fabs(±0)` returns `+0`
     * - `fabs(±∞)` returns `+∞`
     * - `fabs(NaN)` returns `NaN` (preserves payload and quiet/signaling bit)
     * - `fabs(subnormal)` returns the positive subnormal
     * 
     * ## Cross-Platform Guarantee
     * Identical bit-exact results on JavaScript and Native platforms.
     * 
     * ## Reference
     * FreeBSD: `lib/msun/src/s_fabs.c`
     * ```c
     * u_int32_t high;
     * GET_HIGH_WORD(high, x);
     * SET_HIGH_WORD(x, high & 0x7fffffff);
     * return x;
     * ```
     * 
     * @param x Input value
     * @return Absolute value of x with positive sign
     * @see copysign For copying sign bits between values
     */
    fun fabs(x: Double): Double {
        val bits = x.toRawBits()
        val absBits = bits and 0x7FFF_FFFF_FFFF_FFFFL
        return Double.fromBits(absBits)
    }
    
    /**
     * Copy sign bit from one value to another.
     * 
     * Returns a value with the magnitude of [magnitude] and the sign of [sign].
     * This is a pure bit manipulation operation with no arithmetic.
     * 
     * ## Algorithm
     * ```
     * 1. Extract magnitude bits (clear sign): magnitude & 0x7FFF_FFFF_FFFF_FFFF
     * 2. Extract sign bit: sign & 0x8000_0000_0000_0000
     * 3. Combine: magnitudeBits | signBit
     * ```
     * 
     * ## Special Cases
     * - `copysign(x, ±0)` returns `±x`
     * - `copysign(NaN, y)` returns `±NaN` with sign of y
     * - `copysign(±∞, y)` returns `±∞` with sign of y
     * 
     * ## Use Cases
     * - Sign manipulation in numerical algorithms
     * - Implementing signed zero semantics
     * - Building other math functions (e.g., `signbit`)
     * 
     * ## Reference
     * FreeBSD: `lib/msun/src/s_copysign.c`
     * ```c
     * u_int32_t hx, hy;
     * GET_HIGH_WORD(hx, x);
     * GET_HIGH_WORD(hy, y);
     * SET_HIGH_WORD(x, (hx & 0x7fffffff) | (hy & 0x80000000));
     * return x;
     * ```
     * 
     * @param magnitude Value providing the magnitude
     * @param sign Value providing the sign
     * @return Value with magnitude of [magnitude] and sign of [sign]
     * @see signbit For extracting just the sign bit
     * @see fabs For clearing the sign bit
     */
    fun copysign(magnitude: Double, sign: Double): Double {
        val magnitudeBits = magnitude.toRawBits() and 0x7FFF_FFFF_FFFF_FFFFL
        val signBit = sign.toRawBits() and Long.MIN_VALUE
        return Double.fromBits(magnitudeBits or signBit)
    }
    
    /**
     * Extract the sign bit of a value.
     * 
     * Returns `true` if the sign bit (bit 63) is set, indicating a negative value.
     * This works correctly for all values including zeros, infinities, and NaNs.
     * 
     * ## Algorithm
     * ```
     * Check bit 63: (bits ushr 63) != 0
     * ```
     * 
     * ## Special Cases
     * - `signbit(+0)` returns `false`
     * - `signbit(-0)` returns `true` (distinguishes signed zeros)
     * - `signbit(+∞)` returns `false`
     * - `signbit(-∞)` returns `true`
     * - `signbit(NaN)` returns the sign of the NaN payload
     * 
     * ## IEEE-754 Note
     * Unlike `x < 0.0`, this function distinguishes between `+0` and `-0`.
     * 
     * ## Reference
     * FreeBSD: `lib/msun/src/s_signbit.c`
     * ```c
     * union IEEEd2bits u;
     * u.d = d;
     * return (u.bits.sign);
     * ```
     * 
     * @param x Input value
     * @return `true` if sign bit is set (negative), `false` otherwise
     * @see copysign For manipulating sign bits
     */
    fun signbit(x: Double): Boolean {
        return (x.toRawBits() ushr 63) != 0L
    }
}

/**
 * Extension function for absolute value.
 * 
 * Convenience wrapper around [KMath.fabs].
 * 
 * @receiver Input value
 * @return Absolute value
 * @see KMath.fabs
 */
fun Double.abs(): Double = KMath.fabs(this)

/**
 * Extension function for copying sign.
 * 
 * Convenience wrapper around [KMath.copysign].
 * 
 * @receiver Value providing the magnitude
 * @param sign Value providing the sign
 * @return Value with this magnitude and [sign]'s sign
 * @see KMath.copysign
 */
fun Double.withSign(sign: Double): Double = KMath.copysign(this, sign)

/**
 * Extension property for sign bit.
 * 
 * Convenience wrapper around [KMath.signbit].
 * 
 * @receiver Input value
 * @return `true` if negative (sign bit set)
 * @see KMath.signbit
 */
val Double.hasNegativeSign: Boolean
    get() = KMath.signbit(this)

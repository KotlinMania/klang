/*
 * Copyright (c) 2024 KLang Contributors
 * 
 * Portions derived from FreeBSD libm:
 * Copyright (c) 2003-2004 David Schultz <das@FreeBSD.ORG>
 * 
 * Permission to use, copy, modify, and distribute this software is freely
 * granted, provided that this notice is preserved.
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

/**
 * IEEE-754 floating-point classification constants and functions.
 * 
 * These functions classify floating-point values into standard categories
 * defined by IEEE-754: zero, subnormal, normal, infinite, and NaN.
 * 
 * ## Platform Support
 * - JavaScript (ES2015+)
 * - Native (macOS ARM64/x64, Linux x64/ARM64, Windows x64)
 * 
 * ## References
 * - IEEE-754-2008 Section 5.7: Classification
 * - FreeBSD libm: `lib/msun/src/s_fmax.c`, `s_isnan.c`
 * 
 * @since 0.2.0
 */
object Classification {
    
    /** Exponent mask for binary64: bits 62-52 */
    private const val EXP_MASK = 0x7FF0_0000_0000_0000L
    
    /** Mantissa mask for binary64: bits 51-0 */
    private const val FRAC_MASK = 0x000F_FFFF_FFFF_FFFFL
    
    /** Maximum exponent value (all bits set): 0x7FF */
    private const val EXP_MAX = 0x7FFL
    
    /**
     * Check if value is Not-a-Number (NaN).
     * 
     * A value is NaN if the exponent is all 1s (0x7FF) and the mantissa
     * is non-zero. This includes both quiet NaNs and signaling NaNs.
     * 
     * ## Algorithm
     * ```
     * exp == 0x7FF && mantissa != 0
     * ```
     * 
     * ## Special Cases
     * - `isnan(qNaN)` returns `true` (quiet NaN)
     * - `isnan(sNaN)` returns `true` (signaling NaN)
     * - `isnan(±∞)` returns `false`
     * - `isnan(±0)` returns `false`
     * 
     * ## IEEE-754 Note
     * NaN values are unordered: `NaN != NaN` is true.
     * Use this function instead of `x != x` for clarity.
     * 
     * @param x Input value
     * @return `true` if x is NaN, `false` otherwise
     * @see isinf For checking infinity
     * @see isfinite For checking finite values
     */
    fun isnan(x: Double): Boolean {
        val bits = x.toRawBits()
        val exp = (bits and EXP_MASK) ushr 52
        val frac = bits and FRAC_MASK
        return exp == EXP_MAX && frac != 0L
    }
    
    /**
     * Check if value is positive or negative infinity.
     * 
     * A value is infinite if the exponent is all 1s (0x7FF) and the
     * mantissa is zero.
     * 
     * ## Algorithm
     * ```
     * exp == 0x7FF && mantissa == 0
     * ```
     * 
     * ## Special Cases
     * - `isinf(+∞)` returns `true`
     * - `isinf(-∞)` returns `true`
     * - `isinf(NaN)` returns `false`
     * - `isinf(0)` returns `false`
     * 
     * @param x Input value
     * @return `true` if x is ±infinity, `false` otherwise
     * @see isnan For checking NaN
     * @see isfinite For checking finite values
     */
    fun isinf(x: Double): Boolean {
        val bits = x.toRawBits()
        val exp = (bits and EXP_MASK) ushr 52
        val frac = bits and FRAC_MASK
        return exp == EXP_MAX && frac == 0L
    }
    
    /**
     * Check if value is finite (not infinity or NaN).
     * 
     * A value is finite if the exponent is not all 1s. This includes
     * zero, subnormals, and normal numbers.
     * 
     * ## Algorithm
     * ```
     * exp != 0x7FF
     * ```
     * 
     * ## Special Cases
     * - `isfinite(±0)` returns `true`
     * - `isfinite(subnormal)` returns `true`
     * - `isfinite(normal)` returns `true`
     * - `isfinite(±∞)` returns `false`
     * - `isfinite(NaN)` returns `false`
     * 
     * @param x Input value
     * @return `true` if x is finite, `false` otherwise
     * @see isinf For checking infinity
     * @see isnan For checking NaN
     */
    fun isfinite(x: Double): Boolean {
        val bits = x.toRawBits()
        val exp = (bits and EXP_MASK) ushr 52
        return exp != EXP_MAX
    }
    
    /**
     * Check if value is zero (positive or negative).
     * 
     * A value is zero if the exponent and mantissa are both zero.
     * The sign bit may be set (negative zero).
     * 
     * ## Algorithm
     * ```
     * (bits & 0x7FFFFFFFFFFFFFFF) == 0
     * ```
     * 
     * ## Special Cases
     * - `iszero(+0)` returns `true`
     * - `iszero(-0)` returns `true`
     * - `iszero(subnormal)` returns `false`
     * 
     * @param x Input value
     * @return `true` if x is ±0, `false` otherwise
     */
    fun iszero(x: Double): Boolean {
        return (x.toRawBits() and 0x7FFF_FFFF_FFFF_FFFFL) == 0L
    }
    
    /**
     * Check if value is subnormal (denormalized).
     * 
     * A value is subnormal if the exponent is zero and the mantissa
     * is non-zero. Subnormals have reduced precision near zero.
     * 
     * ## Algorithm
     * ```
     * exp == 0 && mantissa != 0
     * ```
     * 
     * ## Special Cases
     * - `issubnormal(smallest_positive)` returns `true`
     * - `issubnormal(0)` returns `false`
     * - `issubnormal(normal)` returns `false`
     * 
     * ## IEEE-754 Note
     * Subnormals (also called denormals) allow gradual underflow,
     * maintaining relative precision near zero.
     * 
     * @param x Input value
     * @return `true` if x is subnormal, `false` otherwise
     */
    fun issubnormal(x: Double): Boolean {
        val bits = x.toRawBits()
        val exp = (bits and EXP_MASK) ushr 52
        val frac = bits and FRAC_MASK
        return exp == 0L && frac != 0L
    }
    
    /**
     * Check if value is normal (not zero, subnormal, infinite, or NaN).
     * 
     * A value is normal if the exponent is between 1 and 0x7FE (inclusive).
     * 
     * ## Algorithm
     * ```
     * 0 < exp < 0x7FF
     * ```
     * 
     * ## Special Cases
     * - `isnormal(1.0)` returns `true`
     * - `isnormal(0)` returns `false`
     * - `isnormal(subnormal)` returns `false`
     * - `isnormal(±∞)` returns `false`
     * - `isnormal(NaN)` returns `false`
     * 
     * @param x Input value
     * @return `true` if x is normal, `false` otherwise
     */
    fun isnormal(x: Double): Boolean {
        val bits = x.toRawBits()
        val exp = (bits and EXP_MASK) ushr 52
        return exp != 0L && exp != EXP_MAX
    }
}

/**
 * Extension function for NaN check.
 * 
 * @receiver Input value
 * @return `true` if NaN
 * @see Classification.isnan
 */
fun Double.isNaN(): Boolean = Classification.isnan(this)

/**
 * Extension function for infinity check.
 * 
 * @receiver Input value
 * @return `true` if ±infinity
 * @see Classification.isinf
 */
fun Double.isInfinite(): Boolean = Classification.isinf(this)

/**
 * Extension function for finite check.
 * 
 * @receiver Input value
 * @return `true` if finite (not NaN or infinity)
 * @see Classification.isfinite
 */
fun Double.isFinite(): Boolean = Classification.isfinite(this)

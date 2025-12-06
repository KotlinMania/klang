/*
 * Copyright (c) 2024 KLang Contributors
 * 
 * Portions derived from FreeBSD libm:
 * Copyright (c) 2004 David Schultz <das@FreeBSD.ORG>
 * 
 * Permission to use, copy, modify, and distribute this software is freely
 * granted, provided that this notice is preserved.
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

/**
 * IEEE-754 compliant comparison functions.
 * 
 * These functions implement min/max operations with special handling for
 * NaN values and signed zeros according to IEEE-754 semantics.
 * 
 * ## Platform Support
 * - JavaScript (ES2015+)
 * - Native (macOS ARM64/x64, Linux x64/ARM64, Windows x64)
 * 
 * ## References
 * - IEEE-754-2008 Section 5.3.1: minNum, maxNum operations
 * - FreeBSD libm: `lib/msun/src/s_fmax.c`, `s_fmin.c`
 * 
 * @since 0.2.0
 */
object Comparison {
    
    /** Exponent mask for binary64: bits 62-52 */
    private const val EXP_MASK = 0x7FF0_0000_0000_0000L
    
    /** Sign bit mask for binary64: bit 63 */
    private val SIGN_MASK = Long.MIN_VALUE
    
    /** Mantissa mask for binary64: bits 51-0 */
    private const val FRAC_MASK = 0x000F_FFFF_FFFF_FFFFL
    
    /**
     * Maximum of two values with IEEE-754 NaN and zero handling.
     * 
     * Returns the larger of [x] and [y]. If either argument is NaN,
     * returns the non-NaN argument (or NaN if both are NaN).
     * Signed zeros are handled such that `fmax(+0, -0) == +0`.
     * 
     * ## Algorithm
     * ```
     * 1. If x is NaN, return y
     * 2. If y is NaN, return x
     * 3. If signs differ, return the positive one
     * 4. Otherwise, return x > y ? x : y
     * ```
     * 
     * ## Special Cases
     * - `fmax(x, NaN)` returns `x`
     * - `fmax(NaN, y)` returns `y`
     * - `fmax(NaN, NaN)` returns `NaN`
     * - `fmax(+0, -0)` returns `+0`
     * - `fmax(-0, +0)` returns `+0`
     * - `fmax(+∞, x)` returns `+∞`
     * - `fmax(x, +∞)` returns `+∞`
     * 
     * ## IEEE-754 Note
     * This implements the `maxNum` operation from IEEE-754-2008,
     * which prefers non-NaN values over NaN values.
     * 
     * ## Reference
     * FreeBSD: `lib/msun/src/s_fmax.c`
     * ```c
     * // Check for NaNs to avoid raising spurious exceptions
     * if (u[0].bits.exp == 2047 && (u[0].bits.manh | u[0].bits.manl) != 0)
     *     return (y);
     * if (u[1].bits.exp == 2047 && (u[1].bits.manh | u[1].bits.manl) != 0)
     *     return (x);
     * 
     * // Handle comparisons of signed zeroes
     * if (u[0].bits.sign != u[1].bits.sign)
     *     return (u[u[0].bits.sign].d);
     * 
     * return (x > y ? x : y);
     * ```
     * 
     * @param x First value
     * @param y Second value
     * @return Maximum of x and y
     * @see fmin For minimum
     * @see Classification.isnan For NaN detection
     */
    fun fmax(x: Double, y: Double): Double {
        val xBits = x.toRawBits()
        val yBits = y.toRawBits()
        
        val xExp = (xBits and EXP_MASK) ushr 52
        val yExp = (yBits and EXP_MASK) ushr 52
        
        val xFrac = xBits and FRAC_MASK
        val yFrac = yBits and FRAC_MASK
        
        // Check for NaNs - return the non-NaN value
        if (xExp == 0x7FFL && xFrac != 0L) return y
        if (yExp == 0x7FFL && yFrac != 0L) return x
        
        // Handle signed zeros: prefer positive zero
        val xSign = (xBits and SIGN_MASK) ushr 63
        val ySign = (yBits and SIGN_MASK) ushr 63
        
        if (xSign != ySign) {
            // Different signs: return the positive one
            return if (xSign == 0L) x else y
        }
        
        // Normal comparison
        return if (x > y) x else y
    }
    
    /**
     * Minimum of two values with IEEE-754 NaN and zero handling.
     * 
     * Returns the smaller of [x] and [y]. If either argument is NaN,
     * returns the non-NaN argument (or NaN if both are NaN).
     * Signed zeros are handled such that `fmin(+0, -0) == -0`.
     * 
     * ## Algorithm
     * ```
     * 1. If x is NaN, return y
     * 2. If y is NaN, return x
     * 3. If signs differ, return the negative one
     * 4. Otherwise, return x < y ? x : y
     * ```
     * 
     * ## Special Cases
     * - `fmin(x, NaN)` returns `x`
     * - `fmin(NaN, y)` returns `y`
     * - `fmin(NaN, NaN)` returns `NaN`
     * - `fmin(+0, -0)` returns `-0`
     * - `fmin(-0, +0)` returns `-0`
     * - `fmin(-∞, x)` returns `-∞`
     * - `fmin(x, -∞)` returns `-∞`
     * 
     * ## IEEE-754 Note
     * This implements the `minNum` operation from IEEE-754-2008,
     * which prefers non-NaN values over NaN values.
     * 
     * ## Reference
     * FreeBSD: `lib/msun/src/s_fmin.c`
     * ```c
     * // Check for NaNs to avoid raising spurious exceptions
     * if (u[0].bits.exp == 2047 && (u[0].bits.manh | u[0].bits.manl) != 0)
     *     return (y);
     * if (u[1].bits.exp == 2047 && (u[1].bits.manh | u[1].bits.manl) != 0)
     *     return (x);
     * 
     * // Handle comparisons of signed zeroes
     * if (u[0].bits.sign != u[1].bits.sign)
     *     return (u[u[1].bits.sign].d);
     * 
     * return (x < y ? x : y);
     * ```
     * 
     * @param x First value
     * @param y Second value
     * @return Minimum of x and y
     * @see fmax For maximum
     * @see Classification.isnan For NaN detection
     */
    fun fmin(x: Double, y: Double): Double {
        val xBits = x.toRawBits()
        val yBits = y.toRawBits()
        
        val xExp = (xBits and EXP_MASK) ushr 52
        val yExp = (yBits and EXP_MASK) ushr 52
        
        val xFrac = xBits and FRAC_MASK
        val yFrac = yBits and FRAC_MASK
        
        // Check for NaNs - return the non-NaN value
        if (xExp == 0x7FFL && xFrac != 0L) return y
        if (yExp == 0x7FFL && yFrac != 0L) return x
        
        // Handle signed zeros: prefer negative zero
        val xSign = (xBits and SIGN_MASK) ushr 63
        val ySign = (yBits and SIGN_MASK) ushr 63
        
        if (xSign != ySign) {
            // Different signs: return the negative one
            return if (ySign == 1L) y else x
        }
        
        // Normal comparison
        return if (x < y) x else y
    }
    
    /**
     * Positive difference: max(x - y, 0).
     * 
     * Returns `x - y` if `x > y`, otherwise returns `+0`.
     * This is equivalent to `fmax(x - y, 0.0)` but may be more efficient.
     * 
     * ## Algorithm
     * ```
     * if x > y then x - y else +0
     * ```
     * 
     * ## Special Cases
     * - `fdim(x, x)` returns `+0`
     * - `fdim(+∞, finite)` returns `+∞`
     * - `fdim(finite, +∞)` returns `+0`
     * - `fdim(NaN, y)` returns `NaN`
     * - `fdim(x, NaN)` returns `NaN`
     * 
     * ## Use Cases
     * - Clamping differences to non-negative values
     * - Computing distances that are always positive
     * 
     * @param x First value
     * @param y Second value
     * @return Positive difference (x - y) or 0
     * @see fmax For general maximum operation
     */
    fun fdim(x: Double, y: Double): Double {
        // Handle NaN cases
        if (x.isNaN() || y.isNaN()) {
            return Double.NaN
        }
        
        // Compute difference
        return if (x > y) x - y else 0.0
    }
}

/**
 * Extension function for maximum.
 * 
 * @receiver First value
 * @param other Second value
 * @return Maximum of this and other
 * @see Comparison.fmax
 */
fun Double.max(other: Double): Double = Comparison.fmax(this, other)

/**
 * Extension function for minimum.
 * 
 * @receiver First value
 * @param other Second value
 * @return Minimum of this and other
 * @see Comparison.fmin
 */
fun Double.min(other: Double): Double = Comparison.fmin(this, other)

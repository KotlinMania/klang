/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package io.github.kotlinmania.klang.math

/**
 * Basic floating-point math operations: sqrt, rounding modes, FP utilities.
 *
 * This object is now a thin Double-typed facade over [Float64Bits], the pure
 * IEEE-754 bit kernel. **No `kotlin.math.*` shortcuts and no native arithmetic
 * is used inside the kernels** — everything is bit-pattern manipulation. The
 * `Double` types here are only the unavoidable round-trip at the API boundary
 * (every value originates from / terminates at a heap slot that round-trips
 * through `Double.toRawBits` / `Double.fromBits`).
 *
 * Provides C-libm-style basic math for `Double`:
 *   - `sqrt(x)`        — IEEE-754 square root (shift-and-add bit kernel)
 *   - `floor(x)`       — round toward -∞
 *   - `ceil(x)`        — round toward +∞
 *   - `trunc(x)`       — round toward zero
 *   - `round(x)`       — round half away from zero (C99)
 *   - `roundEven(x)`   — round half to even (banker's rounding, IEEE-754 default)
 *   - `frexp(x)`       — split into (mantissa in [0.5, 1.0), exponent)
 *   - `ldexp(x, exp)`  — x * 2^exp
 *   - `modf(x)`        — split into (integer part, fractional part)
 *   - `logb(x)`        — unbiased binary exponent as a Double (C99)
 *   - `ilogb(x)`       — unbiased binary exponent as an Int (C99)
 *   - `rint(x)`        — round to nearest integer (current mode = round-half-to-even)
 *   - `nearbyint(x)`   — same as rint, but never raises the inexact exception
 *   - `scalbn(x, n)`   — x * 2^n with integer n (alias of [ldexp])
 *   - `scalbln(x, n)`  — x * 2^n with long n
 *
 * @since 0.3.0
 */
object BasicMath {

    fun sqrt(x: Double): Double = Double.fromBits(Float64Bits.sqrtBits(x.toRawBits()))

    fun floor(x: Double): Double = Double.fromBits(Float64Bits.floorBits(x.toRawBits()))

    fun ceil(x: Double): Double = Double.fromBits(Float64Bits.ceilBits(x.toRawBits()))

    fun trunc(x: Double): Double = Double.fromBits(Float64Bits.truncBits(x.toRawBits()))

    /** Round half away from zero (C99 `round`). */
    fun round(x: Double): Double = Double.fromBits(Float64Bits.roundBits(x.toRawBits()))

    /** Round half to even (banker's rounding, IEEE-754 default). */
    fun roundEven(x: Double): Double = Double.fromBits(Float64Bits.roundEvenBits(x.toRawBits()))

    fun frexp(x: Double): Pair<Double, Int> {
        val (mBits, e) = Float64Bits.frexpBits(x.toRawBits())
        return Double.fromBits(mBits) to e
    }

    fun ldexp(x: Double, exp: Int): Double =
        Double.fromBits(Float64Bits.ldexpBits(x.toRawBits(), exp))

    fun modf(x: Double): Pair<Double, Double> {
        val (iBits, fBits) = Float64Bits.modfBits(x.toRawBits())
        return Double.fromBits(iBits) to Double.fromBits(fBits)
    }

    /**
     * Unbiased binary exponent of `x` as a Double (C99 `logb`).
     *
     * Returns the value `e` such that `|x| = m * 2^e` with `1 <= m < 2`, treating
     * subnormals as though normalized (so `logb(0x1p-1073) == -1073.0`, not `-1022.0`).
     *
     * Special cases:
     *   - `logb(±0)`   = −∞
     *   - `logb(±Inf)` = +∞
     *   - `logb(NaN)`  = NaN
     */
    fun logb(x: Double): Double = Double.fromBits(Float64Bits.logbBits(x.toRawBits()))

    /**
     * Unbiased binary exponent of `x` as an Int (C99 `ilogb`).
     *
     * Special cases:
     *   - `ilogb(±0)`   = [Float64Bits.FP_ILOGB0]
     *   - `ilogb(±Inf)` = [Int.MAX_VALUE]
     *   - `ilogb(NaN)`  = [Float64Bits.FP_ILOGBNAN]
     */
    fun ilogb(x: Double): Int = Float64Bits.ilogbBits(x.toRawBits())

    /**
     * Round `x` to the nearest integer using the current rounding mode.
     *
     * Kotlin Multiplatform does not expose user-controllable IEEE-754 rounding
     * modes; the default is round-half-to-even, so `rint` is the same as
     * [roundEven] on every target.
     */
    fun rint(x: Double): Double = Double.fromBits(Float64Bits.roundEvenBits(x.toRawBits()))

    /**
     * Round `x` to the nearest integer without raising the inexact exception.
     *
     * Functionally identical to [rint] on Kotlin Multiplatform; the inexact
     * exception is not observable on any target.
     */
    fun nearbyint(x: Double): Double = Double.fromBits(Float64Bits.roundEvenBits(x.toRawBits()))

    /**
     * `x * 2^n` for integer `n`. C99 mandates this be functionally identical to
     * [ldexp] on implementations where the radix is 2 (every target this
     * library ships on).
     */
    fun scalbn(x: Double, n: Int): Double =
        Double.fromBits(Float64Bits.ldexpBits(x.toRawBits(), n))

    /**
     * `x * 2^n` for `Long` `n`. Saturating: out-of-range `n` is clamped to the
     * widest representable Int before being forwarded to [ldexp], which then
     * returns ±Inf or ±0 as appropriate.
     */
    fun scalbln(x: Double, n: Long): Double {
        val nInt = when {
            n > Int.MAX_VALUE.toLong() -> Int.MAX_VALUE
            n < Int.MIN_VALUE.toLong() -> Int.MIN_VALUE
            else -> n.toInt()
        }
        return Double.fromBits(Float64Bits.ldexpBits(x.toRawBits(), nInt))
    }
}

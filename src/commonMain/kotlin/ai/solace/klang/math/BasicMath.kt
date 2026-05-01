/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

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
}

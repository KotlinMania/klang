/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Cross-validates the new IEEE-754 binary64 bit kernels in [Float64Bits]
 * against the host runtime's `kotlin.math.*` operations across a broad
 * representative sample of inputs. The bit kernels are the only code path
 * the project's basic-math surface is allowed to use, so this test
 * documents that they are bit-exact equivalent on the host runtime.
 */

package ai.solace.klang.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Float64BitsTest {

    private val samples: DoubleArray = doubleArrayOf(
        0.0, -0.0, 1.0, -1.0, 0.5, -0.5, 0.25, -0.25,
        1.5, -1.5, 2.5, -2.5, 3.499999999, -3.499999999,
        3.5, -3.5, 4.5, -4.5, 1e-300, -1e-300,
        1e300, -1e300,
        Double.MIN_VALUE, -Double.MIN_VALUE,                  // smallest subnormal
        Double.fromBits(0x000F_FFFF_FFFF_FFFFL),              // largest subnormal
        Double.fromBits(0x0010_0000_0000_0000L),             // MIN_NORMAL
        -Double.fromBits(0x0010_0000_0000_0000L),
        Double.MAX_VALUE, -Double.MAX_VALUE,
        4503599627370496.5,                                    // 2^52 + 0.5 (no fractional bits left)
        9007199254740993.0,                                    // 2^53 + 1 (just integer)
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.NaN,
        12.345, -12.345, 100.5, -100.5,
        0.1, -0.1, 0.9999999999, -0.9999999999,
    )

    private fun bitsEq(a: Double, b: Double): Boolean {
        // NaN ↔ NaN: any NaN bit pattern is acceptable.
        if (a.isNaN() && b.isNaN()) return true
        return a.toRawBits() == b.toRawBits()
    }

    @Test
    fun floor_matches_kotlin_math_for_all_samples() {
        for (x in samples) {
            val expected = kotlin.math.floor(x)
            val actual = BasicMath.floor(x)
            assertTrue(
                bitsEq(actual, expected),
                "floor($x): expected=${expected.toRawBits().toString(16)} " +
                    "actual=${actual.toRawBits().toString(16)}"
            )
        }
    }

    @Test
    fun ceil_matches_kotlin_math_for_all_samples() {
        for (x in samples) {
            val expected = kotlin.math.ceil(x)
            val actual = BasicMath.ceil(x)
            assertTrue(
                bitsEq(actual, expected),
                "ceil($x): expected=${expected.toRawBits().toString(16)} " +
                    "actual=${actual.toRawBits().toString(16)}"
            )
        }
    }

    @Test
    fun trunc_matches_kotlin_math_for_all_samples() {
        for (x in samples) {
            val expected = kotlin.math.truncate(x)
            val actual = BasicMath.trunc(x)
            assertTrue(
                bitsEq(actual, expected),
                "trunc($x): expected=${expected.toRawBits().toString(16)} " +
                    "actual=${actual.toRawBits().toString(16)}"
            )
        }
    }

    @Test
    fun round_c99_half_away_from_zero() {
        // C99 round semantics: 0.5 → 1.0, -0.5 → -1.0, 2.5 → 3.0, -2.5 → -3.0
        // (Kotlin's `kotlin.math.round` uses banker's rounding, so we test
        // the explicit C99 semantics directly.)
        assertEquals(1.0, BasicMath.round(0.5))
        assertEquals(-1.0, BasicMath.round(-0.5))
        assertEquals(3.0, BasicMath.round(2.5))
        assertEquals(-3.0, BasicMath.round(-2.5))
        assertEquals(0.0, BasicMath.round(0.499999))
        assertEquals(2.0, BasicMath.round(1.5))
        assertTrue(BasicMath.round(Double.NaN).isNaN())
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.round(Double.POSITIVE_INFINITY))
    }

    @Test
    fun sqrt_bit_kernel_matches_kotlin_math() {
        val sqrtSamples = doubleArrayOf(
            0.0, 1.0, 2.0, 4.0, 9.0, 16.0,
            0.25, 0.5, 1e-10, 1e10, 1e-300, 1e300,
            Double.MIN_VALUE, Double.MAX_VALUE,
            Double.POSITIVE_INFINITY,
        )
        for (x in sqrtSamples) {
            val expected = kotlin.math.sqrt(x)
            val actual = BasicMath.sqrt(x)
            // sqrt is exact bit-for-bit for round-to-nearest-even on these inputs.
            assertTrue(
                bitsEq(actual, expected),
                "sqrt($x): expected=${expected.toRawBits().toString(16)} " +
                    "actual=${actual.toRawBits().toString(16)}"
            )
        }
        // Negative input → NaN.
        assertTrue(BasicMath.sqrt(-1.0).isNaN())
        // -Inf → NaN.
        assertTrue(BasicMath.sqrt(Double.NEGATIVE_INFINITY).isNaN())
        // ±0 preserves sign.
        assertEquals(0L, BasicMath.sqrt(0.0).toRawBits())
        assertEquals(Long.MIN_VALUE, BasicMath.sqrt(-0.0).toRawBits())
    }

    @Test
    fun frexp_round_trip_via_ldexp() {
        // For every finite non-zero x: ldexp(frexp(x).first, frexp(x).second) == x
        for (x in samples) {
            if (x == 0.0 || x.isNaN() || x.isInfinite()) continue
            val (m, e) = BasicMath.frexp(x)
            assertTrue(
                kotlin.math.abs(m) >= 0.5 && kotlin.math.abs(m) < 1.0,
                "frexp mantissa out of [0.5,1) for x=$x: m=$m"
            )
            val rebuilt = BasicMath.ldexp(m, e)
            assertEquals(
                x.toRawBits(), rebuilt.toRawBits(),
                "ldexp(frexp($x)) round-trip mismatch"
            )
        }
    }

    @Test
    fun ldexp_overflow_underflow() {
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.ldexp(1.0, 2000))
        assertEquals(Double.NEGATIVE_INFINITY, BasicMath.ldexp(-1.0, 2000))
        // Underflow to 0 (preserving sign).
        assertEquals(0.0, BasicMath.ldexp(1.0, -2000))
        assertEquals(-0.0, BasicMath.ldexp(-1.0, -2000))
    }

    @Test
    fun modf_split_equals_input() {
        for (x in samples) {
            if (x.isNaN()) {
                val (i, f) = BasicMath.modf(x)
                assertTrue(i.isNaN() && f.isNaN())
                continue
            }
            val (intPart, frac) = BasicMath.modf(x)
            // intPart is integral and same sign as x (or ±0).
            assertEquals(intPart, BasicMath.trunc(intPart))
            // Sum reconstructs x bit-for-bit (limited to representable cases).
            if (x.isInfinite()) {
                assertEquals(x.toRawBits(), intPart.toRawBits())
                continue
            }
            val sum = intPart + frac
            assertEquals(
                x.toRawBits(), sum.toRawBits(),
                "modf($x) split: intPart=$intPart frac=$frac sum=$sum"
            )
        }
    }
}

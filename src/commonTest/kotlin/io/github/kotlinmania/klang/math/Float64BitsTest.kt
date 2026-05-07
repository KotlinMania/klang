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

package io.github.kotlinmania.klang.math

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
    fun sqrt_bit_kernel_produces_RNE_correct_results() {
        // Asserts BasicMath.sqrt produces the IEEE-754 round-to-nearest-even
        // result for binary64 sqrt — verified against precomputed bit patterns,
        // not the host runtime. The previous version of this test compared
        // against `kotlin.math.sqrt`, which made the assertion depend on host
        // libm. That dependency was a bug in the test harness: Windows mingw
        // libm sqrt drifts by 1 ULP from spec on at least one of these inputs,
        // so the test failed on Windows even though klang's kernel — pure
        // Kotlin shift-and-add over the IEEE-754 bit pattern, host-independent
        // by construction — produced the correct answer everywhere.
        //
        // Each (input, expected) pair is the IEEE-754-mandated RNE result.
        // Reference values are the ones Java's spec-conformant Math.sqrt
        // returns and that macOS arm64 / Linux x64 libm both produce.
        val sqrtCases: Array<Pair<Double, Long>> = arrayOf(
            0.0                          to 0x0000_0000_0000_0000L,  // sqrt(+0) = +0
            1.0                          to 0x3FF0_0000_0000_0000L,  // sqrt(1)  = 1
            2.0                          to 0x3FF6_A09E_667F_3BCDL,  // sqrt(2)  ≈ 1.4142135623730951
            4.0                          to 0x4000_0000_0000_0000L,  // sqrt(4)  = 2
            9.0                          to 0x4008_0000_0000_0000L,  // sqrt(9)  = 3
            16.0                         to 0x4010_0000_0000_0000L,  // sqrt(16) = 4
            0.25                         to 0x3FE0_0000_0000_0000L,  // sqrt(0.25) = 0.5
            0.5                          to 0x3FE6_A09E_667F_3BCDL,  // sqrt(0.5)  ≈ 0.7071067811865476
            1e-10                        to 0x3EE4_F8B5_88E3_68F1L,
            1e10                         to 0x40F8_6A00_0000_0000L,  // exactly 100000.0
            1e-300                       to 0x20CA_2FE7_6A3F_9475L,
            1e300                        to 0x5F13_8D35_2E50_96AFL,
            Double.MIN_VALUE             to 0x1E60_0000_0000_0000L,  // sqrt of smallest subnormal
            Double.MAX_VALUE             to 0x5FEF_FFFF_FFFF_FFFFL,
            Double.POSITIVE_INFINITY     to 0x7FF0_0000_0000_0000L,  // sqrt(+Inf) = +Inf
        )
        for ((x, expectedBits) in sqrtCases) {
            val actualBits = BasicMath.sqrt(x).toRawBits()
            assertTrue(
                actualBits == expectedBits,
                "sqrt($x): expected=0x${expectedBits.toULong().toString(16).padStart(16, '0')} " +
                    "actual=0x${actualBits.toULong().toString(16).padStart(16, '0')}"
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

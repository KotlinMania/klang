/*
 * Copyright (c) 2026 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.fp

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [CE8M0]: 8-bit exponent-only scaling format used in MX/MXFP4 quantization.
 *
 * Reference values are derived from the upstream `ggml_e8m0_to_fp32` implementation
 * in llama.cpp's ggml-impl.h (Apache-2.0/MIT) and from IEEE-754 binary32 semantics.
 */
class CE8M0Test {

    /** Helper: call the convenience top-level function. */
    private fun decode(x: Int): Float = ce8m0ToFp32((x and 0xFF).toUByte())
    private fun decodeHalf(x: Int): Float = ce8m0ToFp32Half((x and 0xFF).toUByte())

    @Test
    fun zeroEncodesAsTwoToMinus127() {
        // x == 0 is the special denormal case: 2^(-127)
        val expected = Float.fromBits(0x00400000)
        assertEquals(expected, decode(0))
    }

    @Test
    fun oneEncodesAsTwoToMinus126() {
        // x == 1 -> exponent = 1, mantissa = 0 -> 2^(1 - 127) = 2^(-126)
        val expected = Float.fromBits(1 shl 23)
        assertEquals(expected, decode(1))
    }

    @Test
    fun oneTwentySevenIsOne() {
        // x == 127 -> 2^(0) = 1.0
        assertEquals(1.0f, decode(127))
    }

    @Test
    fun oneTwentyEightIsTwo() {
        // x == 128 -> 2^(1) = 2.0
        assertEquals(2.0f, decode(128))
    }

    @Test
    fun fullRangeMonotonicAndPositive() {
        // The decoded values across the entire E8M0 range are strictly positive
        // and monotonically non-decreasing.
        var prev = decode(0)
        assertTrue(prev > 0.0f, "x=0 should be positive (got $prev)")
        for (x in 1..255) {
            val v = decode(x)
            assertTrue(v > 0.0f, "x=$x should be positive (got $v)")
            assertTrue(v >= prev, "decode($x)=$v should be >= decode(${x - 1})=$prev")
            prev = v
        }
    }

    @Test
    fun halfIsExactlyOneHalfOfFull() {
        // toFloatHalf() should equal toFloat() / 2 for every x in [2, 254].
        // x == 255 is a deliberate asymmetry: full encodes as +Infinity (255 << 23 = 0x7F800000)
        // while half encodes as 2^127 (254 << 23 = 0x7F000000). Upstream behavior — the half
        // form does not overflow to infinity at the top of the range.
        for (x in 2..254) {
            val full = decode(x)
            val half = decodeHalf(x)
            assertEquals(full * 0.5f, half, "x=$x: half=$half, full/2=${full * 0.5f}")
        }
    }

    @Test
    fun halfDoesNotOverflowAtMaxEncoding() {
        // x == 255: half stays finite at 2^127, full saturates to +Infinity.
        assertTrue(decode(255).isInfinite(), "decode(255) should be +Infinity")
        assertEquals(Float.fromBits(0x7F000000), decodeHalf(255))
    }

    @Test
    fun halfBoundaryValues() {
        // x == 0 -> 0x00200000 = 2^(-128)
        assertEquals(Float.fromBits(0x00200000), decodeHalf(0))
        // x == 1 -> 0x00400000 = 2^(-127)
        assertEquals(Float.fromBits(0x00400000), decodeHalf(1))
    }

    @Test
    fun roundTripBitsPreserved() {
        for (x in 0..255) {
            val c = CE8M0.fromBits(x)
            assertEquals((x and 0xFF).toUByte(), c.toBits())
        }
    }

    @Test
    fun fromBitsUByteAndIntAgree() {
        for (x in 0..255) {
            val a = CE8M0.fromBits(x)
            val b = CE8M0.fromBits((x and 0xFF).toUByte())
            assertEquals(a, b)
            assertEquals(a.toFloat(), b.toFloat())
        }
    }

    @Test
    fun knownPowerOfTwoExpansion() {
        // Spot-check a handful of well-known powers of two.
        // x = 127 + k -> 2^k
        val cases = listOf(
            120 to (1.0f / 128.0f),  // 2^-7
            127 to 1.0f,             // 2^0
            130 to 8.0f,             // 2^3
            137 to 1024.0f,          // 2^10
            150 to (1 shl 23).toFloat() // 2^23
        )
        for ((x, expected) in cases) {
            val v = decode(x)
            assertTrue(abs(v - expected) <= expected * 1e-7f, "x=$x: got $v, want $expected")
        }
    }
}

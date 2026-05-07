/*
 * Copyright (c) 2026 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.fp

import ai.solace.klang.bitwise.BitShiftConfig
import ai.solace.klang.bitwise.BitShiftEngine
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [CE8M0]: 8-bit exponent-only scaling format used in MX/MXFP4 quantization.
 *
 * Reference values are derived from the upstream `ggml_e8m0_to_fp32` implementation
 * in llama.cpp's ggml-impl.h (Apache-2.0/MIT) and from IEEE-754 binary32 semantics.
 *
 * Bitwise operations route through [BitShiftEngine] per klang house rule.
 */
class CE8M0Test {

    private val engine = BitShiftEngine(BitShiftConfig.defaultMode, 32)

    /** Helper: build a CE8M0 byte from an Int via the engine, then decode. */
    private fun decode(x: Int): Float = ce8m0ToFp32(engine.bitwiseAnd(x.toLong(), engine.getMask(8)).toUByte())
    private fun decodeHalf(x: Int): Float = ce8m0ToFp32Half(engine.bitwiseAnd(x.toLong(), engine.getMask(8)).toUByte())

    /** Helper: build a fp32 power-of-two bit pattern via the engine. */
    private fun fp32Pow2Bits(unbiasedExp: Int): Int =
        engine.leftShift((unbiasedExp + 127).toLong(), 23).value.toInt()

    @Test
    fun zeroEncodesAsTwoToMinus127() {
        // x == 0 is the special denormal case: 2^(-127), encoded as a denormal:
        // sign=0, exp=0, mantissa=0x400000 (i.e. 0.5 * 2^(-126) = 2^(-127)).
        val expected = Float.fromBits(engine.leftShift(1L, 22).value.toInt())
        assertEquals(expected, decode(0))
    }

    @Test
    fun oneEncodesAsTwoToMinus126() {
        // x == 1 -> exponent = 1, mantissa = 0 -> 2^(1 - 127) = 2^(-126)
        val expected = Float.fromBits(fp32Pow2Bits(-126))
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
        assertEquals(Float.fromBits(fp32Pow2Bits(127)), decodeHalf(255))
    }

    @Test
    fun halfBoundaryValues() {
        // x == 0 -> denormal mantissa bit 21 -> 2^(-128)
        assertEquals(Float.fromBits(engine.leftShift(1L, 21).value.toInt()), decodeHalf(0))
        // x == 1 -> denormal mantissa bit 22 -> 2^(-127)
        assertEquals(Float.fromBits(engine.leftShift(1L, 22).value.toInt()), decodeHalf(1))
    }

    @Test
    fun roundTripBitsPreserved() {
        for (x in 0..255) {
            val c = CE8M0.fromBits(x)
            assertEquals(engine.bitwiseAnd(x.toLong(), engine.getMask(8)).toUByte(), c.toBits())
        }
    }

    @Test
    fun fromBitsUByteAndIntAgree() {
        for (x in 0..255) {
            val a = CE8M0.fromBits(x)
            val b = CE8M0.fromBits(engine.bitwiseAnd(x.toLong(), engine.getMask(8)).toUByte())
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
            150 to Float.fromBits(fp32Pow2Bits(23)) // 2^23
        )
        for ((x, expected) in cases) {
            val v = decode(x)
            assertTrue(abs(v - expected) <= expected * 1e-7f, "x=$x: got $v, want $expected")
        }
    }
}

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
 * Tests for [CUE4M3]: unsigned E4M3 (4 exponent + 3 mantissa, no sign) used in MXFP4.
 *
 * Reference values are derived from the upstream `ggml_ue4m3_to_fp32` /
 * `ggml_fp32_to_ue4m3` implementations in ggml-impl.h.
 */
class CUE4M3Test {

    private fun decode(x: Int): Float = ue4m3ToFp32((x and 0xFF).toUByte())
    private fun encode(f: Float): Int = fp32ToUe4m3(f).toInt() and 0xFF

    @Test
    fun zeroAndSpecialMaxDecodeToZero() {
        assertEquals(0.0f, decode(0))
        assertEquals(0.0f, decode(0x7F))
    }

    @Test
    fun subnormalsScaleByTwoToMinusNine() {
        // exp == 0, man == k -> raw = k * 2^-9, returned = raw * 0.5 = k * 2^-10
        for (k in 1..7) {
            val expectedRaw = k.toFloat() * (1.0f / 512.0f)
            val expectedHalved = expectedRaw * 0.5f
            val got = decode(k)
            assertEquals(expectedHalved, got, "subnormal k=$k")
        }
    }

    @Test
    fun normalizedOneIsExpEqualsBias() {
        // (1 + 0/8) * 2^(7 - 7) = 1.0 raw -> 0.5 returned
        // exp = 7, man = 0 -> bits = 7<<3 = 0x38
        assertEquals(0.5f, decode(0x38))
    }

    @Test
    fun maxFiniteEncodingDecodes() {
        // 0x7E = exp=15, man=6 -> shouldn't NaN here, this is the "max-finite" encoding upstream uses
        val got = decode(0x7E)
        // (1 + 6/8) * 2^(15 - 7) = 1.75 * 256 = 448.0 raw -> 224.0 returned
        assertEquals(224.0f, got)
    }

    @Test
    fun negativeAndNanEncodeToZero() {
        assertEquals(0, encode(-1.0f))
        assertEquals(0, encode(Float.NaN))
        assertEquals(0, encode(Float.NEGATIVE_INFINITY))
        assertEquals(0, encode(0.0f))
        assertEquals(0, encode(-0.0f))
    }

    @Test
    fun saturatesAtMaxEncodingForLargeInputs() {
        // Anything > 448.0f saturates to 0x7E (the max finite encoding upstream returns).
        assertEquals(0x7E, encode(Float.POSITIVE_INFINITY))
        assertEquals(0x7E, encode(1e30f))
        assertEquals(0x7E, encode(1024.0f))
    }

    @Test
    fun encodeDecodeRoundTripOnRepresentableValues() {
        // For x in [1, 0x77] the encoding round-trips: decode→encode returns the same bits.
        // x in [0x78, 0x7E] all decode to values >= 256.0, which the encoder saturates to 0x7E
        // (upstream behaviour: any value whose post-bias exponent reaches 15 maps to 0x7E).
        // x == 0x7F is the special "decodes to 0" case.
        for (x in 1..0x77) {
            val raw = CUE4M3.fromBits(x).decodeRaw()
            val reenc = CUE4M3.fromFloat(raw).toBits().toInt() and 0xFF
            assertEquals(x, reenc, "round-trip failed for x=0x${x.toString(16)} (raw=$raw)")
        }
    }

    @Test
    fun saturatedEncodingsCollapseToMaxFinite() {
        // x in [0x78, 0x7E] all decode to >= 256.0 which the encoder saturates to 0x7E.
        for (x in 0x78..0x7E) {
            val raw = CUE4M3.fromBits(x).decodeRaw()
            val reenc = CUE4M3.fromFloat(raw).toBits().toInt() and 0xFF
            assertEquals(0x7E, reenc, "x=0x${x.toString(16)} (raw=$raw) should saturate to 0x7E")
        }
    }

    @Test
    fun decodeRawIsTwiceToFloat() {
        // toFloat() applies a 0.5x doubling convention; decodeRaw() does not.
        for (x in 0..0xFF) {
            val full = CUE4M3.fromBits(x).decodeRaw()
            val halved = CUE4M3.fromBits(x).toFloat()
            // Allow exact equality because the multiplication is by 0.5 (representable).
            assertEquals(full * 0.5f, halved, "x=0x${x.toString(16)}")
        }
    }

    @Test
    fun fromBitsUByteAndIntAgree() {
        for (x in 0..255) {
            val a = CUE4M3.fromBits(x)
            val b = CUE4M3.fromBits((x and 0xFF).toUByte())
            assertEquals(a, b)
            assertEquals(a.toFloat(), b.toFloat())
        }
    }

    @Test
    fun knownDecodedValues() {
        // exp=8, man=0 -> (1 + 0/8) * 2^(8-7) = 2.0 raw -> 1.0 returned. bits = 8<<3 = 0x40
        assertEquals(1.0f, decode(0x40))
        // exp=9, man=4 -> (1 + 4/8) * 2^(9-7) = 1.5 * 4 = 6.0 raw -> 3.0 returned. bits = 9<<3 | 4 = 0x4C
        assertEquals(3.0f, decode(0x4C))
        // exp=10, man=0 -> (1 + 0) * 2^3 = 8.0 raw -> 4.0 returned. bits = 10<<3 = 0x50
        assertEquals(4.0f, decode(0x50))
    }
}

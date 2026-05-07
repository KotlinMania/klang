/*
 * Copyright (c) 2026 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.fp

import ai.solace.klang.mem.CAutos
import ai.solace.klang.mem.CGlobals
import ai.solace.klang.mem.CHeapVars
import ai.solace.klang.mem.GlobalData
import ai.solace.klang.mem.KStack
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

    @Test
    fun zeroAndSpecialMaxDecodeToZero() {
        assertEquals(0.0f, CUE4M3.fromBits(0).toFloat())
        assertEquals(0.0f, CUE4M3.fromBits(0x7F).toFloat())
    }

    @Test
    fun subnormalsScaleByTwoToMinusNine() {
        // exp == 0, man == k -> raw = k × 2^-9, returned = raw × 0.5 = k × 2^-10
        for (k in 1..7) {
            val expectedRaw = k.toFloat() * (1.0f / 512.0f)
            val expectedHalved = expectedRaw * 0.5f
            val got = CUE4M3.fromBits(k).toFloat()
            assertEquals(expectedHalved, got, "subnormal k=$k")
        }
    }

    @Test
    fun normalizedOneRawIsOneHalfReturned() {
        // exp = 7, man = 0 -> bits = 7<<3 = 0x38; raw = 1.0 -> returned 0.5
        assertEquals(0.5f, CUE4M3.fromBits(0x38).toFloat())
    }

    @Test
    fun maxFiniteEncodingDecodes() {
        // 0x7E = exp=15, man=6 -> raw = 1.75 × 2^8 = 448.0 -> returned 224.0
        assertEquals(224.0f, CUE4M3.fromBits(0x7E).toFloat())
    }

    @Test
    fun negativeAndNanEncodeToZero() {
        assertEquals(0, CUE4M3.fromFloat(-1.0f).toBits())
        assertEquals(0, CUE4M3.fromFloat(Float.NaN).toBits())
        assertEquals(0, CUE4M3.fromFloat(Float.NEGATIVE_INFINITY).toBits())
        assertEquals(0, CUE4M3.fromFloat(0.0f).toBits())
        assertEquals(0, CUE4M3.fromFloat(-0.0f).toBits())
    }

    @Test
    fun saturatesAtMaxEncodingForLargeInputs() {
        assertEquals(0x7E, CUE4M3.fromFloat(Float.POSITIVE_INFINITY).toBits())
        assertEquals(0x7E, CUE4M3.fromFloat(1e30f).toBits())
        assertEquals(0x7E, CUE4M3.fromFloat(1024.0f).toBits())
    }

    @Test
    fun encodeDecodeRoundTripOnRepresentableValues() {
        // For x in [1, 0x77] decode→encode returns the same bits.
        // x in [0x78, 0x7E] all decode to >= 256.0 which the encoder saturates to 0x7E.
        // x == 0x7F is the special "decodes to 0" case.
        for (x in 1..0x77) {
            val raw = CUE4M3.fromBits(x).decodeRaw()
            val reenc = CUE4M3.fromFloat(raw).toBits()
            assertEquals(x, reenc, "round-trip failed for x=0x${x.toString(16)} (raw=$raw)")
        }
    }

    @Test
    fun saturatedEncodingsCollapseToMaxFinite() {
        for (x in 0x78..0x7E) {
            val raw = CUE4M3.fromBits(x).decodeRaw()
            val reenc = CUE4M3.fromFloat(raw).toBits()
            assertEquals(0x7E, reenc, "x=0x${x.toString(16)} (raw=$raw) should saturate to 0x7E")
        }
    }

    @Test
    fun decodeRawIsTwiceToFloat() {
        for (x in 0..0xFF) {
            val full = CUE4M3.fromBits(x).decodeRaw()
            val halved = CUE4M3.fromBits(x).toFloat()
            assertEquals(full * 0.5f, halved, "x=0x${x.toString(16)}")
        }
    }

    @Test
    fun fromBitsUByteAndIntAgree() {
        for (x in 0..255) {
            val a = CUE4M3.fromBits(x)
            val b = CUE4M3.fromBits(x.toUByte())
            assertEquals(a, b)
            assertEquals(a.toFloat(), b.toFloat())
        }
    }

    @Test
    fun knownDecodedValues() {
        assertEquals(1.0f, CUE4M3.fromBits(0x40).toFloat())  // exp=8, man=0
        assertEquals(3.0f, CUE4M3.fromBits(0x4C).toFloat())  // exp=9, man=4
        assertEquals(4.0f, CUE4M3.fromBits(0x50).toFloat())  // exp=10, man=0
    }

    // ---------- Heap-backed storage tests (CAutos / CGlobals / CHeapVars) ----------

    @Test
    fun stackStorageRoundTrip() {
        KStack.init()
        KStack.withFrame {
            val v = CAutos.ue4m3(CUE4M3.fromFloat(2.0f))  // raw=2.0, returned=1.0
            assertEquals(1.0f, v.value.toFloat())
            v.value = CUE4M3.fromBits(0x40)               // exp=8, man=0 -> 1.0
            assertEquals(1.0f, v.value.toFloat())
            v.value = CUE4M3.ZERO
            assertEquals(0.0f, v.value.toFloat())
        }
    }

    @Test
    fun stackStorageDefaultIsZero() {
        KStack.init()
        KStack.withFrame {
            val v = CAutos.ue4m3()
            assertEquals(0, v.value.toBits())
            assertEquals(0.0f, v.value.toFloat())
        }
    }

    @Test
    fun globalStorageRoundTrip() {
        GlobalData.init()
        val v = CGlobals.ue4m3("cue4m3_test_value", CUE4M3.fromBits(0x4C))  // -> 3.0
        assertEquals(3.0f, v.value.toFloat())
        v.value = CUE4M3.fromBits(0x50)  // -> 4.0
        assertEquals(4.0f, v.value.toFloat())
    }

    @Test
    fun heapStorageRoundTrip() {
        val v = CHeapVars.ue4m3(CUE4M3.fromBits(0x7E))  // max finite -> 224.0
        try {
            assertEquals(224.0f, v.value.toFloat())
            v.value = CUE4M3.fromBits(0x38)  // raw=1.0 -> returned 0.5
            assertEquals(0.5f, v.value.toFloat())
        } finally {
            CHeapVars.free(v)
        }
    }
}

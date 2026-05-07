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
 * Tests for [CE8M0]: 8-bit exponent-only scaling format used in MX/MXFP4 quantization.
 *
 * Reference values are derived from the upstream `ggml_e8m0_to_fp32` implementation
 * in llama.cpp's ggml-impl.h (Apache-2.0/MIT) and from IEEE-754 binary32 semantics.
 *
 * Tests follow the heap-backed `CAutos`/`CGlobals`/`CHeapVars` pattern used by the
 * other narrow-FP types (CFloat16, CBF16) — see [MLTypesTest] for the convention.
 */
class CE8M0Test {

    @Test
    fun zeroEncodesAsTwoToMinus127() {
        // x == 0 is the special denormal case: 2^(-127), encoded as a denormal with
        // mantissa bit 22 set (sign=0, exp=0, mantissa=0x400000 = 0.5 × 2^(-126)).
        val expected = Float.fromBits(1 shl 22)
        assertEquals(expected, CE8M0.fromBits(0).toFloat())
    }

    @Test
    fun oneEncodesAsTwoToMinus126() {
        // x == 1 -> exponent field = 1, mantissa = 0 -> 2^(1 - 127) = 2^(-126)
        val expected = Float.fromBits(1 shl 23)
        assertEquals(expected, CE8M0.fromBits(1).toFloat())
    }

    @Test
    fun oneTwentySevenIsOne() {
        assertEquals(1.0f, CE8M0.fromBits(127).toFloat())
    }

    @Test
    fun oneTwentyEightIsTwo() {
        assertEquals(2.0f, CE8M0.fromBits(128).toFloat())
    }

    @Test
    fun fullRangeMonotonicAndPositive() {
        // Values are strictly positive across the entire E8M0 range and
        // monotonically non-decreasing.
        var prev = CE8M0.fromBits(0).toFloat()
        assertTrue(prev > 0.0f, "x=0 should be positive (got $prev)")
        for (x in 1..255) {
            val v = CE8M0.fromBits(x).toFloat()
            assertTrue(v > 0.0f, "x=$x should be positive (got $v)")
            assertTrue(v >= prev, "decode($x)=$v should be >= decode(${x - 1})=$prev")
            prev = v
        }
    }

    @Test
    fun halfIsExactlyOneHalfOfFullExceptAtMax() {
        // toFloatHalf() == toFloat() / 2 for x in [2, 254]. At x == 255 the full
        // form saturates to +Infinity while the half stays finite at 2^127 — that
        // asymmetry is upstream-defined and exercised in the test below.
        for (x in 2..254) {
            val full = CE8M0.fromBits(x).toFloat()
            val half = CE8M0.fromBits(x).toFloatHalf()
            assertEquals(full * 0.5f, half, "x=$x: half=$half, full/2=${full * 0.5f}")
        }
    }

    @Test
    fun halfDoesNotOverflowAtMaxEncoding() {
        assertTrue(CE8M0.fromBits(255).toFloat().isInfinite(), "decode(255) should be +Infinity")
        // half(255) = 2^127 (finite max-finite power-of-two), bit pattern (255-1) << 23.
        assertEquals(Float.fromBits((255 - 1) shl 23), CE8M0.fromBits(255).toFloatHalf())
    }

    @Test
    fun halfBoundaryValues() {
        // x == 0 -> 2^(-128): denormal with mantissa bit 21 set.
        assertEquals(Float.fromBits(1 shl 21), CE8M0.fromBits(0).toFloatHalf())
        // x == 1 -> 2^(-127): denormal with mantissa bit 22 set.
        assertEquals(Float.fromBits(1 shl 22), CE8M0.fromBits(1).toFloatHalf())
    }

    @Test
    fun roundTripBitsPreserved() {
        for (x in 0..255) {
            assertEquals(x, CE8M0.fromBits(x).toBits())
        }
    }

    @Test
    fun fromBitsUByteAndIntAgree() {
        for (x in 0..255) {
            val a = CE8M0.fromBits(x)
            val b = CE8M0.fromBits(x.toUByte())
            assertEquals(a, b)
            assertEquals(a.toFloat(), b.toFloat())
        }
    }

    // ---------- Heap-backed storage tests (CAutos / CGlobals / CHeapVars) ----------

    @Test
    fun stackStorageRoundTrip() {
        KStack.init()
        KStack.withFrame {
            val v = CAutos.e8m0(CE8M0.fromBits(127))
            assertEquals(1.0f, v.value.toFloat())
            v.value = CE8M0.fromBits(130)
            assertEquals(8.0f, v.value.toFloat())  // 2^3
            v.value = CE8M0.ZERO
            assertEquals(Float.fromBits(1 shl 22), v.value.toFloat())
        }
    }

    @Test
    fun stackStorageDefaultIsZero() {
        KStack.init()
        KStack.withFrame {
            val v = CAutos.e8m0()
            assertEquals(0, v.value.toBits())
        }
    }

    @Test
    fun globalStorageRoundTrip() {
        GlobalData.init()
        val scale = CGlobals.e8m0("ce8m0_test_scale", CE8M0.fromBits(127))
        assertEquals(1.0f, scale.value.toFloat())
        scale.value = CE8M0.fromBits(150)  // 2^23
        assertEquals(Float.fromBits(150 shl 23), scale.value.toFloat())
    }

    @Test
    fun heapStorageRoundTrip() {
        val v = CHeapVars.e8m0(CE8M0.fromBits(140))  // 2^13 = 8192.0
        try {
            assertEquals(8192.0f, v.value.toFloat())
            v.value = CE8M0.fromBits(124)  // 2^(-3) = 0.125
            assertEquals(0.125f, v.value.toFloat())
        } finally {
            CHeapVars.free(v)
        }
    }
}

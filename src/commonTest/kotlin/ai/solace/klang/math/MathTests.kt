/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for Basic math functions (fabs, copysign, signbit).
 * 
 * Tests cover:
 * - Normal values (positive and negative)
 * - Signed zeros (+0, -0)
 * - Infinities (+∞, -∞)
 * - NaN (quiet and signaling if distinguishable)
 * - Subnormal values
 * 
 * @since 0.2.0
 */
class BasicTest {
    
    @Test
    fun testFabsNormalValues() {
        assertEquals(5.0, KMath.fabs(5.0))
        assertEquals(5.0, KMath.fabs(-5.0))
        assertEquals(1.5, KMath.fabs(1.5))
        assertEquals(1.5, KMath.fabs(-1.5))
        assertEquals(Double.MAX_VALUE, KMath.fabs(Double.MAX_VALUE))
        assertEquals(Double.MAX_VALUE, KMath.fabs(-Double.MAX_VALUE))
    }
    
    @Test
    fun testFabsZeros() {
        // Both zeros should return +0
        assertEquals(0.0, KMath.fabs(0.0))
        assertEquals(0.0, KMath.fabs(-0.0))
        
        // Verify it's actually +0 (not -0)
        val plusZero = KMath.fabs(-0.0)
        assertTrue(plusZero.toRawBits() == 0L, "fabs(-0) should return +0")
    }
    
    @Test
    fun testFabsInfinities() {
        assertEquals(Double.POSITIVE_INFINITY, KMath.fabs(Double.POSITIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, KMath.fabs(Double.NEGATIVE_INFINITY))
    }
    
    @Test
    fun testFabsNaN() {
        val result = KMath.fabs(Double.NaN)
        assertTrue(result.isNaN(), "fabs(NaN) should return NaN")
        
        // Verify NaN payload is preserved (sign bit cleared)
        val nanBits = Double.NaN.toRawBits()
        val resultBits = result.toRawBits()
        assertEquals(nanBits and 0x7FFF_FFFF_FFFF_FFFFL, resultBits)
    }
    
    @Test
    fun testFabsSubnormal() {
        val smallestPositive = Double.fromBits(1L) // Smallest positive subnormal
        val smallestNegative = Double.fromBits(Long.MIN_VALUE or 1L)
        
        assertEquals(smallestPositive, KMath.fabs(smallestPositive))
        assertEquals(smallestPositive, KMath.fabs(smallestNegative))
    }
    
    @Test
    fun testCopysignNormalValues() {
        assertEquals(5.0, KMath.copysign(5.0, 1.0))
        assertEquals(-5.0, KMath.copysign(5.0, -1.0))
        assertEquals(5.0, KMath.copysign(-5.0, 1.0))
        assertEquals(-5.0, KMath.copysign(-5.0, -1.0))
    }
    
    @Test
    fun testCopysignZeros() {
        // Test all combinations of signed zeros
        val plusZero = 0.0
        val minusZero = -0.0
        
        assertEquals(plusZero, KMath.copysign(plusZero, plusZero))
        val copiedNegZero = KMath.copysign(plusZero, minusZero)
        assertTrue(KMath.signbit(copiedNegZero))
        assertEquals(plusZero, KMath.copysign(minusZero, plusZero))
        val copiedNegZero2 = KMath.copysign(minusZero, minusZero)
        assertTrue(KMath.signbit(copiedNegZero2))
    }
    
    @Test
    fun testCopysignInfinities() {
        assertEquals(Double.POSITIVE_INFINITY, KMath.copysign(Double.POSITIVE_INFINITY, 1.0))
        assertEquals(Double.NEGATIVE_INFINITY, KMath.copysign(Double.POSITIVE_INFINITY, -1.0))
    }
    
    @Test
    fun testCopysignNaN() {
        val result = KMath.copysign(Double.NaN, -1.0)
        assertTrue(result.isNaN())
        assertTrue(KMath.signbit(result), "copysign(NaN, -1) should have negative sign")
    }
    
    @Test
    fun testSignbitNormalValues() {
        assertFalse(KMath.signbit(5.0))
        assertTrue(KMath.signbit(-5.0))
        assertFalse(KMath.signbit(Double.MAX_VALUE))
        assertTrue(KMath.signbit(-Double.MAX_VALUE))
    }
    
    @Test
    fun testSignbitZeros() {
        assertFalse(KMath.signbit(0.0), "+0 should have positive sign")
        assertTrue(KMath.signbit(-0.0), "-0 should have negative sign")
    }
    
    @Test
    fun testSignbitInfinities() {
        assertFalse(KMath.signbit(Double.POSITIVE_INFINITY))
        assertTrue(KMath.signbit(Double.NEGATIVE_INFINITY))
    }
    
    @Test
    fun testExtensionFunctions() {
        // Test extension function wrappers
        assertEquals(5.0, 5.0.abs())
        assertEquals(5.0, (-5.0).abs())
        
        assertEquals(-5.0, 5.0.withSign(-1.0))
        assertEquals(5.0, (-5.0).withSign(1.0))
        
        assertFalse(5.0.hasNegativeSign)
        assertTrue((-5.0).hasNegativeSign)
    }
}

/**
 * Test suite for Classification functions.
 */
class ClassificationTest {
    
    @Test
    fun testIsnanNormalValues() {
        assertFalse(Classification.isnan(0.0))
        assertFalse(Classification.isnan(1.0))
        assertFalse(Classification.isnan(-1.0))
        assertFalse(Classification.isnan(Double.MAX_VALUE))
    }
    
    @Test
    fun testIsnanSpecialValues() {
        assertTrue(Classification.isnan(Double.NaN))
        assertFalse(Classification.isnan(Double.POSITIVE_INFINITY))
        assertFalse(Classification.isnan(Double.NEGATIVE_INFINITY))
        assertFalse(Classification.isnan(0.0))
        assertFalse(Classification.isnan(-0.0))
    }
    
    @Test
    fun testIsinfNormalValues() {
        assertFalse(Classification.isinf(0.0))
        assertFalse(Classification.isinf(1.0))
        assertFalse(Classification.isinf(-1.0))
        assertFalse(Classification.isinf(Double.MAX_VALUE))
    }
    
    @Test
    fun testIsinfSpecialValues() {
        assertTrue(Classification.isinf(Double.POSITIVE_INFINITY))
        assertTrue(Classification.isinf(Double.NEGATIVE_INFINITY))
        assertFalse(Classification.isinf(Double.NaN))
        assertFalse(Classification.isinf(0.0))
    }
    
    @Test
    fun testIsfiniteValues() {
        assertTrue(Classification.isfinite(0.0))
        assertTrue(Classification.isfinite(1.0))
        assertTrue(Classification.isfinite(-1.0))
        assertTrue(Classification.isfinite(Double.MAX_VALUE))
        assertTrue(Classification.isfinite(Double.MIN_VALUE))
        
        assertFalse(Classification.isfinite(Double.POSITIVE_INFINITY))
        assertFalse(Classification.isfinite(Double.NEGATIVE_INFINITY))
        assertFalse(Classification.isfinite(Double.NaN))
    }
    
    @Test
    fun testIszero() {
        assertTrue(Classification.iszero(0.0))
        assertTrue(Classification.iszero(-0.0))
        
        assertFalse(Classification.iszero(Double.MIN_VALUE))
        assertFalse(Classification.iszero(1.0))
    }
    
    @Test
    fun testIssubnormal() {
        val smallestSubnormal = Double.fromBits(1L)
        assertTrue(Classification.issubnormal(smallestSubnormal))
        assertTrue(Classification.issubnormal(-smallestSubnormal))
        
        assertFalse(Classification.issubnormal(0.0))
        assertFalse(Classification.issubnormal(1.0))
        // Note: Double.MIN_VALUE is actually the smallest POSITIVE subnormal value
        // so this should be TRUE, not FALSE
        assertTrue(Classification.issubnormal(Double.MIN_VALUE))
    }
    
    @Test
    fun testIsnormal() {
        assertTrue(Classification.isnormal(1.0))
        assertTrue(Classification.isnormal(-1.0))
        assertTrue(Classification.isnormal(Double.MAX_VALUE))
        
        assertFalse(Classification.isnormal(0.0))
        assertFalse(Classification.isnormal(Double.POSITIVE_INFINITY))
        assertFalse(Classification.isnormal(Double.NaN))
    }
    
    @Test
    fun testExtensionFunctions() {
        assertTrue(Double.NaN.isNaN())
        assertTrue(Double.POSITIVE_INFINITY.isInfinite())
        assertTrue(1.0.isFinite())
    }
}

/**
 * Test suite for Comparison functions.
 */
class ComparisonTest {
    
    @Test
    fun testFmaxNormalValues() {
        assertEquals(5.0, Comparison.fmax(5.0, 3.0))
        assertEquals(5.0, Comparison.fmax(3.0, 5.0))
        assertEquals(0.0, Comparison.fmax(-1.0, 0.0))
        assertEquals(0.0, Comparison.fmax(0.0, -1.0))
    }
    
    @Test
    fun testFmaxNaN() {
        // When one argument is NaN, return the non-NaN
        assertEquals(5.0, Comparison.fmax(Double.NaN, 5.0))
        assertEquals(5.0, Comparison.fmax(5.0, Double.NaN))
        
        // When both are NaN, return NaN
        assertTrue(Comparison.fmax(Double.NaN, Double.NaN).isNaN())
    }
    
    @Test
    fun testFmaxSignedZeros() {
        // fmax(+0, -0) should return +0
        val result = Comparison.fmax(0.0, -0.0)
        assertEquals(0.0, result)
        assertFalse(KMath.signbit(result), "fmax(+0, -0) should return +0")
        
        // fmax(-0, +0) should also return +0
        val result2 = Comparison.fmax(-0.0, 0.0)
        assertFalse(KMath.signbit(result2), "fmax(-0, +0) should return +0")
    }
    
    @Test
    fun testFmaxInfinities() {
        assertEquals(Double.POSITIVE_INFINITY, Comparison.fmax(Double.POSITIVE_INFINITY, 1.0))
        assertEquals(Double.POSITIVE_INFINITY, Comparison.fmax(1.0, Double.POSITIVE_INFINITY))
        assertEquals(1.0, Comparison.fmax(Double.NEGATIVE_INFINITY, 1.0))
    }
    
    @Test
    fun testFminNormalValues() {
        assertEquals(3.0, Comparison.fmin(5.0, 3.0))
        assertEquals(3.0, Comparison.fmin(3.0, 5.0))
        assertEquals(-1.0, Comparison.fmin(-1.0, 0.0))
        assertEquals(-1.0, Comparison.fmin(0.0, -1.0))
    }
    
    @Test
    fun testFminNaN() {
        // When one argument is NaN, return the non-NaN
        assertEquals(5.0, Comparison.fmin(Double.NaN, 5.0))
        assertEquals(5.0, Comparison.fmin(5.0, Double.NaN))
        
        // When both are NaN, return NaN
        assertTrue(Comparison.fmin(Double.NaN, Double.NaN).isNaN())
    }
    
    @Test
    fun testFminSignedZeros() {
        // fmin(+0, -0) should return -0
        val result = Comparison.fmin(0.0, -0.0)
        assertEquals(-0.0, result)
        assertTrue(KMath.signbit(result), "fmin(+0, -0) should return -0")
        
        // fmin(-0, +0) should also return -0
        val result2 = Comparison.fmin(-0.0, 0.0)
        assertTrue(KMath.signbit(result2), "fmin(-0, +0) should return -0")
    }
    
    @Test
    fun testFminInfinities() {
        assertEquals(1.0, Comparison.fmin(Double.POSITIVE_INFINITY, 1.0))
        assertEquals(1.0, Comparison.fmin(1.0, Double.POSITIVE_INFINITY))
        assertEquals(Double.NEGATIVE_INFINITY, Comparison.fmin(Double.NEGATIVE_INFINITY, 1.0))
    }
    
    @Test
    fun testFdim() {
        assertEquals(2.0, Comparison.fdim(5.0, 3.0))
        assertEquals(0.0, Comparison.fdim(3.0, 5.0))
        assertEquals(0.0, Comparison.fdim(5.0, 5.0))
        
        // With infinity
        assertEquals(Double.POSITIVE_INFINITY, Comparison.fdim(Double.POSITIVE_INFINITY, 1.0))
        assertEquals(0.0, Comparison.fdim(1.0, Double.POSITIVE_INFINITY))
        
        // With NaN
        assertTrue(Comparison.fdim(Double.NaN, 1.0).isNaN())
        assertTrue(Comparison.fdim(1.0, Double.NaN).isNaN())
    }
    
    @Test
    fun testExtensionFunctions() {
        assertEquals(5.0, 5.0.max(3.0))
        assertEquals(3.0, 5.0.min(3.0))
    }
}

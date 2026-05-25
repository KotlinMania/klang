package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class C_Int64Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMinMax() {
        assertEquals(0L, C_Int64.zero().toLong())
        assertEquals(1L, C_Int64.one().toLong())
        assertEquals(Long.MIN_VALUE, C_Int64.minValue().toLong())
        assertEquals(Long.MAX_VALUE, C_Int64.maxValue().toLong())
    }

    @Test
    fun roundTripLong() {
        for (v in listOf(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x7FFF_FFFF_FFFF_FFFFL, -0x1234_5678_9ABC_DEF0L)) {
            assertEquals(v, C_Int64.fromLong(v).toLong())
        }
    }

    @Test
    fun signDetection() {
        assertTrue(C_Int64.fromLong(-1L).isNegative())
        assertTrue(C_Int64.minValue().isNegative())
        assertFalse(C_Int64.zero().isNegative())
        assertFalse(C_Int64.maxValue().isNegative())
    }

    @Test
    fun additionMixedSigns() {
        assertEquals(50L, (C_Int64.fromLong(100L) + C_Int64.fromLong(-50L)).toLong())
        assertEquals(-50L, (C_Int64.fromLong(-100L) + C_Int64.fromLong(50L)).toLong())
    }

    @Test
    fun additionWraps() {
        assertEquals(Long.MIN_VALUE, (C_Int64.maxValue() + C_Int64.one()).toLong())
        assertEquals(Long.MAX_VALUE, (C_Int64.minValue() + C_Int64.fromLong(-1L)).toLong())
    }

    @Test
    fun subtractionAndMultiplicationSigns() {
        assertEquals(150L, (C_Int64.fromLong(100L) - C_Int64.fromLong(-50L)).toLong())
        assertEquals(-200L, (C_Int64.fromLong(10L) * C_Int64.fromLong(-20L)).toLong())
        assertEquals(200L, (C_Int64.fromLong(-10L) * C_Int64.fromLong(-20L)).toLong())
    }

    @Test
    fun negationAndAbs() {
        assertEquals(-100L, C_Int64.fromLong(100L).negate().toLong())
        assertEquals(100L, C_Int64.fromLong(-100L).abs().toLong())
        // -MIN wraps to MIN in two's complement
        assertEquals(Long.MIN_VALUE, C_Int64.minValue().negate().toLong())
        assertEquals(Long.MIN_VALUE, C_Int64.minValue().abs().toLong())
        assertEquals(-42L, (-C_Int64.fromLong(42L)).toLong())
    }

    @Test
    fun divisionAndModulus() {
        assertEquals(-1000L / 7L, (C_Int64.fromLong(-1000L) / C_Int64.fromLong(7L)).toLong())
        assertEquals(-1000L % 7L, (C_Int64.fromLong(-1000L) % C_Int64.fromLong(7L)).toLong())
        assertEquals(1000L / -7L, (C_Int64.fromLong(1000L) / C_Int64.fromLong(-7L)).toLong())
        assertFailsWith<IllegalArgumentException> { C_Int64.fromLong(1L) / C_Int64.zero() }
        assertFailsWith<IllegalArgumentException> { C_Int64.fromLong(1L) % C_Int64.zero() }
    }

    @Test
    fun bitwiseTruthTables() {
        val a = C_Int64.fromLong(0x0F0F_0F0F_0F0F_0F0FL)
        val b = C_Int64.fromLong(0x00FF_00FF_00FF_00FFL)
        assertEquals(0x0F0F_0F0F_0F0F_0F0FL and 0x00FF_00FF_00FF_00FFL, (a and b).toLong())
        assertEquals(0x0F0F_0F0F_0F0F_0F0FL or 0x00FF_00FF_00FF_00FFL, (a or b).toLong())
        assertEquals(0x0F0F_0F0F_0F0F_0F0FL xor 0x00FF_00FF_00FF_00FFL, (a xor b).toLong())
        assertEquals(-1L, C_Int64.zero().inv().toLong())
        assertEquals(0x1234_5678L, C_Int64.fromLong(0x1234_5678L).inv().inv().toLong())
    }

    @Test
    fun shiftLeftAcrossWidth() {
        assertEquals(2L, C_Int64.one().shiftLeft(1).toLong())
        assertEquals(0x1_0000_0000L, C_Int64.one().shiftLeft(32).toLong())
        assertEquals(0x4000_0000_0000_0000L, C_Int64.one().shiftLeft(62).toLong())
        assertEquals(Long.MIN_VALUE, C_Int64.one().shiftLeft(63).toLong())
    }

    @Test
    fun arithmeticRightShiftSignExtends() {
        assertEquals(-64L, C_Int64.fromLong(-256L).shiftRight(2).toLong())
        assertEquals(-1L, C_Int64.fromLong(-1L).shiftRight(63).toLong())
        assertEquals(64L, C_Int64.fromLong(256L).shiftRight(2).toLong())
        // identity
        assertEquals(-1234567L, C_Int64.fromLong(-1234567L).shiftRight(0).toLong())
    }

    @Test
    fun logicalRightShiftZeroFill() {
        assertEquals(-1L ushr 1, C_Int64.fromLong(-1L).shiftRightUnsigned(1).toLong())
        assertEquals(-256L ushr 8, C_Int64.fromLong(-256L).shiftRightUnsigned(8).toLong())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_Int64.one().shiftLeft(64) }
        assertFailsWith<IllegalArgumentException> { C_Int64.one().shiftRight(64) }
    }

    @Test
    fun comparisonAcrossSigns() {
        assertTrue(C_Int64.fromLong(-1L) < C_Int64.zero())
        assertTrue(C_Int64.minValue() < C_Int64.maxValue())
        assertTrue(C_Int64.minValue() < C_Int64.fromLong(Long.MIN_VALUE + 1L))
        assertEquals(0, C_Int64.fromLong(42L).compareTo(C_Int64.fromLong(42L)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_Int64.fromLong(-123_456_789L)
        val b = C_Int64.fromLong(-123_456_789L)
        assertEquals(a, b)
        assertNotEquals(a, C_Int64.fromLong(123_456_789L))
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals(-123_456_789L))
        assertFalse(a.equals(null))
    }

    @Test
    fun hexFormat() {
        assertEquals("0x0000000000000000", C_Int64.zero().toHexString())
        assertEquals("0xffffffffffffffff", C_Int64.fromLong(-1L).toHexString())
        assertEquals("0x8000000000000000", C_Int64.minValue().toHexString())
        assertEquals("0x7fffffffffffffff", C_Int64.maxValue().toHexString())
    }
}

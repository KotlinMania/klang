package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class C_Int32Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMinMax() {
        assertEquals(0, C_Int32.zero().toInt())
        assertEquals(1, C_Int32.one().toInt())
        assertEquals(Int.MIN_VALUE, C_Int32.minValue().toInt())
        assertEquals(Int.MAX_VALUE, C_Int32.maxValue().toInt())
    }

    @Test
    fun roundTripInt() {
        for (v in listOf(0, 1, -1, 42, -42, 0x7FFFFFFF, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertEquals(v, C_Int32.fromInt(v).toInt())
        }
    }

    @Test
    fun signDetection() {
        assertTrue(C_Int32.fromInt(-1).isNegative())
        assertTrue(C_Int32.minValue().isNegative())
        assertFalse(C_Int32.zero().isNegative())
        assertFalse(C_Int32.maxValue().isNegative())
    }

    @Test
    fun additionMixedSigns() {
        assertEquals(50, (C_Int32.fromInt(100) + C_Int32.fromInt(-50)).toInt())
        assertEquals(-50, (C_Int32.fromInt(-100) + C_Int32.fromInt(50)).toInt())
        assertEquals(0, (C_Int32.fromInt(-12345) + C_Int32.fromInt(12345)).toInt())
    }

    @Test
    fun additionWraps() {
        assertEquals(Int.MIN_VALUE, (C_Int32.maxValue() + C_Int32.one()).toInt())
        assertEquals(Int.MAX_VALUE, (C_Int32.minValue() + C_Int32.fromInt(-1)).toInt())
        assertEquals(-2, (C_Int32.maxValue() + C_Int32.maxValue()).toInt())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(Int.MAX_VALUE, (C_Int32.minValue() - C_Int32.one()).toInt())
    }

    @Test
    fun negationAndAbs() {
        assertEquals(-100, C_Int32.fromInt(100).negate().toInt())
        assertEquals(100, C_Int32.fromInt(-100).abs().toInt())
        assertEquals(Int.MAX_VALUE, C_Int32.maxValue().abs().toInt())
        // -MIN wraps to MIN in two's complement
        assertEquals(Int.MIN_VALUE, C_Int32.minValue().negate().toInt())
        assertEquals(Int.MIN_VALUE, C_Int32.minValue().abs().toInt())
        assertEquals(-42, (-C_Int32.fromInt(42)).toInt())
    }

    @Test
    fun multiplicationWraps() {
        assertEquals(-200, (C_Int32.fromInt(10) * C_Int32.fromInt(-20)).toInt())
        assertEquals(200, (C_Int32.fromInt(-10) * C_Int32.fromInt(-20)).toInt())
        // 0x10000 * 0x10000 = 0x1_00000000 → low 32 bits = 0
        assertEquals(0, (C_Int32.fromInt(0x10000) * C_Int32.fromInt(0x10000)).toInt())
    }

    @Test
    fun divisionAndModulus() {
        assertEquals(-100 / 7, (C_Int32.fromInt(-100) / C_Int32.fromInt(7)).toInt())
        assertEquals(-100 % 7, (C_Int32.fromInt(-100) % C_Int32.fromInt(7)).toInt())
        assertEquals(100 / -7, (C_Int32.fromInt(100) / C_Int32.fromInt(-7)).toInt())
        assertEquals((-100) / (-7), (C_Int32.fromInt(-100) / C_Int32.fromInt(-7)).toInt())
        assertFailsWith<IllegalArgumentException> { C_Int32.fromInt(1) / C_Int32.zero() }
        assertFailsWith<IllegalArgumentException> { C_Int32.fromInt(1) % C_Int32.zero() }
    }

    @Test
    fun bitwiseTruthTables() {
        val a = C_Int32.fromInt(0x0F0F0F0F)
        val b = C_Int32.fromInt(0x00FF00FF)
        assertEquals(0x0F0F0F0F and 0x00FF00FF, (a and b).toInt())
        assertEquals(0x0F0F0F0F or 0x00FF00FF, (a or b).toInt())
        assertEquals(0x0F0F0F0F xor 0x00FF00FF, (a xor b).toInt())
        // ~0 = -1; double-inv identity
        assertEquals(-1, C_Int32.zero().inv().toInt())
        assertEquals(0xCAFEBABE.toInt(), C_Int32.fromInt(0xCAFEBABE.toInt()).inv().inv().toInt())
    }

    @Test
    fun shiftLeftAcrossWidth() {
        assertEquals(2, C_Int32.one().shiftLeft(1).toInt())
        assertEquals(0x40000000, C_Int32.one().shiftLeft(30).toInt())
        // Shift 1 → bit 31 → MIN_VALUE
        assertEquals(Int.MIN_VALUE, C_Int32.one().shiftLeft(31).toInt())
    }

    @Test
    fun arithmeticRightShiftSignExtends() {
        // -256 >> 2 = -64
        assertEquals(-64, C_Int32.fromInt(-256).shiftRight(2).toInt())
        // -1 >> 31 stays -1
        assertEquals(-1, C_Int32.fromInt(-1).shiftRight(31).toInt())
        // 256 >> 2 = 64
        assertEquals(64, C_Int32.fromInt(256).shiftRight(2).toInt())
        // identity
        assertEquals(-1234567, C_Int32.fromInt(-1234567).shiftRight(0).toInt())
    }

    @Test
    fun logicalRightShiftZeroFill() {
        // -1 ushr 1 → 0x7FFFFFFF
        assertEquals(0x7FFFFFFF, C_Int32.fromInt(-1).shiftRightUnsigned(1).toInt())
        // -256 ushr 8 → 0xFFFFFF00 ushr 8 = 0x00FFFFFF
        assertEquals(0x00FFFFFF, C_Int32.fromInt(-256).shiftRightUnsigned(8).toInt())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_Int32.one().shiftLeft(32) }
        assertFailsWith<IllegalArgumentException> { C_Int32.one().shiftRight(32) }
    }

    @Test
    fun comparisonAcrossSigns() {
        assertTrue(C_Int32.fromInt(-1) < C_Int32.zero())
        assertTrue(C_Int32.minValue() < C_Int32.maxValue())
        assertTrue(C_Int32.minValue() < C_Int32.fromInt(Int.MIN_VALUE + 1))
        assertEquals(0, C_Int32.fromInt(42).compareTo(C_Int32.fromInt(42)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_Int32.fromInt(-12345)
        val b = C_Int32.fromInt(-12345)
        assertEquals(a, b)
        assertNotEquals(a, C_Int32.fromInt(12345))
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals(-12345))
        assertFalse(a.equals(null))
    }

    @Test
    fun hexFormat() {
        assertEquals("0x00000000", C_Int32.zero().toHexString())
        assertEquals("0xffffffff", C_Int32.fromInt(-1).toHexString())
        assertEquals("0x80000000", C_Int32.minValue().toHexString())
        assertEquals("0x7fffffff", C_Int32.maxValue().toHexString())
    }
}

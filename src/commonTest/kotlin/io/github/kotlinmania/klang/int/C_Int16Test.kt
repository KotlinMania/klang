package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class C_Int16Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMinMax() {
        assertEquals(0, C_Int16.zero().toInt())
        assertEquals(1, C_Int16.one().toInt())
        assertEquals(Short.MIN_VALUE.toInt(), C_Int16.minValue().toInt())
        assertEquals(Short.MAX_VALUE.toInt(), C_Int16.maxValue().toInt())
    }

    @Test
    fun roundTripShort() {
        for (v in listOf<Short>(0, 1, -1, Short.MIN_VALUE, Short.MAX_VALUE, 12345, -12345)) {
            assertEquals(v, C_Int16.fromShort(v).toShort())
        }
    }

    @Test
    fun fromIntTruncates() {
        assertEquals(0, C_Int16.fromInt(0x10000).toInt())
        assertEquals(-1, C_Int16.fromInt(0xFFFF).toInt())
        assertEquals(Short.MIN_VALUE.toInt(), C_Int16.fromInt(0x8000).toInt())
    }

    @Test
    fun signDetection() {
        assertTrue(C_Int16.fromInt(-1).isNegative())
        assertTrue(C_Int16.minValue().isNegative())
        assertFalse(C_Int16.zero().isNegative())
        assertFalse(C_Int16.maxValue().isNegative())
    }

    @Test
    fun additionMixedSigns() {
        assertEquals(50, (C_Int16.fromInt(100) + C_Int16.fromInt(-50)).toInt())
        assertEquals(-50, (C_Int16.fromInt(-100) + C_Int16.fromInt(50)).toInt())
    }

    @Test
    fun additionWrapsAt16Bits() {
        assertEquals(Short.MIN_VALUE.toInt(), (C_Int16.maxValue() + C_Int16.one()).toInt())
        assertEquals(Short.MAX_VALUE.toInt(), (C_Int16.minValue() + C_Int16.fromInt(-1)).toInt())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(Short.MAX_VALUE.toInt(), (C_Int16.minValue() - C_Int16.one()).toInt())
    }

    @Test
    fun negationAndAbs() {
        assertEquals(-100, C_Int16.fromInt(100).negate().toInt())
        assertEquals(100, C_Int16.fromInt(-100).abs().toInt())
        assertEquals(Short.MAX_VALUE.toInt(), C_Int16.maxValue().abs().toInt())
        // -MIN wraps
        assertEquals(Short.MIN_VALUE.toInt(), C_Int16.minValue().negate().toInt())
        assertEquals(Short.MIN_VALUE.toInt(), C_Int16.minValue().abs().toInt())
        assertEquals(-42, (-C_Int16.fromInt(42)).toInt())
    }

    @Test
    fun multiplicationWraps() {
        assertEquals(-200, (C_Int16.fromInt(10) * C_Int16.fromInt(-20)).toInt())
        assertEquals(200, (C_Int16.fromInt(-10) * C_Int16.fromInt(-20)).toInt())
        // 256 * 256 = 65536 → wraps to 0
        assertEquals(0, (C_Int16.fromInt(256) * C_Int16.fromInt(256)).toInt())
    }

    @Test
    fun divisionAndModulus() {
        assertEquals(-100 / 7, (C_Int16.fromInt(-100) / C_Int16.fromInt(7)).toInt())
        assertEquals(-100 % 7, (C_Int16.fromInt(-100) % C_Int16.fromInt(7)).toInt())
        assertFailsWith<IllegalArgumentException> { C_Int16.fromInt(1) / C_Int16.zero() }
        assertFailsWith<IllegalArgumentException> { C_Int16.fromInt(1) % C_Int16.zero() }
    }

    @Test
    fun bitwiseTruthTables() {
        val a = C_Int16.fromInt(0x0F0F)
        val b = C_Int16.fromInt(0x00FF)
        assertEquals(0x000F, (a and b).toInt() and 0xFFFF)
        assertEquals(0x0FFF, (a or b).toInt() and 0xFFFF)
        assertEquals(0x0FF0, (a xor b).toInt() and 0xFFFF)
        // inv of all-zero = all-one = -1; double-inv is identity
        assertEquals(-1, C_Int16.zero().inv().toInt())
        assertEquals(C_Int16.fromInt(0x1234).toInt(), C_Int16.fromInt(0x1234).inv().inv().toInt())
    }

    @Test
    fun shiftLeftAcrossWidth() {
        assertEquals(2, C_Int16.one().shiftLeft(1).toInt())
        assertEquals(0x4000, C_Int16.one().shiftLeft(14).toInt())
        assertEquals(Short.MIN_VALUE.toInt(), C_Int16.one().shiftLeft(15).toInt())
    }

    @Test
    fun arithmeticRightShiftSignExtends() {
        // -256 >> 2 = -64
        assertEquals(-64, C_Int16.fromInt(-256).shiftRight(2).toInt())
        // -1 >> 15 stays -1
        assertEquals(-1, C_Int16.fromInt(-1).shiftRight(15).toInt())
        // 256 >> 2 = 64
        assertEquals(64, C_Int16.fromInt(256).shiftRight(2).toInt())
        // identity
        assertEquals(-42, C_Int16.fromInt(-42).shiftRight(0).toInt())
    }

    @Test
    fun logicalRightShiftZeroFill() {
        // -256 ushr 2 → 0xFF00 ushr 2 = 0x3FC0 = 16320
        assertEquals(0x3FC0, C_Int16.fromInt(-256).shiftRightUnsigned(2).toInt())
        // -1 ushr 15 → 0xFFFF ushr 15 = 1
        assertEquals(1, C_Int16.fromInt(-1).shiftRightUnsigned(15).toInt())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_Int16.one().shiftLeft(16) }
        assertFailsWith<IllegalArgumentException> { C_Int16.one().shiftRight(16) }
    }

    @Test
    fun comparison() {
        assertTrue(C_Int16.fromInt(-1) < C_Int16.zero())
        assertTrue(C_Int16.minValue() < C_Int16.maxValue())
        assertEquals(0, C_Int16.fromInt(42).compareTo(C_Int16.fromInt(42)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_Int16.fromInt(-12345)
        val b = C_Int16.fromInt(-12345)
        assertEquals(a, b)
        assertNotEquals(a, C_Int16.fromInt(12345))
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals(-12345))
        assertFalse(a.equals(null))
    }

    @Test
    fun hexFormat() {
        assertEquals("0x0000", C_Int16.zero().toHexString())
        assertEquals("0xffff", C_Int16.fromInt(-1).toHexString())
        assertEquals("0x8000", C_Int16.minValue().toHexString())
        assertEquals("0x7fff", C_Int16.maxValue().toHexString())
    }
}

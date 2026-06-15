package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class C_UInt64Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMax() {
        assertEquals(0uL, C_UInt64.zero().toULong())
        assertEquals(1uL, C_UInt64.one().toULong())
        assertEquals(ULong.MAX_VALUE, C_UInt64.maxValue().toULong())
    }

    @Test
    fun roundTripULong() {
        for (v in listOf(0uL, 1uL, 0x1234_5678_9ABC_DEF0uL, 0xDEAD_BEEF_CAFE_BABEuL, ULong.MAX_VALUE)) {
            assertEquals(v, C_UInt64.fromULong(v).toULong())
        }
    }

    @Test
    fun fromLongPreservesBitPattern() {
        assertEquals(ULong.MAX_VALUE, C_UInt64.fromLong(-1L).toULong())
        assertEquals(0x8000_0000_0000_0000uL, C_UInt64.fromLong(Long.MIN_VALUE).toULong())
    }

    @Test
    fun additionWraps() {
        assertEquals(0uL, (C_UInt64.maxValue() + C_UInt64.one()).toULong())
        assertEquals(ULong.MAX_VALUE - 1uL,
                     (C_UInt64.maxValue() + C_UInt64.maxValue()).toULong())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(ULong.MAX_VALUE, (C_UInt64.zero() - C_UInt64.one()).toULong())
        assertEquals(ULong.MAX_VALUE - 1uL, (C_UInt64.zero() - C_UInt64.fromULong(2uL)).toULong())
    }

    @Test
    fun multiplicationWraps() {
        // 2^32 * 2^32 = 2^64 → wraps to 0
        val a = C_UInt64.fromULong(0x1_0000_0000uL)
        assertEquals(0uL, (a * a).toULong())
        // MAX * MAX = (2^64 - 1)^2 → low 64 bits = 1
        assertEquals(1uL, (C_UInt64.maxValue() * C_UInt64.maxValue()).toULong())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt64.fromULong(1000uL)
        val b = C_UInt64.fromULong(7uL)
        assertEquals(142uL, (a / b).toULong())
        assertEquals(6uL, (a % b).toULong())
        // Large unsigned: MAX / 2 = floor((2^64-1)/2)
        assertEquals(0x7FFF_FFFF_FFFF_FFFFuL,
                     (C_UInt64.maxValue() / C_UInt64.fromULong(2uL)).toULong())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_UInt64.fromULong(10uL)
        assertFailsWith<IllegalArgumentException> { a / C_UInt64.zero() }
        assertFailsWith<IllegalArgumentException> { a % C_UInt64.zero() }
    }

    @Test
    fun bitwiseTruthTables() {
        val a = C_UInt64.fromULong(0xFF00_FF00_FF00_FF00uL)
        val b = C_UInt64.fromULong(0x00FF_00FF_00FF_00FFuL)
        assertEquals(0uL, (a and b).toULong())
        assertEquals(ULong.MAX_VALUE, (a or b).toULong())
        assertEquals(ULong.MAX_VALUE, (a xor b).toULong())
        assertEquals(0uL, (a xor a).toULong())
        assertEquals(b.toULong(), a.inv().toULong())
        assertEquals(ULong.MAX_VALUE, C_UInt64.zero().inv().toULong())
    }

    @Test
    fun shiftLeftAcrossWidth() {
        val one = C_UInt64.one()
        assertEquals(2uL, one.shiftLeft(1).toULong())
        assertEquals(0x1_0000_0000uL, one.shiftLeft(32).toULong())
        assertEquals(0x8000_0000_0000_0000uL, one.shiftLeft(63).toULong())
    }

    @Test
    fun shiftLeftWrapsHighBits() {
        assertEquals(ULong.MAX_VALUE - 1uL, C_UInt64.maxValue().shiftLeft(1).toULong())
        assertEquals(0uL, C_UInt64.fromULong(0x8000_0000_0000_0000uL).shiftLeft(1).toULong())
    }

    @Test
    fun shiftRightZeroFill() {
        assertEquals(0x4000_0000_0000_0000uL,
                     C_UInt64.fromULong(0x8000_0000_0000_0000uL).shiftRight(1).toULong())
        assertEquals(1uL, C_UInt64.fromULong(0x8000_0000_0000_0000uL).shiftRight(63).toULong())
        assertEquals(0x7FFF_FFFF_FFFF_FFFFuL, C_UInt64.maxValue().shiftRight(1).toULong())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_UInt64.one().shiftLeft(64) }
        assertFailsWith<IllegalArgumentException> { C_UInt64.one().shiftRight(64) }
    }

    @Test
    fun comparison() {
        assertTrue(C_UInt64.fromULong(100uL) < C_UInt64.maxValue())
        // 0x8000000000000000 (= Long.MIN_VALUE as signed) is still less than MAX as unsigned
        assertTrue(C_UInt64.fromULong(0x8000_0000_0000_0000uL) < C_UInt64.maxValue())
        assertEquals(0, C_UInt64.fromULong(0x1234uL).compareTo(C_UInt64.fromULong(0x1234uL)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_UInt64.fromULong(0xCAFE_BABE_DEAD_BEEFuL)
        val b = C_UInt64.fromULong(0xCAFE_BABE_DEAD_BEEFuL)
        assertEquals(a, b)
        assertNotEquals(a, C_UInt64.zero())
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals(0xCAFEBABEuL))
        assertFalse(a.equals(null))
    }

    @Test
    fun hexFormat() {
        assertEquals("0x0000000000000000", C_UInt64.zero().toHexString())
        assertEquals("0xdeadbeefcafebabe",
                     C_UInt64.fromULong(0xDEAD_BEEF_CAFE_BABEuL).toHexString())
        assertEquals("0xffffffffffffffff", C_UInt64.maxValue().toHexString())
    }
}

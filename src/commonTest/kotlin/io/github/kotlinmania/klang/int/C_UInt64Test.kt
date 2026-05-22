package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun roundTrip() {
        for (v in listOf(0uL, 1uL, 0xDEADBEEFCAFEBABEuL, ULong.MAX_VALUE)) {
            assertEquals(v, C_UInt64.fromULong(v).toULong())
        }
    }

    @Test
    fun additionWraps() {
        assertEquals(0uL, (C_UInt64.maxValue() + C_UInt64.one()).toULong())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(ULong.MAX_VALUE, (C_UInt64.zero() - C_UInt64.one()).toULong())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt64.fromULong(1000uL)
        val b = C_UInt64.fromULong(7uL)
        assertEquals(142uL, (a / b).toULong())
        assertEquals(6uL, (a % b).toULong())
        assertFailsWith<IllegalArgumentException> { a / C_UInt64.zero() }
    }

    @Test
    fun bitwise() {
        val a = C_UInt64.fromULong(0xFF00FF00FF00FF00uL)
        val b = C_UInt64.fromULong(0x00FF00FF00FF00FFuL)
        assertEquals(0uL, (a and b).toULong())
        assertEquals(ULong.MAX_VALUE, (a or b).toULong())
        assertEquals(ULong.MAX_VALUE, (a xor b).toULong())
        assertEquals(b.toULong(), a.inv().toULong())
    }

    @Test
    fun shifts() {
        assertEquals(0x8000_0000_0000_0000uL, C_UInt64.one().shiftLeft(63).toULong())
        assertEquals(1uL, C_UInt64.fromULong(0x8000_0000_0000_0000uL).shiftRight(63).toULong())
    }

    @Test
    fun comparison() {
        assertTrue(C_UInt64.fromULong(100uL) < C_UInt64.maxValue())
    }
}

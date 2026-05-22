package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class C_Int8Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMinMax() {
        assertEquals(0, C_Int8.zero().toInt())
        assertEquals(1, C_Int8.one().toInt())
        assertEquals(-128, C_Int8.minValue().toInt())
        assertEquals(127, C_Int8.maxValue().toInt())
    }

    @Test
    fun roundTrip() {
        for (v in listOf<Byte>(0, 1, -1, -128, 127, 42)) {
            assertEquals(v, C_Int8.fromByte(v).toByte())
        }
    }

    @Test
    fun additionWrapsAt8Bits() {
        // 127 + 1 = -128 (two's complement wrap)
        assertEquals(-128, (C_Int8.maxValue() + C_Int8.one()).toInt())
    }

    @Test
    fun negationAndAbs() {
        assertEquals(-100, C_Int8.fromInt(100).negate().toInt())
        assertEquals(100, C_Int8.fromInt(-100).abs().toInt())
    }

    @Test
    fun divisionAndModulus() {
        assertEquals(-100 / 7, (C_Int8.fromInt(-100) / C_Int8.fromInt(7)).toInt())
        assertEquals(-100 % 7, (C_Int8.fromInt(-100) % C_Int8.fromInt(7)).toInt())
        assertFailsWith<IllegalArgumentException> { C_Int8.fromInt(1) / C_Int8.zero() }
    }

    @Test
    fun arithmeticAndLogicalShifts() {
        val neg = C_Int8.fromInt(-64)
        assertEquals(-16, neg.shiftRight(2).toInt())
        // unsigned right shift: 0xC0 ushr 2 = 0x30 = 48
        assertEquals(48, neg.shiftRightUnsigned(2).toInt())
        assertEquals(2, C_Int8.one().shiftLeft(1).toInt())
    }

    @Test
    fun comparison() {
        assertTrue(C_Int8.fromInt(-1) < C_Int8.zero())
        assertTrue(C_Int8.minValue() < C_Int8.maxValue())
    }
}

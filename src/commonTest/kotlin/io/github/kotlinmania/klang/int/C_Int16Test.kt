package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun roundTrip() {
        for (v in listOf<Short>(0, 1, -1, Short.MIN_VALUE, Short.MAX_VALUE, 12345)) {
            assertEquals(v, C_Int16.fromShort(v).toShort())
        }
    }

    @Test
    fun additionWrapsAt16Bits() {
        // MAX + 1 wraps to MIN
        assertEquals(Short.MIN_VALUE.toInt(), (C_Int16.maxValue() + C_Int16.one()).toInt())
    }

    @Test
    fun negationAndAbs() {
        assertEquals(-100, C_Int16.fromInt(100).negate().toInt())
        assertEquals(100, C_Int16.fromInt(-100).abs().toInt())
    }

    @Test
    fun divisionAndModulus() {
        assertEquals(-100 / 7, (C_Int16.fromInt(-100) / C_Int16.fromInt(7)).toInt())
        assertEquals(-100 % 7, (C_Int16.fromInt(-100) % C_Int16.fromInt(7)).toInt())
        assertFailsWith<IllegalArgumentException> { C_Int16.fromInt(1) / C_Int16.zero() }
    }

    @Test
    fun arithmeticAndLogicalShifts() {
        val neg = C_Int16.fromInt(-256)
        assertEquals(-64, neg.shiftRight(2).toInt())
        // Logical right shift treats value as unsigned: 0xFF00 ushr 2 = 0x3FC0
        assertEquals(0x3FC0, neg.shiftRightUnsigned(2).toInt())
        assertEquals(2, C_Int16.one().shiftLeft(1).toInt())
    }

    @Test
    fun comparison() {
        assertTrue(C_Int16.fromInt(-1) < C_Int16.zero())
        assertTrue(C_Int16.minValue() < C_Int16.maxValue())
    }
}

package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun roundTrip() {
        for (v in listOf(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x7FFF_FFFF_FFFF_FFFFL)) {
            assertEquals(v, C_Int64.fromLong(v).toLong())
        }
    }

    @Test
    fun signedArithmetic() {
        assertEquals(50L, (C_Int64.fromLong(100L) + C_Int64.fromLong(-50L)).toLong())
        assertEquals(150L, (C_Int64.fromLong(100L) - C_Int64.fromLong(-50L)).toLong())
        assertEquals(-200L, (C_Int64.fromLong(10L) * C_Int64.fromLong(-20L)).toLong())
    }

    @Test
    fun additionWraps() {
        assertEquals(Long.MIN_VALUE, (C_Int64.maxValue() + C_Int64.one()).toLong())
    }

    @Test
    fun negationAndAbs() {
        assertEquals(-100L, C_Int64.fromLong(100L).negate().toLong())
        assertEquals(100L, C_Int64.fromLong(-100L).abs().toLong())
        // -MIN_VALUE wraps
        assertEquals(Long.MIN_VALUE, C_Int64.minValue().negate().toLong())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_Int64.fromLong(-1000L)
        val b = C_Int64.fromLong(7L)
        assertEquals(-1000L / 7L, (a / b).toLong())
        assertEquals(-1000L % 7L, (a % b).toLong())
        assertFailsWith<IllegalArgumentException> { a / C_Int64.zero() }
    }

    @Test
    fun arithmeticAndLogicalShifts() {
        val neg = C_Int64.fromLong(-256L)
        assertEquals(-64L, neg.shiftRight(2).toLong())
        assertEquals(-256L ushr 2, neg.shiftRightUnsigned(2).toLong())
        assertEquals(2L, C_Int64.one().shiftLeft(1).toLong())
    }

    @Test
    fun comparison() {
        assertTrue(C_Int64.fromLong(-1L) < C_Int64.zero())
        assertTrue(C_Int64.minValue() < C_Int64.maxValue())
    }
}

package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class C_Int32Test {

    @BeforeTest
    fun setup() {
        KMalloc.init(64 * 1024)
    }

    @Test
    fun zeroOneMinMax() {
        assertEquals(0, C_Int32.zero().toInt())
        assertEquals(1, C_Int32.one().toInt())
        assertEquals(Int.MIN_VALUE, C_Int32.minValue().toInt())
        assertEquals(Int.MAX_VALUE, C_Int32.maxValue().toInt())
    }

    @Test
    fun fromIntRoundTrip() {
        for (v in listOf(0, 1, -1, 42, -42, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertEquals(v, C_Int32.fromInt(v).toInt())
        }
    }

    @Test
    fun signDetection() {
        assertTrue(C_Int32.fromInt(-1).isNegative())
        assertFalse(C_Int32.zero().isNegative())
        assertFalse(C_Int32.fromInt(1).isNegative())
        assertTrue(C_Int32.minValue().isNegative())
    }

    @Test
    fun additionPositiveAndNegative() {
        assertEquals(50, (C_Int32.fromInt(100) + C_Int32.fromInt(-50)).toInt())
        assertEquals(-50, (C_Int32.fromInt(-100) + C_Int32.fromInt(50)).toInt())
    }

    @Test
    fun additionWrapsOnOverflow() {
        // Int.MAX_VALUE + 1 wraps to Int.MIN_VALUE in two's complement
        assertEquals(Int.MIN_VALUE, (C_Int32.maxValue() + C_Int32.one()).toInt())
    }

    @Test
    fun subtraction() {
        assertEquals(150, (C_Int32.fromInt(100) - C_Int32.fromInt(-50)).toInt())
    }

    @Test
    fun negation() {
        assertEquals(-100, C_Int32.fromInt(100).negate().toInt())
        assertEquals(100, C_Int32.fromInt(-100).negate().toInt())
        // -MIN_VALUE wraps back to MIN_VALUE (two's complement)
        assertEquals(Int.MIN_VALUE, C_Int32.minValue().negate().toInt())
    }

    @Test
    fun unaryMinusOperator() {
        assertEquals(-42, (-C_Int32.fromInt(42)).toInt())
    }

    @Test
    fun absoluteValue() {
        assertEquals(100, C_Int32.fromInt(-100).abs().toInt())
        assertEquals(100, C_Int32.fromInt(100).abs().toInt())
    }

    @Test
    fun multiplication() {
        assertEquals(-200, (C_Int32.fromInt(10) * C_Int32.fromInt(-20)).toInt())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_Int32.fromInt(-100)
        val b = C_Int32.fromInt(7)
        assertEquals(-100 / 7, (a / b).toInt())
        assertEquals(-100 % 7, (a % b).toInt())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_Int32.fromInt(10)
        val zero = C_Int32.zero()
        assertFailsWith<IllegalArgumentException> { a / zero }
        assertFailsWith<IllegalArgumentException> { a % zero }
    }

    @Test
    fun bitwiseOps() {
        val a = C_Int32.fromInt(0x0F0F0F0F)
        val b = C_Int32.fromInt(0x00FF00FF)
        assertEquals(0x0F0F0F0F and 0x00FF00FF, (a and b).toInt())
        assertEquals(0x0F0F0F0F or 0x00FF00FF, (a or b).toInt())
        assertEquals(0x0F0F0F0F xor 0x00FF00FF, (a xor b).toInt())
        assertEquals((0x0F0F0F0F).inv(), a.inv().toInt())
    }

    @Test
    fun arithmeticAndLogicalShifts() {
        val neg = C_Int32.fromInt(-256)
        // Arithmetic right shift preserves sign
        assertEquals(-64, neg.shiftRight(2).toInt())
        // Logical right shift fills zero
        assertEquals((-256).ushr(2), neg.shiftRightUnsigned(2).toInt())
        // Left shift
        assertEquals(2, C_Int32.one().shiftLeft(1).toInt())
    }

    @Test
    fun comparison() {
        assertTrue(C_Int32.fromInt(-1) < C_Int32.zero())
        assertTrue(C_Int32.minValue() < C_Int32.maxValue())
        assertEquals(0, C_Int32.fromInt(42).compareTo(C_Int32.fromInt(42)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_Int32.fromInt(-12345)
        val b = C_Int32.fromInt(-12345)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}

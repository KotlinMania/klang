package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class C_UInt32Test {

    @BeforeTest
    fun setup() {
        KMalloc.init(64 * 1024)
    }

    @Test
    fun zeroAndOne() {
        assertEquals(0u, C_UInt32.zero().toUInt())
        assertEquals(1u, C_UInt32.one().toUInt())
        assertEquals(UInt.MAX_VALUE, C_UInt32.maxValue().toUInt())
    }

    @Test
    fun fromUIntRoundTrip() {
        for (v in listOf(0u, 1u, 42u, 0xDEADBEEFu, 0xFFFFFFFFu, UInt.MAX_VALUE)) {
            assertEquals(v, C_UInt32.fromUInt(v).toUInt())
        }
    }

    @Test
    fun addition() {
        val a = C_UInt32.fromUInt(100u)
        val b = C_UInt32.fromUInt(250u)
        assertEquals(350u, (a + b).toUInt())
    }

    @Test
    fun additionWrapsOnOverflow() {
        val max = C_UInt32.maxValue()
        val one = C_UInt32.one()
        assertEquals(0u, (max + one).toUInt())
    }

    @Test
    fun subtractionWrapsBelowZero() {
        val zero = C_UInt32.zero()
        val one = C_UInt32.one()
        assertEquals(UInt.MAX_VALUE, (zero - one).toUInt())
    }

    @Test
    fun multiplicationWraps() {
        val a = C_UInt32.fromUInt(0x10000u) // 2^16
        val b = C_UInt32.fromUInt(0x10000u)
        // 2^32 wraps to 0
        assertEquals(0u, (a * b).toUInt())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt32.fromUInt(100u)
        val b = C_UInt32.fromUInt(7u)
        assertEquals(14u, (a / b).toUInt())
        assertEquals(2u, (a % b).toUInt())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_UInt32.fromUInt(10u)
        val zero = C_UInt32.zero()
        assertFailsWith<IllegalArgumentException> { a / zero }
        assertFailsWith<IllegalArgumentException> { a % zero }
    }

    @Test
    fun bitwiseOps() {
        val a = C_UInt32.fromUInt(0xFF00FF00u)
        val b = C_UInt32.fromUInt(0x00FF00FFu)
        assertEquals(0u, (a and b).toUInt())
        assertEquals(0xFFFFFFFFu, (a or b).toUInt())
        assertEquals(0xFFFFFFFFu, (a xor b).toUInt())
        assertEquals(0x00FF00FFu, a.inv().toUInt())
    }

    @Test
    fun shifts() {
        val v = C_UInt32.fromUInt(0x00000001u)
        assertEquals(0x80000000u, v.shiftLeft(31).toUInt())
        val high = C_UInt32.fromUInt(0x80000000u)
        assertEquals(0x00000001u, high.shiftRight(31).toUInt())
    }

    @Test
    fun comparison() {
        val small = C_UInt32.fromUInt(100u)
        val big = C_UInt32.fromUInt(UInt.MAX_VALUE)
        assertTrue(small < big)
        assertFalse(big < small)
        assertEquals(0, C_UInt32.fromUInt(42u).compareTo(C_UInt32.fromUInt(42u)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_UInt32.fromUInt(0xCAFEBABEu)
        val b = C_UInt32.fromUInt(0xCAFEBABEu)
        val c = C_UInt32.fromUInt(0xDEADBEEFu)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a != c)
    }

    @Test
    fun hexFormat() {
        assertEquals("0x00000000", C_UInt32.zero().toHexString())
        assertEquals("0xdeadbeef", C_UInt32.fromUInt(0xDEADBEEFu).toHexString())
        assertEquals("0xffffffff", C_UInt32.maxValue().toHexString())
    }
}

package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class C_UInt32Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMax() {
        assertEquals(0u, C_UInt32.zero().toUInt())
        assertEquals(1u, C_UInt32.one().toUInt())
        assertEquals(UInt.MAX_VALUE, C_UInt32.maxValue().toUInt())
    }

    @Test
    fun roundTripUInt() {
        for (v in listOf(0u, 1u, 0x12345678u, 0xCAFEBABEu, 0xDEADBEEFu, UInt.MAX_VALUE)) {
            assertEquals(v, C_UInt32.fromUInt(v).toUInt())
        }
    }

    @Test
    fun fromIntPreservesBitPattern() {
        // -1 → 0xFFFFFFFF as unsigned
        assertEquals(UInt.MAX_VALUE, C_UInt32.fromInt(-1).toUInt())
        assertEquals(0x80000000u, C_UInt32.fromInt(Int.MIN_VALUE).toUInt())
    }

    @Test
    fun additionWraps() {
        assertEquals(0u, (C_UInt32.maxValue() + C_UInt32.one()).toUInt())
        assertEquals(UInt.MAX_VALUE - 1u, (C_UInt32.maxValue() + C_UInt32.maxValue()).toUInt())
        assertEquals(UInt.MAX_VALUE,
                     (C_UInt32.fromUInt(0xFFFFFFFFu) + C_UInt32.zero()).toUInt())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(UInt.MAX_VALUE, (C_UInt32.zero() - C_UInt32.one()).toUInt())
        assertEquals(UInt.MAX_VALUE - 1u, (C_UInt32.zero() - C_UInt32.fromUInt(2u)).toUInt())
    }

    @Test
    fun multiplicationWraps() {
        val a = C_UInt32.fromUInt(0x10000u)  // 2^16
        assertEquals(0u, (a * a).toUInt())
        // MAX * MAX = (2^32 - 1)^2 → low 32 bits = 1
        assertEquals(1u, (C_UInt32.maxValue() * C_UInt32.maxValue()).toUInt())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt32.fromUInt(100u)
        val b = C_UInt32.fromUInt(7u)
        assertEquals(14u, (a / b).toUInt())
        assertEquals(2u, (a % b).toUInt())
        // Large unsigned division
        assertEquals(0x10000u, (C_UInt32.fromUInt(0x10000000u) / C_UInt32.fromUInt(0x1000u)).toUInt())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_UInt32.fromUInt(10u)
        assertFailsWith<IllegalArgumentException> { a / C_UInt32.zero() }
        assertFailsWith<IllegalArgumentException> { a % C_UInt32.zero() }
    }

    @Test
    fun bitwiseTruthTables() {
        val a = C_UInt32.fromUInt(0xFF00FF00u)
        val b = C_UInt32.fromUInt(0x00FF00FFu)
        assertEquals(0u, (a and b).toUInt())
        assertEquals(0xFFFFFFFFu, (a or b).toUInt())
        assertEquals(0xFFFFFFFFu, (a xor b).toUInt())
        assertEquals(0u, (a xor a).toUInt())
        assertEquals(b.toUInt(), a.inv().toUInt())
        assertEquals(UInt.MAX_VALUE, C_UInt32.zero().inv().toUInt())
    }

    @Test
    fun shiftLeftAcrossWidth() {
        val one = C_UInt32.one()
        assertEquals(2u, one.shiftLeft(1).toUInt())
        assertEquals(0x10000u, one.shiftLeft(16).toUInt())
        assertEquals(0x80000000u, one.shiftLeft(31).toUInt())
        assertEquals(0xCAFEBABEu, C_UInt32.fromUInt(0xCAFEBABEu).shiftLeft(0).toUInt())
    }

    @Test
    fun shiftLeftWrapsHighBits() {
        assertEquals(0xFFFFFFFEu, C_UInt32.maxValue().shiftLeft(1).toUInt())
        assertEquals(0u, C_UInt32.fromUInt(0x80000000u).shiftLeft(1).toUInt())
    }

    @Test
    fun shiftRightZeroFill() {
        assertEquals(0x40000000u, C_UInt32.fromUInt(0x80000000u).shiftRight(1).toUInt())
        assertEquals(1u, C_UInt32.fromUInt(0x80000000u).shiftRight(31).toUInt())
        assertEquals(0x7FFFFFFFu, C_UInt32.fromUInt(0xFFFFFFFFu).shiftRight(1).toUInt())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_UInt32.one().shiftLeft(32) }
        assertFailsWith<IllegalArgumentException> { C_UInt32.one().shiftRight(32) }
    }

    @Test
    fun comparison() {
        assertTrue(C_UInt32.fromUInt(100u) < C_UInt32.maxValue())
        // 0x80000000u (= Int.MIN_VALUE if reinterpreted as signed) is still less than MAX
        assertTrue(C_UInt32.fromUInt(0x80000000u) < C_UInt32.maxValue())
        assertEquals(0, C_UInt32.fromUInt(42u).compareTo(C_UInt32.fromUInt(42u)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_UInt32.fromUInt(0xCAFEBABEu)
        val b = C_UInt32.fromUInt(0xCAFEBABEu)
        assertEquals(a, b)
        assertNotEquals(a, C_UInt32.fromUInt(0xDEADBEEFu))
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals(0xCAFEBABEu))
        assertFalse(a.equals(null))
    }

    @Test
    fun hexFormat() {
        assertEquals("0x00000000", C_UInt32.zero().toHexString())
        assertEquals("0x0000000a", C_UInt32.fromUInt(10u).toHexString())
        assertEquals("0xdeadbeef", C_UInt32.fromUInt(0xDEADBEEFu).toHexString())
        assertEquals("0xffffffff", C_UInt32.maxValue().toHexString())
    }
}

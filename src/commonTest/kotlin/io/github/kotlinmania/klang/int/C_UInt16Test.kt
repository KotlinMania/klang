package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class C_UInt16Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMax() {
        assertEquals(0u, C_UInt16.zero().toUInt())
        assertEquals(1u, C_UInt16.one().toUInt())
        assertEquals(0xFFFFu, C_UInt16.maxValue().toUInt())
    }

    @Test
    fun roundTripUShort() {
        for (v in listOf<UShort>(0u, 1u, 0x1234u, 0xCAFEu, 0xDEADu, 0xFFFFu)) {
            assertEquals(v, C_UInt16.fromUShort(v).toUShort())
        }
    }

    @Test
    fun fromUIntTruncatesToLowShort() {
        assertEquals(0u, C_UInt16.fromUInt(0x10000u).toUInt())
        assertEquals(0xBEEFu, C_UInt16.fromUInt(0xDEADBEEFu).toUInt())
        assertEquals(0xFFFFu, C_UInt16.fromUInt(UInt.MAX_VALUE).toUInt())
    }

    @Test
    fun additionWrapsAt16Bits() {
        assertEquals(0u, (C_UInt16.maxValue() + C_UInt16.one()).toUInt())
        assertEquals(0x100u, (C_UInt16.fromUInt(0xFF00u) + C_UInt16.fromUInt(0x200u)).toUInt())
        assertEquals(0xFFFEu, (C_UInt16.maxValue() + C_UInt16.maxValue()).toUInt())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(0xFFFFu, (C_UInt16.zero() - C_UInt16.one()).toUInt())
        assertEquals(0xFF00u, (C_UInt16.zero() - C_UInt16.fromUInt(0x100u)).toUInt())
    }

    @Test
    fun multiplicationWraps() {
        val a = C_UInt16.fromUInt(0x100u)
        assertEquals(0u, (a * a).toUInt())
        // 0xFFFF * 0xFFFF = 0xFFFE0001 → wraps to 0x0001
        assertEquals(1u, (C_UInt16.maxValue() * C_UInt16.maxValue()).toUInt())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt16.fromUInt(1000u)
        val b = C_UInt16.fromUInt(7u)
        assertEquals(142u, (a / b).toUInt())
        assertEquals(6u, (a % b).toUInt())
        assertEquals(0xCAFEu, (C_UInt16.fromUInt(0xCAFEu) / C_UInt16.one()).toUInt())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_UInt16.fromUInt(10u)
        assertFailsWith<IllegalArgumentException> { a / C_UInt16.zero() }
        assertFailsWith<IllegalArgumentException> { a % C_UInt16.zero() }
    }

    @Test
    fun bitwiseTruthTables() {
        val a = C_UInt16.fromUInt(0xFF00u)
        val b = C_UInt16.fromUInt(0x00FFu)
        assertEquals(0u, (a and b).toUInt())
        assertEquals(0xFFFFu, (a or b).toUInt())
        assertEquals(0xFFFFu, (a xor b).toUInt())
        assertEquals(0u, (a xor a).toUInt())
        assertEquals(0x00FFu, a.inv().toUInt())
        assertEquals(0xFFFFu, C_UInt16.zero().inv().toUInt())
    }

    @Test
    fun shiftLeftAcrossWidth() {
        val one = C_UInt16.one()
        assertEquals(0x02u, one.shiftLeft(1).toUInt())
        assertEquals(0x8000u, one.shiftLeft(15).toUInt())
        assertEquals(0xCAFEu, C_UInt16.fromUInt(0xCAFEu).shiftLeft(0).toUInt())
    }

    @Test
    fun shiftLeftWrapsHighBits() {
        assertEquals(0xFFFEu, C_UInt16.maxValue().shiftLeft(1).toUInt())
        assertEquals(0u, C_UInt16.fromUInt(0x8000u).shiftLeft(1).toUInt())
    }

    @Test
    fun shiftRightZeroFill() {
        assertEquals(0x4000u, C_UInt16.fromUInt(0x8000u).shiftRight(1).toUInt())
        assertEquals(1u, C_UInt16.fromUInt(0x8000u).shiftRight(15).toUInt())
        assertEquals(0x7FFFu, C_UInt16.fromUInt(0xFFFFu).shiftRight(1).toUInt())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_UInt16.one().shiftLeft(16) }
        assertFailsWith<IllegalArgumentException> { C_UInt16.one().shiftRight(16) }
    }

    @Test
    fun comparison() {
        assertTrue(C_UInt16.fromUInt(100u) < C_UInt16.maxValue())
        assertTrue(C_UInt16.zero() < C_UInt16.one())
        assertEquals(0, C_UInt16.fromUInt(0x1234u).compareTo(C_UInt16.fromUInt(0x1234u)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_UInt16.fromUInt(0xABCDu)
        val b = C_UInt16.fromUInt(0xABCDu)
        assertEquals(a, b)
        assertNotEquals(a, C_UInt16.fromUInt(0xABCEu))
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals("0xABCD"))
        assertFalse(a.equals(null))
    }

    @Test
    fun hexFormat() {
        assertEquals("0x0000", C_UInt16.zero().toHexString())
        assertEquals("0x00ab", C_UInt16.fromUInt(0xABu).toHexString())
        assertEquals("0xcafe", C_UInt16.fromUInt(0xCAFEu).toHexString())
        assertEquals("0xffff", C_UInt16.maxValue().toHexString())
    }
}

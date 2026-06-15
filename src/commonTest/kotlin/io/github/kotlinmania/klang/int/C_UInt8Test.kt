package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Comprehensive coverage for [C_UInt8].
 *
 * Validates wrap-on-overflow at the 8-bit boundary, divide-by-zero behavior,
 * bitwise truth tables, and shifts spanning the full 0..7 range.
 */
class C_UInt8Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    // -- Construction ---------------------------------------------------------

    @Test
    fun zeroOneMax() {
        assertEquals(0u, C_UInt8.zero().toUInt())
        assertEquals(1u, C_UInt8.one().toUInt())
        assertEquals(0xFFu, C_UInt8.maxValue().toUInt())
    }

    @Test
    fun roundTripUByte() {
        for (v in listOf<UByte>(0u, 1u, 0x55u, 0xAAu, 0xCAu, 0xFFu)) {
            assertEquals(v, C_UInt8.fromUByte(v).toUByte())
        }
    }

    @Test
    fun fromUIntTruncatesToLowByte() {
        assertEquals(0u, C_UInt8.fromUInt(0x100u).toUInt())
        assertEquals(0x42u, C_UInt8.fromUInt(0xCAFEBA42u).toUInt())
        assertEquals(0xFFu, C_UInt8.fromUInt(UInt.MAX_VALUE).toUInt())
    }

    // -- Addition / subtraction wrap -----------------------------------------

    @Test
    fun additionInRange() {
        assertEquals(150u, (C_UInt8.fromUInt(50u) + C_UInt8.fromUInt(100u)).toUInt())
        assertEquals(0xFFu, (C_UInt8.fromUInt(0xFEu) + C_UInt8.one()).toUInt())
    }

    @Test
    fun additionWrapsAt8Bits() {
        assertEquals(0u, (C_UInt8.maxValue() + C_UInt8.one()).toUInt())
        assertEquals(0x10u, (C_UInt8.fromUInt(0xF0u) + C_UInt8.fromUInt(0x20u)).toUInt())
        // Two MAX values: 255 + 255 = 510 → wraps to 254
        assertEquals(0xFEu, (C_UInt8.maxValue() + C_UInt8.maxValue()).toUInt())
    }

    @Test
    fun subtractionInRange() {
        assertEquals(50u, (C_UInt8.fromUInt(100u) - C_UInt8.fromUInt(50u)).toUInt())
    }

    @Test
    fun subtractionWrapsBelowZero() {
        assertEquals(0xFFu, (C_UInt8.zero() - C_UInt8.one()).toUInt())
        assertEquals(0xFEu, (C_UInt8.zero() - C_UInt8.fromUInt(2u)).toUInt())
        assertEquals(0xFFu, (C_UInt8.one() - C_UInt8.fromUInt(2u)).toUInt())
    }

    // -- Multiplication ------------------------------------------------------

    @Test
    fun multiplicationInRange() {
        assertEquals(0x40u, (C_UInt8.fromUInt(0x10u) * C_UInt8.fromUInt(4u)).toUInt())
        assertEquals(0xFFu, (C_UInt8.maxValue() * C_UInt8.one()).toUInt())
        assertEquals(0u, (C_UInt8.maxValue() * C_UInt8.zero()).toUInt())
    }

    @Test
    fun multiplicationWraps() {
        // 0x10 * 0x10 = 0x100 → wraps to 0
        assertEquals(0u, (C_UInt8.fromUInt(0x10u) * C_UInt8.fromUInt(0x10u)).toUInt())
        // 0xFF * 0xFF = 0xFE01 → wraps to 0x01
        assertEquals(1u, (C_UInt8.maxValue() * C_UInt8.maxValue()).toUInt())
    }

    // -- Division / modulus --------------------------------------------------

    @Test
    fun divisionAndModulus() {
        val a = C_UInt8.fromUInt(100u)
        val b = C_UInt8.fromUInt(7u)
        assertEquals(14u, (a / b).toUInt())
        assertEquals(2u, (a % b).toUInt())
    }

    @Test
    fun divisionByOneIsIdentity() {
        assertEquals(0xCDu, (C_UInt8.fromUInt(0xCDu) / C_UInt8.one()).toUInt())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_UInt8.fromUInt(10u)
        assertFailsWith<IllegalArgumentException> { a / C_UInt8.zero() }
        assertFailsWith<IllegalArgumentException> { a % C_UInt8.zero() }
    }

    // -- Bitwise -------------------------------------------------------------

    @Test
    fun bitwiseAndTruthTable() {
        val a = C_UInt8.fromUInt(0xF0u)
        val b = C_UInt8.fromUInt(0x0Fu)
        assertEquals(0u, (a and b).toUInt())
        assertEquals(0xF0u, (a and a).toUInt())
        assertEquals(0u, (a and C_UInt8.zero()).toUInt())
        assertEquals(0xF0u, (a and C_UInt8.maxValue()).toUInt())
    }

    @Test
    fun bitwiseOrTruthTable() {
        val a = C_UInt8.fromUInt(0xF0u)
        val b = C_UInt8.fromUInt(0x0Fu)
        assertEquals(0xFFu, (a or b).toUInt())
        assertEquals(0xF0u, (a or a).toUInt())
        assertEquals(0xF0u, (a or C_UInt8.zero()).toUInt())
        assertEquals(0xFFu, (a or C_UInt8.maxValue()).toUInt())
    }

    @Test
    fun bitwiseXorTruthTable() {
        val a = C_UInt8.fromUInt(0xF0u)
        val b = C_UInt8.fromUInt(0x0Fu)
        assertEquals(0xFFu, (a xor b).toUInt())
        assertEquals(0u, (a xor a).toUInt())
        assertEquals(0xF0u, (a xor C_UInt8.zero()).toUInt())
        assertEquals(0x0Fu, (a xor C_UInt8.maxValue()).toUInt())
    }

    @Test
    fun bitwiseInv() {
        assertEquals(0xFFu, C_UInt8.zero().inv().toUInt())
        assertEquals(0u, C_UInt8.maxValue().inv().toUInt())
        assertEquals(0x55u, C_UInt8.fromUInt(0xAAu).inv().toUInt())
        // Double-inv is identity
        assertEquals(0xCDu, C_UInt8.fromUInt(0xCDu).inv().inv().toUInt())
    }

    // -- Shifts --------------------------------------------------------------

    @Test
    fun shiftLeftAcrossWidth() {
        val one = C_UInt8.one()
        assertEquals(0x02u, one.shiftLeft(1).toUInt())
        assertEquals(0x40u, one.shiftLeft(6).toUInt())
        assertEquals(0x80u, one.shiftLeft(7).toUInt())
        // Shift by 0 is identity
        assertEquals(0xCDu, C_UInt8.fromUInt(0xCDu).shiftLeft(0).toUInt())
    }

    @Test
    fun shiftLeftWrapsHighBits() {
        // 0xFF << 1 = 0x1FE → drops bit 8 → 0xFE
        assertEquals(0xFEu, C_UInt8.maxValue().shiftLeft(1).toUInt())
        // 0x80 << 1 = 0x100 → wraps to 0
        assertEquals(0u, C_UInt8.fromUInt(0x80u).shiftLeft(1).toUInt())
    }

    @Test
    fun shiftRightZeroFill() {
        assertEquals(0x40u, C_UInt8.fromUInt(0x80u).shiftRight(1).toUInt())
        assertEquals(1u, C_UInt8.fromUInt(0x80u).shiftRight(7).toUInt())
        // High bit zero-fills (logical/unsigned semantics for the unsigned type)
        assertEquals(0x7Fu, C_UInt8.fromUInt(0xFFu).shiftRight(1).toUInt())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_UInt8.one().shiftLeft(-1) }
        assertFailsWith<IllegalArgumentException> { C_UInt8.one().shiftLeft(8) }
        assertFailsWith<IllegalArgumentException> { C_UInt8.one().shiftRight(8) }
    }

    // -- Comparison / equality -----------------------------------------------

    @Test
    fun comparisonOrdering() {
        assertTrue(C_UInt8.zero() < C_UInt8.one())
        assertTrue(C_UInt8.fromUInt(0x80u) < C_UInt8.fromUInt(0x81u))
        assertTrue(C_UInt8.maxValue() > C_UInt8.zero())
        assertEquals(0, C_UInt8.fromUInt(42u).compareTo(C_UInt8.fromUInt(42u)))
    }

    @Test
    fun equalsAndHashCode() {
        val a = C_UInt8.fromUInt(0xABu)
        val b = C_UInt8.fromUInt(0xABu)
        val c = C_UInt8.fromUInt(0xACu)
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equalsAgainstNonType() {
        assertFalse(C_UInt8.zero().equals("not a C_UInt8"))
        assertFalse(C_UInt8.zero().equals(null))
    }

    // -- Hex formatting ------------------------------------------------------

    @Test
    fun hexFormat() {
        assertEquals("0x00", C_UInt8.zero().toHexString())
        assertEquals("0x01", C_UInt8.one().toHexString())
        assertEquals("0xab", C_UInt8.fromUInt(0xABu).toHexString())
        assertEquals("0xff", C_UInt8.maxValue().toHexString())
    }

    // -- copy --------------------------------------------------------------

    @Test
    fun copyIsIndependent() {
        val a = C_UInt8.fromUInt(0x42u)
        val b = a.copy()
        assertEquals(a.toUInt(), b.toUInt())
        // Distinct heap allocations
        assertTrue(a.addr != b.addr)
    }
}

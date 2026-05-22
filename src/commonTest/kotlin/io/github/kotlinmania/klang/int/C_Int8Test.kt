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
 * Comprehensive coverage for [C_Int8].
 *
 * Validates two's-complement wrap, sign-extension, sign-bit-boundary shifts,
 * arithmetic vs logical right shift, MIN_VALUE edge cases, and bitwise ops.
 */
class C_Int8Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    // -- Construction --------------------------------------------------------

    @Test
    fun zeroOneMinMax() {
        assertEquals(0, C_Int8.zero().toInt())
        assertEquals(1, C_Int8.one().toInt())
        assertEquals(-128, C_Int8.minValue().toInt())
        assertEquals(127, C_Int8.maxValue().toInt())
    }

    @Test
    fun roundTripByte() {
        for (v in listOf<Byte>(0, 1, -1, -128, 127, 42, -42)) {
            assertEquals(v, C_Int8.fromByte(v).toByte())
        }
    }

    @Test
    fun fromIntTruncatesToLowByte() {
        // 0x100 → 0, 0x1FF → -1
        assertEquals(0, C_Int8.fromInt(0x100).toInt())
        assertEquals(-1, C_Int8.fromInt(0xFF).toInt())
        // High-bit positive Int → negative Byte
        assertEquals(-128, C_Int8.fromInt(0x80).toInt())
    }

    @Test
    fun signDetection() {
        assertTrue(C_Int8.fromInt(-1).isNegative())
        assertTrue(C_Int8.minValue().isNegative())
        assertFalse(C_Int8.zero().isNegative())
        assertFalse(C_Int8.fromInt(1).isNegative())
        assertFalse(C_Int8.maxValue().isNegative())
    }

    // -- Addition ------------------------------------------------------------

    @Test
    fun additionMixedSigns() {
        assertEquals(50, (C_Int8.fromInt(100) + C_Int8.fromInt(-50)).toInt())
        assertEquals(-50, (C_Int8.fromInt(-100) + C_Int8.fromInt(50)).toInt())
        assertEquals(0, (C_Int8.fromInt(-50) + C_Int8.fromInt(50)).toInt())
    }

    @Test
    fun additionWrapsAt8Bits() {
        // 127 + 1 → -128 (sign flip via two's complement)
        assertEquals(-128, (C_Int8.maxValue() + C_Int8.one()).toInt())
        // -128 + -1 → 127
        assertEquals(127, (C_Int8.minValue() + C_Int8.fromInt(-1)).toInt())
        // 127 + 127 = 254 → wraps to -2
        assertEquals(-2, (C_Int8.maxValue() + C_Int8.maxValue()).toInt())
    }

    // -- Subtraction ---------------------------------------------------------

    @Test
    fun subtractionMixedSigns() {
        assertEquals(150 - 256, (C_Int8.fromInt(100) - C_Int8.fromInt(-50)).toInt())
        assertEquals(-50, (C_Int8.fromInt(0) - C_Int8.fromInt(50)).toInt())
    }

    @Test
    fun subtractionWrapsAt8Bits() {
        // -128 - 1 → 127
        assertEquals(127, (C_Int8.minValue() - C_Int8.one()).toInt())
    }

    // -- Negation / abs ------------------------------------------------------

    @Test
    fun negation() {
        assertEquals(-100, C_Int8.fromInt(100).negate().toInt())
        assertEquals(100, C_Int8.fromInt(-100).negate().toInt())
        // Unary minus operator
        assertEquals(-42, (-C_Int8.fromInt(42)).toInt())
    }

    @Test
    fun negationOfMinValueWraps() {
        // -(-128) doesn't fit in 8-bit signed, wraps to -128
        assertEquals(-128, C_Int8.minValue().negate().toInt())
    }

    @Test
    fun absoluteValue() {
        assertEquals(100, C_Int8.fromInt(-100).abs().toInt())
        assertEquals(100, C_Int8.fromInt(100).abs().toInt())
        assertEquals(0, C_Int8.zero().abs().toInt())
        assertEquals(127, C_Int8.maxValue().abs().toInt())
        // abs(MIN_VALUE) wraps in two's complement
        assertEquals(-128, C_Int8.minValue().abs().toInt())
    }

    // -- Multiplication ------------------------------------------------------

    @Test
    fun multiplicationSignRules() {
        assertEquals(-50, (C_Int8.fromInt(-10) * C_Int8.fromInt(5)).toInt())
        assertEquals(50, (C_Int8.fromInt(-10) * C_Int8.fromInt(-5)).toInt())
        assertEquals(0, (C_Int8.maxValue() * C_Int8.zero()).toInt())
    }

    @Test
    fun multiplicationWraps() {
        // 16 * 16 = 256 → wraps to 0 in 8-bit signed
        assertEquals(0, (C_Int8.fromInt(16) * C_Int8.fromInt(16)).toInt())
    }

    // -- Division / modulus --------------------------------------------------

    @Test
    fun divisionAndModulusSigns() {
        assertEquals(-100 / 7, (C_Int8.fromInt(-100) / C_Int8.fromInt(7)).toInt())
        assertEquals(-100 % 7, (C_Int8.fromInt(-100) % C_Int8.fromInt(7)).toInt())
        assertEquals(100 / -7, (C_Int8.fromInt(100) / C_Int8.fromInt(-7)).toInt())
        assertEquals((-100) / (-7), (C_Int8.fromInt(-100) / C_Int8.fromInt(-7)).toInt())
    }

    @Test
    fun divisionByZeroThrows() {
        val a = C_Int8.fromInt(10)
        assertFailsWith<IllegalArgumentException> { a / C_Int8.zero() }
        assertFailsWith<IllegalArgumentException> { a % C_Int8.zero() }
    }

    // -- Bitwise -------------------------------------------------------------

    @Test
    fun bitwiseTruthTables() {
        val a = C_Int8.fromInt(0x0F)
        val b = C_Int8.fromInt(0x70.toByte().toInt())
        assertEquals(0x00, (a and b).toInt() and 0xFF)
        assertEquals(0x7F, (a or b).toInt() and 0xFF)
        assertEquals(0x7F, (a xor b).toInt() and 0xFF)
    }

    @Test
    fun bitwiseInvAndBitFlip() {
        // ~0 = -1 (all-ones), ~-1 = 0
        assertEquals(-1, C_Int8.zero().inv().toInt())
        assertEquals(0, C_Int8.fromInt(-1).inv().toInt())
        assertEquals(C_Int8.fromInt(0x55).toInt(), C_Int8.fromInt(0xAA.toByte().toInt()).inv().toInt())
    }

    // -- Shifts --------------------------------------------------------------

    @Test
    fun shiftLeftAcrossWidth() {
        assertEquals(2, C_Int8.one().shiftLeft(1).toInt())
        assertEquals(0x40, C_Int8.one().shiftLeft(6).toInt())
        // Shift 1 → bit 7 → -128 in 8-bit signed
        assertEquals(-128, C_Int8.one().shiftLeft(7).toInt())
        assertEquals(0xCD.toByte().toInt(), C_Int8.fromInt(0xCD.toByte().toInt()).shiftLeft(0).toInt())
    }

    @Test
    fun arithmeticRightShiftSignExtends() {
        // -64 >> 2 = -16 (sign bit preserved)
        assertEquals(-16, C_Int8.fromInt(-64).shiftRight(2).toInt())
        // -1 >> 7 stays -1 (all ones)
        assertEquals(-1, C_Int8.fromInt(-1).shiftRight(7).toInt())
        // 64 >> 2 = 16 (positive: same as unsigned)
        assertEquals(16, C_Int8.fromInt(64).shiftRight(2).toInt())
        // shift by 0 is identity
        assertEquals(-42, C_Int8.fromInt(-42).shiftRight(0).toInt())
    }

    @Test
    fun logicalRightShiftZeroFill() {
        // -64 ushr 2 → treat as 0xC0 → 0x30 = 48
        assertEquals(48, C_Int8.fromInt(-64).shiftRightUnsigned(2).toInt())
        // -1 ushr 7 → treat as 0xFF → 1
        assertEquals(1, C_Int8.fromInt(-1).shiftRightUnsigned(7).toInt())
    }

    @Test
    fun shiftRangeReject() {
        assertFailsWith<IllegalArgumentException> { C_Int8.one().shiftLeft(8) }
        assertFailsWith<IllegalArgumentException> { C_Int8.one().shiftRight(8) }
        assertFailsWith<IllegalArgumentException> { C_Int8.one().shiftRightUnsigned(-1) }
    }

    // -- Comparison ----------------------------------------------------------

    @Test
    fun comparisonAcrossSigns() {
        assertTrue(C_Int8.fromInt(-1) < C_Int8.zero())
        assertTrue(C_Int8.minValue() < C_Int8.maxValue())
        assertTrue(C_Int8.minValue() < C_Int8.fromInt(-127))
        assertEquals(0, C_Int8.fromInt(42).compareTo(C_Int8.fromInt(42)))
    }

    // -- Equality / hashing --------------------------------------------------

    @Test
    fun equalsAndHashCode() {
        val a = C_Int8.fromInt(-50)
        val b = C_Int8.fromInt(-50)
        val c = C_Int8.fromInt(50)
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equalsAgainstNonType() {
        assertFalse(C_Int8.zero().equals(0))
        assertFalse(C_Int8.zero().equals(null))
    }

    // -- Hex formatting (unsigned representation) ----------------------------

    @Test
    fun hexFormat() {
        assertEquals("0x00", C_Int8.zero().toHexString())
        assertEquals("0xff", C_Int8.fromInt(-1).toHexString())
        assertEquals("0x80", C_Int8.minValue().toHexString())
        assertEquals("0x7f", C_Int8.maxValue().toHexString())
    }
}

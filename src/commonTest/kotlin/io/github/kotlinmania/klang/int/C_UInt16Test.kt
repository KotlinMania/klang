package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun roundTrip() {
        for (v in listOf(0u, 1u, 0x1234u, 0xFFFFu)) {
            assertEquals(v.toUShort(), C_UInt16.fromUShort(v.toUShort()).toUShort())
        }
    }

    @Test
    fun additionWrapsAt16Bits() {
        assertEquals(0u, (C_UInt16.maxValue() + C_UInt16.one()).toUInt())
        assertEquals(0x100u, (C_UInt16.fromUInt(0xFF00u) + C_UInt16.fromUInt(0x200u)).toUInt())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(0xFFFFu, (C_UInt16.zero() - C_UInt16.one()).toUInt())
    }

    @Test
    fun multiplicationWraps() {
        val a = C_UInt16.fromUInt(0x100u)
        // 0x100 * 0x100 = 0x10000, wraps to 0
        assertEquals(0u, (a * a).toUInt())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt16.fromUInt(1000u)
        val b = C_UInt16.fromUInt(7u)
        assertEquals(142u, (a / b).toUInt())
        assertEquals(6u, (a % b).toUInt())
        assertFailsWith<IllegalArgumentException> { a / C_UInt16.zero() }
    }

    @Test
    fun bitwise() {
        val a = C_UInt16.fromUInt(0xFF00u)
        val b = C_UInt16.fromUInt(0x00FFu)
        assertEquals(0u, (a and b).toUInt())
        assertEquals(0xFFFFu, (a or b).toUInt())
        assertEquals(0xFFFFu, (a xor b).toUInt())
    }

    @Test
    fun shifts() {
        assertEquals(0x8000u, C_UInt16.one().shiftLeft(15).toUInt())
        assertEquals(1u, C_UInt16.fromUInt(0x8000u).shiftRight(15).toUInt())
    }

    @Test
    fun comparison() {
        assertTrue(C_UInt16.fromUInt(100u) < C_UInt16.maxValue())
    }
}

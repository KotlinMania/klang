package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.mem.KMalloc
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class C_UInt8Test {

    @BeforeTest
    fun setup() { KMalloc.init(64 * 1024) }

    @Test
    fun zeroOneMax() {
        assertEquals(0u, C_UInt8.zero().toUInt())
        assertEquals(1u, C_UInt8.one().toUInt())
        assertEquals(0xFFu, C_UInt8.maxValue().toUInt())
    }

    @Test
    fun roundTrip() {
        for (v in listOf<UByte>(0u, 1u, 0xCAu, 0xFFu)) {
            assertEquals(v, C_UInt8.fromUByte(v).toUByte())
        }
    }

    @Test
    fun additionWrapsAt8Bits() {
        assertEquals(0u, (C_UInt8.maxValue() + C_UInt8.one()).toUInt())
        assertEquals(0x10u, (C_UInt8.fromUInt(0xF0u) + C_UInt8.fromUInt(0x20u)).toUInt())
    }

    @Test
    fun subtractionWraps() {
        assertEquals(0xFFu, (C_UInt8.zero() - C_UInt8.one()).toUInt())
    }

    @Test
    fun divisionAndModulus() {
        val a = C_UInt8.fromUInt(100u)
        val b = C_UInt8.fromUInt(7u)
        assertEquals(14u, (a / b).toUInt())
        assertEquals(2u, (a % b).toUInt())
        assertFailsWith<IllegalArgumentException> { a / C_UInt8.zero() }
    }

    @Test
    fun bitwise() {
        val a = C_UInt8.fromUInt(0xF0u)
        val b = C_UInt8.fromUInt(0x0Fu)
        assertEquals(0u, (a and b).toUInt())
        assertEquals(0xFFu, (a or b).toUInt())
        assertEquals(0xFFu, (a xor b).toUInt())
    }

    @Test
    fun shifts() {
        assertEquals(0x80u, C_UInt8.one().shiftLeft(7).toUInt())
        assertEquals(1u, C_UInt8.fromUInt(0x80u).shiftRight(7).toUInt())
    }

    @Test
    fun comparison() {
        assertTrue(C_UInt8.fromUInt(100u) < C_UInt8.maxValue())
    }
}

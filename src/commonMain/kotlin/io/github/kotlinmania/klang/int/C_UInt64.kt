package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_UInt64: C-compatible `uint64_t` with zero-copy heap operations.
 *
 * Range: 0 to 2^64 - 1. All shifts/bitwise ops go through a [BitShiftEngine]
 * configured for 64 bits. Arithmetic uses Kotlin's `ULong` (which wraps
 * modulo 2^64 — matching unsigned C semantics).
 */
class C_UInt64 private constructor(val addr: Int) : Comparable<C_UInt64> {

    /** Raw 64-bit stored value as Long. */
    private fun toRawLong(): Long = GlobalHeap.ld(addr)

    fun toULong(): ULong = toRawLong().toULong()

    fun toHexString(): String = "0x" + toULong().toString(16).padStart(16, '0')

    override fun toString(): String = toULong().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_UInt64) return false
        return GlobalHeap.ld(this.addr) == GlobalHeap.ld(other.addr)
    }

    override fun hashCode(): Int = GlobalHeap.ld(addr).hashCode()

    override fun compareTo(other: C_UInt64): Int =
        this.toULong().compareTo(other.toULong())

    operator fun plus(other: C_UInt64): C_UInt64 =
        store((this.toULong() + other.toULong()).toLong())

    operator fun minus(other: C_UInt64): C_UInt64 =
        store((this.toULong() - other.toULong()).toLong())

    operator fun times(other: C_UInt64): C_UInt64 =
        store((this.toULong() * other.toULong()).toLong())

    operator fun div(other: C_UInt64): C_UInt64 {
        val divisor = other.toULong()
        require(divisor != 0uL) { "C_UInt64 division by zero" }
        return store((this.toULong() / divisor).toLong())
    }

    operator fun rem(other: C_UInt64): C_UInt64 {
        val divisor = other.toULong()
        require(divisor != 0uL) { "C_UInt64 modulus by zero" }
        return store((this.toULong() % divisor).toLong())
    }

    infix fun and(other: C_UInt64): C_UInt64 =
        store(engine.bitwiseAnd(this.toRawLong(), other.toRawLong()))

    infix fun or(other: C_UInt64): C_UInt64 =
        store(engine.bitwiseOr(this.toRawLong(), other.toRawLong()))

    infix fun xor(other: C_UInt64): C_UInt64 =
        store(engine.bitwiseXor(this.toRawLong(), other.toRawLong()))

    fun inv(): C_UInt64 = store(engine.bitwiseNot(this.toRawLong()))

    fun shiftLeft(bits: Int): C_UInt64 {
        require(bits in 0..63) { "C_UInt64 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toRawLong(), bits).value)
    }

    fun shiftRight(bits: Int): C_UInt64 {
        require(bits in 0..63) { "C_UInt64 shift amount out of range: $bits" }
        return store(engine.unsignedRightShift(this.toRawLong(), bits).value)
    }

    fun copy(): C_UInt64 = alloc().also { GlobalHeap.sd(it.addr, GlobalHeap.ld(this.addr)) }

    private fun store(value: Long): C_UInt64 {
        val res = alloc()
        GlobalHeap.sd(res.addr, value)
        return res
    }

    companion object {
        const val BYTES: Int = 8

        /** BitShiftEngine for 64-bit operations (shifts, bitwise). */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 64)

        fun alloc(): C_UInt64 = C_UInt64(KMalloc.malloc(BYTES))
        fun zero(): C_UInt64 = alloc().also { GlobalHeap.sd(it.addr, 0L) }
        fun one(): C_UInt64 = alloc().also { GlobalHeap.sd(it.addr, 1L) }
        /** All-1s bit pattern = 2^64 - 1. */
        fun maxValue(): C_UInt64 = alloc().also { GlobalHeap.sd(it.addr, engine.bitwiseNot(0L)) }

        fun fromULong(value: ULong): C_UInt64 =
            alloc().also { GlobalHeap.sd(it.addr, value.toLong()) }

        fun fromLong(value: Long): C_UInt64 =
            alloc().also { GlobalHeap.sd(it.addr, value) }
    }
}

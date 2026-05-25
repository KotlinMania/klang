package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_Int32: C-compatible `int32_t` with zero-copy heap operations.
 *
 * Range: -2_147_483_648 to 2_147_483_647 (two's complement). All shifts/bitwise
 * ops/masks go through a [BitShiftEngine] configured for 32 bits.
 *
 * @property addr Heap address of the 4-byte value
 */
class C_Int32 private constructor(val addr: Int) : Comparable<C_Int32> {

    /** Load the value as unsigned Long (zero-extended from 32 bits). */
    private fun toUnsignedLong(): Long = engine.bitwiseAnd(GlobalHeap.lw(addr).toLong(), MASK_32)

    /** Load the value as a signed Long (sign-extended from 32 bits). */
    private fun toSignedLong(): Long = signExtender.signExtend(toUnsignedLong(), 32)

    /** Load as native Int. */
    fun toInt(): Int = toSignedLong().toInt()

    fun isNegative(): Boolean = engine.isBitSet(toUnsignedLong(), 31)

    fun toHexString(): String {
        val v = toUnsignedLong().toString(16)
        return "0x" + v.padStart(8, '0')
    }

    override fun toString(): String = toInt().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_Int32) return false
        return GlobalHeap.lw(this.addr) == GlobalHeap.lw(other.addr)
    }

    override fun hashCode(): Int = GlobalHeap.lw(addr)

    override fun compareTo(other: C_Int32): Int =
        this.toSignedLong().compareTo(other.toSignedLong())

    operator fun plus(other: C_Int32): C_Int32 =
        store(this.toSignedLong() + other.toSignedLong())

    operator fun minus(other: C_Int32): C_Int32 =
        store(this.toSignedLong() - other.toSignedLong())

    operator fun times(other: C_Int32): C_Int32 =
        store(this.toSignedLong() * other.toSignedLong())

    operator fun div(other: C_Int32): C_Int32 {
        val divisor = other.toInt()
        require(divisor != 0) { "C_Int32 division by zero" }
        return store((this.toInt() / divisor).toLong())
    }

    operator fun rem(other: C_Int32): C_Int32 {
        val divisor = other.toInt()
        require(divisor != 0) { "C_Int32 modulus by zero" }
        return store((this.toInt() % divisor).toLong())
    }

    operator fun unaryMinus(): C_Int32 = negate()

    fun negate(): C_Int32 = store(-this.toSignedLong())

    fun abs(): C_Int32 = if (isNegative()) negate() else copy()

    infix fun and(other: C_Int32): C_Int32 =
        store(engine.bitwiseAnd(this.toUnsignedLong(), other.toUnsignedLong()))

    infix fun or(other: C_Int32): C_Int32 =
        store(engine.bitwiseOr(this.toUnsignedLong(), other.toUnsignedLong()))

    infix fun xor(other: C_Int32): C_Int32 =
        store(engine.bitwiseXor(this.toUnsignedLong(), other.toUnsignedLong()))

    fun inv(): C_Int32 = store(engine.bitwiseNot(this.toUnsignedLong()))

    fun shiftLeft(bits: Int): C_Int32 {
        require(bits in 0..31) { "C_Int32 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toUnsignedLong(), bits).value)
    }

    /** Arithmetic right shift (sign-extending). */
    fun shiftRight(bits: Int): C_Int32 {
        require(bits in 0..31) { "C_Int32 shift amount out of range: $bits" }
        if (bits == 0) return copy()
        val unsignedValue = this.toUnsignedLong()
        val shifted = engine.unsignedRightShift(unsignedValue, bits).value
        val result = if (isNegative()) {
            val signMask = engine.bitwiseAnd(
                engine.leftShift(engine.getMask(bits), 32 - bits).value,
                MASK_32,
            )
            engine.bitwiseOr(shifted, signMask)
        } else {
            shifted
        }
        return store(result)
    }

    /** Logical right shift (zero-fill, ignores sign). */
    fun shiftRightUnsigned(bits: Int): C_Int32 {
        require(bits in 0..31) { "C_Int32 shift amount out of range: $bits" }
        val unsignedValue = this.toUnsignedLong()
        return store(engine.unsignedRightShift(unsignedValue, bits).value)
    }

    fun copy(): C_Int32 = alloc().also { GlobalHeap.sw(it.addr, GlobalHeap.lw(this.addr)) }

    private fun store(value: Long): C_Int32 {
        val res = alloc()
        GlobalHeap.sw(res.addr, value.toInt())
        return res
    }

    companion object {
        const val BYTES: Int = 4

        /** BitShiftEngine for 32-bit operations (shifts, bitwise, width mask). */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 32)

        /** 64-bit engine used solely to sign-extend 32-bit values to a full Long. */
        private val signExtender = BitShiftEngine(BitShiftMode.NATIVE, 64)
        private val MASK_32: Long = engine.getMask(32)

        fun alloc(): C_Int32 = C_Int32(KMalloc.malloc(BYTES))
        fun zero(): C_Int32 = alloc().also { GlobalHeap.sw(it.addr, 0) }
        fun one(): C_Int32 = alloc().also { GlobalHeap.sw(it.addr, 1) }
        fun minValue(): C_Int32 = alloc().also { GlobalHeap.sw(it.addr, Int.MIN_VALUE) }
        fun maxValue(): C_Int32 = alloc().also { GlobalHeap.sw(it.addr, Int.MAX_VALUE) }

        fun fromInt(value: Int): C_Int32 =
            alloc().also { GlobalHeap.sw(it.addr, value) }
    }
}

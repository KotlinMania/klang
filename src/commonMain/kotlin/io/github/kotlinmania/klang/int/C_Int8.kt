package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_Int8: C-compatible `int8_t` with zero-copy heap operations.
 *
 * Range: -128 to 127 (two's complement). All shifts/bitwise ops/masks go
 * through a [BitShiftEngine] configured for 8 bits.
 */
class C_Int8 private constructor(val addr: Int) : Comparable<C_Int8> {

    private fun toUnsignedLong(): Long = engine.bitwiseAnd(GlobalHeap.lb(addr).toLong(), MASK_8)
    private fun toSignedLong(): Long = signExtender.signExtend(toUnsignedLong(), 8)

    fun toByte(): Byte = toSignedLong().toByte()
    fun toInt(): Int = toSignedLong().toInt()

    fun isNegative(): Boolean = engine.isBitSet(toUnsignedLong(), 7)

    fun toHexString(): String = "0x" + toUnsignedLong().toString(16).padStart(2, '0')

    override fun toString(): String = toInt().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_Int8) return false
        return GlobalHeap.lb(this.addr) == GlobalHeap.lb(other.addr)
    }

    override fun hashCode(): Int = GlobalHeap.lb(addr).toInt()

    override fun compareTo(other: C_Int8): Int =
        this.toSignedLong().compareTo(other.toSignedLong())

    operator fun plus(other: C_Int8): C_Int8 =
        store(this.toSignedLong() + other.toSignedLong())

    operator fun minus(other: C_Int8): C_Int8 =
        store(this.toSignedLong() - other.toSignedLong())

    operator fun times(other: C_Int8): C_Int8 =
        store(this.toSignedLong() * other.toSignedLong())

    operator fun div(other: C_Int8): C_Int8 {
        val divisor = other.toSignedLong()
        require(divisor != 0L) { "C_Int8 division by zero" }
        return store(this.toSignedLong() / divisor)
    }

    operator fun rem(other: C_Int8): C_Int8 {
        val divisor = other.toSignedLong()
        require(divisor != 0L) { "C_Int8 modulus by zero" }
        return store(this.toSignedLong() % divisor)
    }

    operator fun unaryMinus(): C_Int8 = negate()

    fun negate(): C_Int8 = store(-this.toSignedLong())

    fun abs(): C_Int8 = if (isNegative()) negate() else copy()

    infix fun and(other: C_Int8): C_Int8 =
        store(engine.bitwiseAnd(this.toUnsignedLong(), other.toUnsignedLong()))

    infix fun or(other: C_Int8): C_Int8 =
        store(engine.bitwiseOr(this.toUnsignedLong(), other.toUnsignedLong()))

    infix fun xor(other: C_Int8): C_Int8 =
        store(engine.bitwiseXor(this.toUnsignedLong(), other.toUnsignedLong()))

    fun inv(): C_Int8 = store(engine.bitwiseNot(this.toUnsignedLong()))

    fun shiftLeft(bits: Int): C_Int8 {
        require(bits in 0..7) { "C_Int8 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toUnsignedLong(), bits).value)
    }

    /** Arithmetic right shift (sign-extending). */
    fun shiftRight(bits: Int): C_Int8 {
        require(bits in 0..7) { "C_Int8 shift amount out of range: $bits" }
        if (bits == 0) return copy()
        val shifted = engine.unsignedRightShift(this.toUnsignedLong(), bits).value
        val result = if (isNegative()) {
            val signMask = engine.bitwiseAnd(
                engine.leftShift(engine.getMask(bits), 8 - bits).value,
                MASK_8,
            )
            engine.bitwiseOr(shifted, signMask)
        } else {
            shifted
        }
        return store(result)
    }

    /** Logical right shift (zero-fill, ignores sign). */
    fun shiftRightUnsigned(bits: Int): C_Int8 {
        require(bits in 0..7) { "C_Int8 shift amount out of range: $bits" }
        return store(engine.unsignedRightShift(this.toUnsignedLong(), bits).value)
    }

    fun copy(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, GlobalHeap.lb(this.addr)) }

    private fun store(value: Long): C_Int8 {
        val res = alloc()
        GlobalHeap.sb(res.addr, engine.bitwiseAnd(value, MASK_8).toByte())
        return res
    }

    companion object {
        const val BYTES: Int = 1
        const val MIN_VALUE: Byte = Byte.MIN_VALUE
        const val MAX_VALUE: Byte = Byte.MAX_VALUE

        /** BitShiftEngine for 8-bit operations (shifts, bitwise, width mask). */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 8)

        /** 64-bit engine used solely to sign-extend 8-bit values to a full Long. */
        private val signExtender = BitShiftEngine(BitShiftMode.NATIVE, 64)
        private val MASK_8: Long = engine.getMask(8)

        fun alloc(): C_Int8 = C_Int8(KMalloc.malloc(BYTES))
        fun zero(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, 0) }
        fun one(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, 1) }
        fun minValue(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, MIN_VALUE) }
        fun maxValue(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, MAX_VALUE) }

        fun fromByte(value: Byte): C_Int8 =
            alloc().also { GlobalHeap.sb(it.addr, value) }

        fun fromInt(value: Int): C_Int8 =
            alloc().also { GlobalHeap.sb(it.addr, engine.bitwiseAnd(value.toLong(), MASK_8).toByte()) }
    }
}

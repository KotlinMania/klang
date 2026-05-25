package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_Int8: C-compatible `int8_t` with zero-copy heap operations.
 *
 * Range: -128 to 127 (two's complement). Shifts route through a
 * [BitShiftEngine] configured for 8 bits — that's the only place Kotlin's
 * cross-target bit-alignment problems matter. Sign extension uses Kotlin's
 * primitive `Byte.toLong()` widening (one canonical sign-extension instruction
 * on every backend). AND/OR/XOR/NOT use native operators on full Long values.
 *
 * @native-bitshift-allowed Native AND/OR/XOR/NOT on full Long values are
 * uniformly safe across all targets (no alignment differences). Sign extension
 * via primitive widening is a single CPU instruction on all backends. The width
 * mask comes from BitShiftEngine.getMask(). Only shifts use the engine's
 * cross-platform shift logic.
 */
class C_Int8 private constructor(val addr: Int) : Comparable<C_Int8> {

    private fun toSignedLong(): Long = GlobalHeap.lb(addr).toLong()
    private fun toUnsignedLong(): Long = toSignedLong() and MASK_8

    fun toByte(): Byte = GlobalHeap.lb(addr)
    fun toInt(): Int = toSignedLong().toInt()

    fun isNegative(): Boolean = GlobalHeap.lb(addr) < 0

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
        store(this.toUnsignedLong() and other.toUnsignedLong())

    infix fun or(other: C_Int8): C_Int8 =
        store(this.toUnsignedLong() or other.toUnsignedLong())

    infix fun xor(other: C_Int8): C_Int8 =
        store(this.toUnsignedLong() xor other.toUnsignedLong())

    fun inv(): C_Int8 = store(this.toUnsignedLong().inv())

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
            val signMask = engine.leftShift(engine.getMask(bits), 8 - bits).value and MASK_8
            shifted or signMask
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
        GlobalHeap.sb(res.addr, (value and MASK_8).toByte())
        return res
    }

    companion object {
        const val BYTES: Int = 1
        const val MIN_VALUE: Byte = Byte.MIN_VALUE
        const val MAX_VALUE: Byte = Byte.MAX_VALUE

        /** BitShiftEngine for 8-bit shifts. */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 8)
        private val MASK_8: Long = engine.getMask(8)

        fun alloc(): C_Int8 = C_Int8(KMalloc.malloc(BYTES))
        fun zero(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, 0) }
        fun one(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, 1) }
        fun minValue(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, MIN_VALUE) }
        fun maxValue(): C_Int8 = alloc().also { GlobalHeap.sb(it.addr, MAX_VALUE) }

        fun fromByte(value: Byte): C_Int8 =
            alloc().also { GlobalHeap.sb(it.addr, value) }

        fun fromInt(value: Int): C_Int8 =
            alloc().also { GlobalHeap.sb(it.addr, (value.toLong() and MASK_8).toByte()) }
    }
}

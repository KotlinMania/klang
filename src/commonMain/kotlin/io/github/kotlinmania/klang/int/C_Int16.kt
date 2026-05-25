package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_Int16: C-compatible `int16_t` with zero-copy heap operations.
 *
 * Range: -32_768 to 32_767 (two's complement). Shifts route through a
 * [BitShiftEngine] configured for 16 bits. Sign extension uses Kotlin's
 * primitive `Short.toLong()` widening; AND/OR/XOR/NOT use native operators.
 *
 * @native-bitshift-allowed This fixed-width integer type uses native bitwise
 * operators (and, or, xor, inv) for masking Long values, which is safe across
 * all targets. Shifts are routed through BitShiftEngine for cross-platform
 * determinism.
 */
class C_Int16 private constructor(val addr: Int) : Comparable<C_Int16> {

    private fun toSignedLong(): Long = GlobalHeap.lh(addr).toLong()
    private fun toUnsignedLong(): Long = toSignedLong() and MASK_16

    fun toShort(): Short = GlobalHeap.lh(addr)
    fun toInt(): Int = toSignedLong().toInt()

    fun isNegative(): Boolean = GlobalHeap.lh(addr) < 0

    fun toHexString(): String = "0x" + toUnsignedLong().toString(16).padStart(4, '0')

    override fun toString(): String = toInt().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_Int16) return false
        return GlobalHeap.lh(this.addr) == GlobalHeap.lh(other.addr)
    }

    override fun hashCode(): Int = GlobalHeap.lh(addr).toInt()

    override fun compareTo(other: C_Int16): Int =
        this.toSignedLong().compareTo(other.toSignedLong())

    operator fun plus(other: C_Int16): C_Int16 =
        store(this.toSignedLong() + other.toSignedLong())

    operator fun minus(other: C_Int16): C_Int16 =
        store(this.toSignedLong() - other.toSignedLong())

    operator fun times(other: C_Int16): C_Int16 =
        store(this.toSignedLong() * other.toSignedLong())

    operator fun div(other: C_Int16): C_Int16 {
        val divisor = other.toSignedLong()
        require(divisor != 0L) { "C_Int16 division by zero" }
        return store(this.toSignedLong() / divisor)
    }

    operator fun rem(other: C_Int16): C_Int16 {
        val divisor = other.toSignedLong()
        require(divisor != 0L) { "C_Int16 modulus by zero" }
        return store(this.toSignedLong() % divisor)
    }

    operator fun unaryMinus(): C_Int16 = negate()

    fun negate(): C_Int16 = store(-this.toSignedLong())

    fun abs(): C_Int16 = if (isNegative()) negate() else copy()

    infix fun and(other: C_Int16): C_Int16 =
        store(this.toUnsignedLong() and other.toUnsignedLong())

    infix fun or(other: C_Int16): C_Int16 =
        store(this.toUnsignedLong() or other.toUnsignedLong())

    infix fun xor(other: C_Int16): C_Int16 =
        store(this.toUnsignedLong() xor other.toUnsignedLong())

    fun inv(): C_Int16 = store(this.toUnsignedLong().inv())

    fun shiftLeft(bits: Int): C_Int16 {
        require(bits in 0..15) { "C_Int16 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toUnsignedLong(), bits).value)
    }

    /** Arithmetic right shift (sign-extending). */
    fun shiftRight(bits: Int): C_Int16 {
        require(bits in 0..15) { "C_Int16 shift amount out of range: $bits" }
        if (bits == 0) return copy()
        val shifted = engine.unsignedRightShift(this.toUnsignedLong(), bits).value
        val result = if (isNegative()) {
            val signMask = engine.leftShift(engine.getMask(bits), 16 - bits).value and MASK_16
            shifted or signMask
        } else {
            shifted
        }
        return store(result)
    }

    /** Logical right shift (zero-fill). */
    fun shiftRightUnsigned(bits: Int): C_Int16 {
        require(bits in 0..15) { "C_Int16 shift amount out of range: $bits" }
        return store(engine.unsignedRightShift(this.toUnsignedLong(), bits).value)
    }

    fun copy(): C_Int16 = alloc().also { GlobalHeap.sh(it.addr, GlobalHeap.lh(this.addr)) }

    private fun store(value: Long): C_Int16 {
        val res = alloc()
        GlobalHeap.sh(res.addr, (value and MASK_16).toShort())
        return res
    }

    companion object {
        const val BYTES: Int = 2
        const val MIN_VALUE: Short = Short.MIN_VALUE
        const val MAX_VALUE: Short = Short.MAX_VALUE

        /** BitShiftEngine for 16-bit shifts. */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 16)
        private val MASK_16: Long = engine.getMask(16)

        fun alloc(): C_Int16 = C_Int16(KMalloc.malloc(BYTES))
        fun zero(): C_Int16 = alloc().also { GlobalHeap.sh(it.addr, 0) }
        fun one(): C_Int16 = alloc().also { GlobalHeap.sh(it.addr, 1) }
        fun minValue(): C_Int16 = alloc().also { GlobalHeap.sh(it.addr, MIN_VALUE) }
        fun maxValue(): C_Int16 = alloc().also { GlobalHeap.sh(it.addr, MAX_VALUE) }

        fun fromShort(value: Short): C_Int16 =
            alloc().also { GlobalHeap.sh(it.addr, value) }

        fun fromInt(value: Int): C_Int16 =
            alloc().also { GlobalHeap.sh(it.addr, (value.toLong() and MASK_16).toShort()) }
    }
}

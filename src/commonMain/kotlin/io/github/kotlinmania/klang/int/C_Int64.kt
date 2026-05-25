package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_Int64: C-compatible `int64_t` with zero-copy heap operations.
 *
 * Range: Long.MIN_VALUE to Long.MAX_VALUE (two's complement). Shifts route
 * through a [BitShiftEngine] configured for 64 bits. AND/OR/XOR/NOT use
 * native operators on full Long values.
 *
 * @native-bitshift-allowed Native AND/OR/XOR/NOT on full Long values are
 * uniformly safe across all targets (no alignment differences). For 64-bit
 * values stored in Long, no width mask or sign extension is needed. Only
 * shifts use the engine's cross-platform shift logic.
 */
class C_Int64 private constructor(val addr: Int) : Comparable<C_Int64> {

    private fun toRawLong(): Long = GlobalHeap.ld(addr)

    fun toLong(): Long = toRawLong()

    fun isNegative(): Boolean = toRawLong() < 0L

    fun toHexString(): String = "0x" + toLong().toULong().toString(16).padStart(16, '0')

    override fun toString(): String = toLong().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_Int64) return false
        return GlobalHeap.ld(this.addr) == GlobalHeap.ld(other.addr)
    }

    override fun hashCode(): Int = GlobalHeap.ld(addr).hashCode()

    override fun compareTo(other: C_Int64): Int =
        this.toLong().compareTo(other.toLong())

    operator fun plus(other: C_Int64): C_Int64 =
        store(this.toLong() + other.toLong())

    operator fun minus(other: C_Int64): C_Int64 =
        store(this.toLong() - other.toLong())

    operator fun times(other: C_Int64): C_Int64 =
        store(this.toLong() * other.toLong())

    operator fun div(other: C_Int64): C_Int64 {
        val divisor = other.toLong()
        require(divisor != 0L) { "C_Int64 division by zero" }
        return store(this.toLong() / divisor)
    }

    operator fun rem(other: C_Int64): C_Int64 {
        val divisor = other.toLong()
        require(divisor != 0L) { "C_Int64 modulus by zero" }
        return store(this.toLong() % divisor)
    }

    operator fun unaryMinus(): C_Int64 = negate()

    fun negate(): C_Int64 = store(-this.toLong())

    fun abs(): C_Int64 = if (isNegative()) negate() else copy()

    infix fun and(other: C_Int64): C_Int64 =
        store(this.toRawLong() and other.toRawLong())

    infix fun or(other: C_Int64): C_Int64 =
        store(this.toRawLong() or other.toRawLong())

    infix fun xor(other: C_Int64): C_Int64 =
        store(this.toRawLong() xor other.toRawLong())

    fun inv(): C_Int64 = store(this.toRawLong().inv())

    fun shiftLeft(bits: Int): C_Int64 {
        require(bits in 0..63) { "C_Int64 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toRawLong(), bits).value)
    }

    /** Arithmetic right shift (sign-extending). */
    fun shiftRight(bits: Int): C_Int64 {
        require(bits in 0..63) { "C_Int64 shift amount out of range: $bits" }
        if (bits == 0) return copy()
        val shifted = engine.unsignedRightShift(this.toRawLong(), bits).value
        val result = if (isNegative()) {
            val signMask = engine.leftShift(engine.getMask(bits), 64 - bits).value
            shifted or signMask
        } else {
            shifted
        }
        return store(result)
    }

    /** Logical right shift (zero-fill). */
    fun shiftRightUnsigned(bits: Int): C_Int64 {
        require(bits in 0..63) { "C_Int64 shift amount out of range: $bits" }
        return store(engine.unsignedRightShift(this.toRawLong(), bits).value)
    }

    fun copy(): C_Int64 = alloc().also { GlobalHeap.sd(it.addr, GlobalHeap.ld(this.addr)) }

    private fun store(value: Long): C_Int64 {
        val res = alloc()
        GlobalHeap.sd(res.addr, value)
        return res
    }

    companion object {
        const val BYTES: Int = 8

        /** BitShiftEngine for 64-bit shifts. */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 64)

        fun alloc(): C_Int64 = C_Int64(KMalloc.malloc(BYTES))
        fun zero(): C_Int64 = alloc().also { GlobalHeap.sd(it.addr, 0L) }
        fun one(): C_Int64 = alloc().also { GlobalHeap.sd(it.addr, 1L) }
        fun minValue(): C_Int64 = alloc().also { GlobalHeap.sd(it.addr, Long.MIN_VALUE) }
        fun maxValue(): C_Int64 = alloc().also { GlobalHeap.sd(it.addr, Long.MAX_VALUE) }

        fun fromLong(value: Long): C_Int64 =
            alloc().also { GlobalHeap.sd(it.addr, value) }
    }
}

package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_Int32: C-compatible `int32_t` with zero-copy heap operations.
 *
 * Range: -2_147_483_648 to 2_147_483_647 (two's complement). Shifts route
 * through a [BitShiftEngine] configured for 32 bits. Sign extension uses
 * Kotlin's primitive `Int.toLong()` widening; AND/OR/XOR/NOT use native
 * operators on full Long values.
 *
 * @native-bitshift-allowed This fixed-width integer type uses native bitwise
 * operators (and, or, xor, inv) for masking Long values, which is safe across
 * all targets. Shifts are routed through BitShiftEngine for cross-platform
 * determinism.
 *
 * @property addr Heap address of the 4-byte value
 */
class C_Int32 private constructor(val addr: Int) : Comparable<C_Int32> {

    /** Load the value as a signed Long (sign-extended from 32 bits). */
    private fun toLong(): Long = GlobalHeap.lw(addr).toLong()

    /** Load as native Int. */
    fun toInt(): Int = GlobalHeap.lw(addr)

    fun isNegative(): Boolean = GlobalHeap.lw(addr) < 0

    fun toHexString(): String {
        val v = (toLong() and MASK_32).toString(16)
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
        this.toLong().compareTo(other.toLong())

    operator fun plus(other: C_Int32): C_Int32 =
        store(this.toLong() + other.toLong())

    operator fun minus(other: C_Int32): C_Int32 =
        store(this.toLong() - other.toLong())

    operator fun times(other: C_Int32): C_Int32 =
        store(this.toLong() * other.toLong())

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

    fun negate(): C_Int32 = store(-this.toLong())

    fun abs(): C_Int32 = if (isNegative()) negate() else copy()

    infix fun and(other: C_Int32): C_Int32 =
        store(this.toLong() and other.toLong())

    infix fun or(other: C_Int32): C_Int32 =
        store(this.toLong() or other.toLong())

    infix fun xor(other: C_Int32): C_Int32 =
        store(this.toLong() xor other.toLong())

    fun inv(): C_Int32 = store(this.toLong().inv())

    fun shiftLeft(bits: Int): C_Int32 {
        require(bits in 0..31) { "C_Int32 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toLong(), bits).value)
    }

    /** Arithmetic right shift (sign-extending). */
    fun shiftRight(bits: Int): C_Int32 {
        require(bits in 0..31) { "C_Int32 shift amount out of range: $bits" }
        if (bits == 0) return copy()
        val unsignedValue = this.toLong() and MASK_32
        val shifted = engine.unsignedRightShift(unsignedValue, bits).value
        val result = if (isNegative()) {
            val signMask = engine.leftShift(engine.getMask(bits), 32 - bits).value and MASK_32
            shifted or signMask
        } else {
            shifted
        }
        return store(result)
    }

    /** Logical right shift (zero-fill, ignores sign). */
    fun shiftRightUnsigned(bits: Int): C_Int32 {
        require(bits in 0..31) { "C_Int32 shift amount out of range: $bits" }
        val unsignedValue = this.toLong() and MASK_32
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

        /** BitShiftEngine for 32-bit shifts. */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 32)
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

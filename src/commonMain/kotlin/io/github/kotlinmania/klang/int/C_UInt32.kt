package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_UInt32: C-compatible `uint32_t` with zero-copy heap operations.
 *
 * Range: 0 to 4_294_967_295. Stored as 4 bytes on heap; loaded into a Long for
 * arithmetic. All shifts/bitwise ops/masks go through a [BitShiftEngine]
 * configured for 32 bits.
 *
 * @property addr Heap address of the 4-byte value
 */
class C_UInt32 private constructor(val addr: Int) : Comparable<C_UInt32> {

    /** Load the stored value as a Long (zero-extended). */
    private fun toLong(): Long = engine.bitwiseAnd(GlobalHeap.lw(addr).toLong(), MASK_32)

    /** Load the stored value as a Kotlin UInt. */
    fun toUInt(): UInt = toLong().toUInt()

    fun toHexString(): String {
        val v = toLong().toString(16)
        return "0x" + v.padStart(8, '0')
    }

    override fun toString(): String = toUInt().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_UInt32) return false
        return this.toLong() == other.toLong()
    }

    override fun hashCode(): Int = toLong().toInt()

    override fun compareTo(other: C_UInt32): Int =
        this.toUInt().compareTo(other.toUInt())

    /** Wrapping addition (32-bit). */
    operator fun plus(other: C_UInt32): C_UInt32 =
        store(engine.bitwiseAnd(this.toLong() + other.toLong(), MASK_32))

    /** Wrapping subtraction (32-bit). */
    operator fun minus(other: C_UInt32): C_UInt32 =
        store(engine.bitwiseAnd(this.toLong() - other.toLong(), MASK_32))

    /** Wrapping multiplication (32-bit). */
    operator fun times(other: C_UInt32): C_UInt32 =
        store(engine.bitwiseAnd((this.toUInt() * other.toUInt()).toLong(), MASK_32))

    operator fun div(other: C_UInt32): C_UInt32 {
        val divisor = other.toUInt()
        require(divisor != 0u) { "C_UInt32 division by zero" }
        return store((this.toUInt() / divisor).toLong())
    }

    operator fun rem(other: C_UInt32): C_UInt32 {
        val divisor = other.toUInt()
        require(divisor != 0u) { "C_UInt32 modulus by zero" }
        return store((this.toUInt() % divisor).toLong())
    }

    infix fun and(other: C_UInt32): C_UInt32 =
        store(engine.bitwiseAnd(this.toLong(), other.toLong()))

    infix fun or(other: C_UInt32): C_UInt32 =
        store(engine.bitwiseOr(this.toLong(), other.toLong()))

    infix fun xor(other: C_UInt32): C_UInt32 =
        store(engine.bitwiseXor(this.toLong(), other.toLong()))

    fun inv(): C_UInt32 = store(engine.bitwiseAnd(engine.bitwiseNot(this.toLong()), MASK_32))

    fun shiftLeft(bits: Int): C_UInt32 {
        require(bits in 0..31) { "C_UInt32 shift amount out of range: $bits" }
        val r = engine.leftShift(this.toLong(), bits)
        return store(r.value)
    }

    /** Logical right shift (zero-fill). */
    fun shiftRight(bits: Int): C_UInt32 {
        require(bits in 0..31) { "C_UInt32 shift amount out of range: $bits" }
        val r = engine.unsignedRightShift(this.toLong(), bits)
        return store(r.value)
    }

    fun copy(): C_UInt32 = alloc().also { GlobalHeap.sw(it.addr, GlobalHeap.lw(this.addr)) }

    private fun store(value: Long): C_UInt32 {
        val res = alloc()
        GlobalHeap.sw(res.addr, value.toInt())
        return res
    }

    companion object {
        /** Size in bytes (matches sizeof(uint32_t) in C). */
        const val BYTES: Int = 4

        /** BitShiftEngine for 32-bit operations (shifts, bitwise, width mask). */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 32)

        /** Cached 32-bit width mask. */
        private val MASK_32: Long = engine.getMask(32)

        fun alloc(): C_UInt32 = C_UInt32(KMalloc.malloc(BYTES))
        fun zero(): C_UInt32 = alloc().also { GlobalHeap.sw(it.addr, 0) }
        fun one(): C_UInt32 = alloc().also { GlobalHeap.sw(it.addr, 1) }
        fun maxValue(): C_UInt32 = alloc().also { GlobalHeap.sw(it.addr, MASK_32.toInt()) }

        fun fromUInt(value: UInt): C_UInt32 =
            alloc().also { GlobalHeap.sw(it.addr, value.toInt()) }

        fun fromInt(value: Int): C_UInt32 =
            alloc().also { GlobalHeap.sw(it.addr, value) }
    }
}

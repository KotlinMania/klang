package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_UInt8: C-compatible `uint8_t` with zero-copy heap operations.
 *
 * Range: 0 to 255. All shifts/bitwise ops/masks go through a [BitShiftEngine]
 * configured for 8 bits.
 */
class C_UInt8 private constructor(val addr: Int) : Comparable<C_UInt8> {

    private fun toLong(): Long = engine.bitwiseAnd(GlobalHeap.lb(addr).toLong(), MASK_8)
    fun toUByte(): UByte = toLong().toUByte()
    fun toUInt(): UInt = toLong().toUInt()

    fun toHexString(): String {
        val v = toLong().toString(16)
        return "0x" + v.padStart(2, '0')
    }

    override fun toString(): String = toUInt().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_UInt8) return false
        return this.toLong() == other.toLong()
    }

    override fun hashCode(): Int = toLong().toInt()

    override fun compareTo(other: C_UInt8): Int =
        this.toLong().compareTo(other.toLong())

    operator fun plus(other: C_UInt8): C_UInt8 =
        store(engine.bitwiseAnd(this.toLong() + other.toLong(), MASK_8))

    operator fun minus(other: C_UInt8): C_UInt8 =
        store(engine.bitwiseAnd(this.toLong() - other.toLong(), MASK_8))

    operator fun times(other: C_UInt8): C_UInt8 =
        store(engine.bitwiseAnd(this.toLong() * other.toLong(), MASK_8))

    operator fun div(other: C_UInt8): C_UInt8 {
        val divisor = other.toLong()
        require(divisor != 0L) { "C_UInt8 division by zero" }
        return store(this.toLong() / divisor)
    }

    operator fun rem(other: C_UInt8): C_UInt8 {
        val divisor = other.toLong()
        require(divisor != 0L) { "C_UInt8 modulus by zero" }
        return store(this.toLong() % divisor)
    }

    infix fun and(other: C_UInt8): C_UInt8 =
        store(engine.bitwiseAnd(this.toLong(), other.toLong()))

    infix fun or(other: C_UInt8): C_UInt8 =
        store(engine.bitwiseOr(this.toLong(), other.toLong()))

    infix fun xor(other: C_UInt8): C_UInt8 =
        store(engine.bitwiseXor(this.toLong(), other.toLong()))

    fun inv(): C_UInt8 = store(engine.bitwiseAnd(engine.bitwiseNot(this.toLong()), MASK_8))

    fun shiftLeft(bits: Int): C_UInt8 {
        require(bits in 0..7) { "C_UInt8 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toLong(), bits).value)
    }

    fun shiftRight(bits: Int): C_UInt8 {
        require(bits in 0..7) { "C_UInt8 shift amount out of range: $bits" }
        return store(engine.unsignedRightShift(this.toLong(), bits).value)
    }

    fun copy(): C_UInt8 = alloc().also { GlobalHeap.sb(it.addr, GlobalHeap.lb(this.addr)) }

    private fun store(value: Long): C_UInt8 {
        val res = alloc()
        GlobalHeap.sb(res.addr, engine.bitwiseAnd(value, MASK_8).toByte())
        return res
    }

    companion object {
        const val BYTES: Int = 1

        /** BitShiftEngine for 8-bit operations (shifts, bitwise, width mask). */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 8)
        private val MASK_8: Long = engine.getMask(8)

        fun alloc(): C_UInt8 = C_UInt8(KMalloc.malloc(BYTES))
        fun zero(): C_UInt8 = alloc().also { GlobalHeap.sb(it.addr, 0) }
        fun one(): C_UInt8 = alloc().also { GlobalHeap.sb(it.addr, 1) }
        fun maxValue(): C_UInt8 = alloc().also { GlobalHeap.sb(it.addr, MASK_8.toByte()) }

        fun fromUByte(value: UByte): C_UInt8 =
            alloc().also { GlobalHeap.sb(it.addr, value.toByte()) }

        fun fromUInt(value: UInt): C_UInt8 =
            alloc().also { GlobalHeap.sb(it.addr, engine.bitwiseAnd(value.toLong(), MASK_8).toByte()) }
    }
}

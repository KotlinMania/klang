package io.github.kotlinmania.klang.int

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc

/**
 * C_UInt16: C-compatible `uint16_t` with zero-copy heap operations.
 *
 * Range: 0 to 65_535. Shifts go through a [BitShiftEngine] configured for
 * 16 bits — that's where Kotlin's cross-target bit-alignment problems live.
 * AND/OR/XOR/NOT on full Long values are uniformly safe across targets, so
 * the type applies the engine-built width mask with the native `and` operator
 * for speed.
 *
 * @native-bitshift-allowed This fixed-width integer type uses native bitwise
 * operators (and, or, xor, inv) for masking Long values, which is safe across
 * all targets. Shifts are routed through BitShiftEngine for cross-platform
 * determinism.
 */
class C_UInt16 private constructor(val addr: Int) : Comparable<C_UInt16> {

    private fun toLong(): Long = GlobalHeap.lh(addr).toLong() and MASK_16
    fun toUShort(): UShort = toLong().toUShort()
    fun toUInt(): UInt = toLong().toUInt()

    fun toHexString(): String = "0x" + toLong().toString(16).padStart(4, '0')

    override fun toString(): String = toUInt().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is C_UInt16) return false
        return this.toLong() == other.toLong()
    }

    override fun hashCode(): Int = toLong().toInt()

    override fun compareTo(other: C_UInt16): Int =
        this.toLong().compareTo(other.toLong())

    operator fun plus(other: C_UInt16): C_UInt16 =
        store((this.toLong() + other.toLong()) and MASK_16)

    operator fun minus(other: C_UInt16): C_UInt16 =
        store((this.toLong() - other.toLong()) and MASK_16)

    operator fun times(other: C_UInt16): C_UInt16 =
        store((this.toLong() * other.toLong()) and MASK_16)

    operator fun div(other: C_UInt16): C_UInt16 {
        val divisor = other.toLong()
        require(divisor != 0L) { "C_UInt16 division by zero" }
        return store(this.toLong() / divisor)
    }

    operator fun rem(other: C_UInt16): C_UInt16 {
        val divisor = other.toLong()
        require(divisor != 0L) { "C_UInt16 modulus by zero" }
        return store(this.toLong() % divisor)
    }

    infix fun and(other: C_UInt16): C_UInt16 =
        store(this.toLong() and other.toLong())

    infix fun or(other: C_UInt16): C_UInt16 =
        store(this.toLong() or other.toLong())

    infix fun xor(other: C_UInt16): C_UInt16 =
        store(this.toLong() xor other.toLong())

    fun inv(): C_UInt16 = store(this.toLong().inv() and MASK_16)

    fun shiftLeft(bits: Int): C_UInt16 {
        require(bits in 0..15) { "C_UInt16 shift amount out of range: $bits" }
        return store(engine.leftShift(this.toLong(), bits).value)
    }

    fun shiftRight(bits: Int): C_UInt16 {
        require(bits in 0..15) { "C_UInt16 shift amount out of range: $bits" }
        return store(engine.unsignedRightShift(this.toLong(), bits).value)
    }

    fun copy(): C_UInt16 = alloc().also { GlobalHeap.sh(it.addr, GlobalHeap.lh(this.addr)) }

    private fun store(value: Long): C_UInt16 {
        val res = alloc()
        GlobalHeap.sh(res.addr, (value and MASK_16).toShort())
        return res
    }

    companion object {
        const val BYTES: Int = 2

        /** BitShiftEngine for 16-bit shifts. */
        private val engine = BitShiftEngine(BitShiftMode.NATIVE, 16)
        private val MASK_16: Long = engine.getMask(16)

        fun alloc(): C_UInt16 = C_UInt16(KMalloc.malloc(BYTES))
        fun zero(): C_UInt16 = alloc().also { GlobalHeap.sh(it.addr, 0) }
        fun one(): C_UInt16 = alloc().also { GlobalHeap.sh(it.addr, 1) }
        fun maxValue(): C_UInt16 = alloc().also { GlobalHeap.sh(it.addr, MASK_16.toShort()) }

        fun fromUShort(value: UShort): C_UInt16 =
            alloc().also { GlobalHeap.sh(it.addr, value.toShort()) }

        fun fromUInt(value: UInt): C_UInt16 =
            alloc().also { GlobalHeap.sh(it.addr, (value.toLong() and MASK_16).toShort()) }
    }
}

package ai.solace.klang.fp

import ai.solace.klang.bitwise.Float64Math

/**
 * CDouble: 64-bit IEEE-754 binary64 (C `double`).
 *
 * Format: 1 sign bit, 11 exponent bits, 52 mantissa bits
 * - Sign: bit 63
 * - Exponent: bits 62-52 (bias = 1023)
 * - Mantissa: bits 51-0
 *
 * Uses Float64Math for arithmetic operations.
 * Future: Use HPC16x8 for 128-bit intermediate precision.
 */
class CDouble private constructor(private val bits: Long) {
    
    val value: Double get() = Double.fromBits(bits)
    
    fun toDouble(): Double = value
    fun toFloat(): Float = Float.fromBits(Float64Math.toFloat32Bits(bits))
    fun toBits(): Long = bits
    
    // Unary operators
    operator fun unaryMinus(): CDouble = fromBits(Float64Math.negateBits(bits))
    
    // Arithmetic operators using Float64Math
    operator fun plus(other: CDouble): CDouble {
        return fromBits(Float64Math.addBits(this.bits, other.bits))
    }
    
    operator fun minus(other: CDouble): CDouble {
        return fromBits(Float64Math.subBits(this.bits, other.bits))
    }
    
    operator fun times(other: CDouble): CDouble {
        return fromBits(Float64Math.mulBits(this.bits, other.bits))
    }
    
    operator fun div(other: CDouble): CDouble {
        return fromBits(Float64Math.divBits(this.bits, other.bits))
    }
    
    // Comparison using Float64Math
    operator fun compareTo(other: CDouble): Int {
        return Float64Math.compareBits(this.bits, other.bits)
    }
    
    // String representation
    override fun toString(): String = value.toString()
    override fun equals(other: Any?): Boolean = other is CDouble && other.toBits() == bits
    override fun hashCode(): Int = bits.hashCode()
    
    companion object {
        // Special values
        val ZERO = CDouble(Float64Math.ZERO_BITS)
        val ONE = CDouble(Float64Math.ONE_BITS)
        val NaN = CDouble(Float64Math.NAN_BITS)
        val POSITIVE_INFINITY = CDouble(Float64Math.INF_BITS)
        val NEGATIVE_INFINITY = CDouble(Float64Math.NEG_INF_BITS)
        
        /**
         * Create CDouble from raw bits.
         */
        fun fromBits(bits: Long): CDouble = CDouble(bits)
        
        /**
         * Create CDouble from Double.
         */
        fun fromDouble(value: Double): CDouble = CDouble(value.toRawBits())
        
        /**
         * Create from Float using proper widening conversion.
         */
        fun fromFloat(value: Float): CDouble {
            val f64bits = Float64Math.fromFloat32Bits(value.toRawBits())
            return CDouble(f64bits)
        }
        
        /**
         * Create from Int.
         */
        fun fromInt(value: Int): CDouble = fromDouble(value.toDouble())
        
        /**
         * Create from Long.
         */
        fun fromLong(value: Long): CDouble = fromDouble(value.toDouble())
    }
}

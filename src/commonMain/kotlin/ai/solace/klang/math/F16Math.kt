/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

import ai.solace.klang.bitwise.Float16Math
import ai.solace.klang.mem.GlobalHeap

/**
 * F16Math: Heap-native math operations for IEEE-754 binary16 (half precision).
 * 
 * Uses Float16Math for bit-exact 16-bit float operations.
 * All functions operate directly on heap memory addresses.
 */
object F16Math {
    
    // ==================== Basic Operations ====================
    
    /**
     * Absolute value: |x|
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun abs(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lh(srcAddr).toInt() and 0xFFFF
        val result = Float16Math.absBits(bits)
        GlobalHeap.sh(destAddr, result.toShort())
    }
    
    /**
     * Square root (via float32 conversion)
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun sqrt(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lh(srcAddr).toInt() and 0xFFFF
        val f32bits = Float16Math.toFloat32Bits(bits)
        val f32 = Float.fromBits(f32bits)
        val sqrtF32 = kotlin.math.sqrt(f32)
        val resultBits = Float16Math.fromFloat32Bits(sqrtF32.toRawBits())
        GlobalHeap.sh(destAddr, resultBits.toShort())
    }
    
    /**
     * Copy sign from y to x: |x| * sign(y)
     * @param destAddr Destination heap address
     * @param xAddr Magnitude source address
     * @param yAddr Sign source address
     */
    fun copysign(destAddr: Int, xAddr: Int, yAddr: Int) {
        val xBits = GlobalHeap.lh(xAddr).toInt() and 0xFFFF
        val yBits = GlobalHeap.lh(yAddr).toInt() and 0xFFFF
        val result = (xBits and 0x7FFF) or (yBits and 0x8000)
        GlobalHeap.sh(destAddr, result.toShort())
    }
    
    // ==================== Classification ====================
    
    /**
     * Check if value is NaN
     * @param addr Heap address to check
     * @return true if NaN
     */
    fun isNaN(addr: Int): Boolean {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        val exp = (bits ushr 10) and 0x1F
        val frac = bits and 0x3FF
        return exp == 0x1F && frac != 0
    }
    
    /**
     * Check if value is infinite
     * @param addr Heap address to check
     * @return true if +Inf or -Inf
     */
    fun isInf(addr: Int): Boolean {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        val exp = (bits ushr 10) and 0x1F
        val frac = bits and 0x3FF
        return exp == 0x1F && frac == 0
    }
    
    /**
     * Check if value is finite (not NaN, not Inf)
     * @param addr Heap address to check
     * @return true if finite
     */
    fun isFinite(addr: Int): Boolean {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        val exp = (bits ushr 10) and 0x1F
        return exp != 0x1F
    }
    
    /**
     * Check if value is zero (+0.0 or -0.0)
     * @param addr Heap address to check
     * @return true if zero
     */
    fun isZero(addr: Int): Boolean {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        return (bits and 0x7FFF) == 0
    }
    
    /**
     * Check if value is subnormal (denormalized)
     * @param addr Heap address to check
     * @return true if subnormal
     */
    fun isSubnormal(addr: Int): Boolean {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        val exp = (bits ushr 10) and 0x1F
        val frac = bits and 0x3FF
        return exp == 0 && frac != 0
    }
    
    /**
     * Check if value is normal (not zero, subnormal, infinite, or NaN)
     * @param addr Heap address to check
     * @return true if normal
     */
    fun isNormal(addr: Int): Boolean {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        val exp = (bits ushr 10) and 0x1F
        return exp != 0 && exp != 0x1F
    }
    
    /**
     * Extract sign bit (0 for positive/+0, 1 for negative/-0)
     * @param addr Heap address to check
     * @return 0 or 1
     */
    fun signbit(addr: Int): Int {
        val bits = GlobalHeap.lh(addr).toInt() and 0xFFFF
        return (bits ushr 15) and 1
    }
    
    // ==================== Comparison ====================
    
    /**
     * Maximum of two values (NaN-aware)
     * @param destAddr Destination heap address
     * @param aAddr First value address
     * @param bAddr Second value address
     */
    fun max(destAddr: Int, aAddr: Int, bAddr: Int) {
        val aBits = GlobalHeap.lh(aAddr).toInt() and 0xFFFF
        val bBits = GlobalHeap.lh(bAddr).toInt() and 0xFFFF
        
        // NaN check
        if (isNaN(aAddr) || isNaN(bAddr)) {
            GlobalHeap.sh(destAddr, aBits.toShort())
            return
        }
        
        val cmp = Float16Math.compareBits(aBits, bBits)
        val result = if (cmp >= 0) aBits else bBits
        GlobalHeap.sh(destAddr, result.toShort())
    }
    
    /**
     * Minimum of two values (NaN-aware)
     * @param destAddr Destination heap address
     * @param aAddr First value address
     * @param bAddr Second value address
     */
    fun min(destAddr: Int, aAddr: Int, bAddr: Int) {
        val aBits = GlobalHeap.lh(aAddr).toInt() and 0xFFFF
        val bBits = GlobalHeap.lh(bAddr).toInt() and 0xFFFF
        
        // NaN check
        if (isNaN(aAddr) || isNaN(bAddr)) {
            GlobalHeap.sh(destAddr, aBits.toShort())
            return
        }
        
        val cmp = Float16Math.compareBits(aBits, bBits)
        val result = if (cmp <= 0) aBits else bBits
        GlobalHeap.sh(destAddr, result.toShort())
    }
    
    /**
     * Positive difference: max(x - y, 0)
     * @param destAddr Destination heap address
     * @param xAddr First value address
     * @param yAddr Second value address
     */
    fun dim(destAddr: Int, xAddr: Int, yAddr: Int) {
        val xBits = GlobalHeap.lh(xAddr).toInt() and 0xFFFF
        val yBits = GlobalHeap.lh(yAddr).toInt() and 0xFFFF
        val diffBits = Float16Math.subBits(xBits, yBits)
        
        // Check if positive
        val isPositive = (diffBits and 0x8000) == 0 && (diffBits and 0x7FFF) != 0
        val result = if (isPositive) diffBits else 0
        GlobalHeap.sh(destAddr, result.toShort())
    }
    
    // ==================== Rounding (via float32) ====================
    
    /**
     * Floor: largest integer <= x
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun floor(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lh(srcAddr).toInt() and 0xFFFF
        val f32bits = Float16Math.toFloat32Bits(bits)
        val value = Float.fromBits(f32bits)
        val result = kotlin.math.floor(value)
        val resultBits = Float16Math.fromFloat32Bits(result.toRawBits())
        GlobalHeap.sh(destAddr, resultBits.toShort())
    }
    
    /**
     * Ceiling: smallest integer >= x
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun ceil(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lh(srcAddr).toInt() and 0xFFFF
        val f32bits = Float16Math.toFloat32Bits(bits)
        val value = Float.fromBits(f32bits)
        val result = kotlin.math.ceil(value)
        val resultBits = Float16Math.fromFloat32Bits(result.toRawBits())
        GlobalHeap.sh(destAddr, resultBits.toShort())
    }
    
    /**
     * Truncate towards zero
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun trunc(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lh(srcAddr).toInt() and 0xFFFF
        val f32bits = Float16Math.toFloat32Bits(bits)
        val value = Float.fromBits(f32bits)
        val result = kotlin.math.truncate(value)
        val resultBits = Float16Math.fromFloat32Bits(result.toRawBits())
        GlobalHeap.sh(destAddr, resultBits.toShort())
    }
    
    /**
     * Round to nearest integer (ties to even)
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun round(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lh(srcAddr).toInt() and 0xFFFF
        val f32bits = Float16Math.toFloat32Bits(bits)
        val value = Float.fromBits(f32bits)
        val result = kotlin.math.round(value)
        val resultBits = Float16Math.fromFloat32Bits(result.toRawBits())
        GlobalHeap.sh(destAddr, resultBits.toShort())
    }
    
    // ==================== Vector Operations ====================
    
    /**
     * Vector absolute value
     * @param destAddr Destination array start address
     * @param srcAddr Source array start address
     * @param count Number of elements
     */
    fun abs_vec(destAddr: Int, srcAddr: Int, count: Int) {
        for (i in 0 until count) {
            val offset = i * 2  // 2 bytes per float16
            abs(destAddr + offset, srcAddr + offset)
        }
    }
    
    /**
     * Vector square root
     * @param destAddr Destination array start address
     * @param srcAddr Source array start address
     * @param count Number of elements
     */
    fun sqrt_vec(destAddr: Int, srcAddr: Int, count: Int) {
        for (i in 0 until count) {
            val offset = i * 2
            sqrt(destAddr + offset, srcAddr + offset)
        }
    }
    
    /**
     * Vector maximum
     * @param destAddr Destination array start address
     * @param aAddr First array start address
     * @param bAddr Second array start address
     * @param count Number of elements
     */
    fun max_vec(destAddr: Int, aAddr: Int, bAddr: Int, count: Int) {
        for (i in 0 until count) {
            val offset = i * 2
            max(destAddr + offset, aAddr + offset, bAddr + offset)
        }
    }
    
    /**
     * Vector minimum
     * @param destAddr Destination array start address
     * @param aAddr First array start address
     * @param bAddr Second array start address
     * @param count Number of elements
     */
    fun min_vec(destAddr: Int, aAddr: Int, bAddr: Int, count: Int) {
        for (i in 0 until count) {
            val offset = i * 2
            min(destAddr + offset, aAddr + offset, bAddr + offset)
        }
    }
}

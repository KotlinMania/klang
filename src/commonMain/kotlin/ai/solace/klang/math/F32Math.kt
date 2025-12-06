/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

import ai.solace.klang.fp.Float32Math
import ai.solace.klang.mem.GlobalHeap
import kotlin.math.abs as kotlinAbs

/**
 * F32Math: Heap-native math operations for 32-bit floats.
 * 
 * Uses Float32Math for deterministic bit-exact operations across platforms.
 * All functions operate directly on heap memory addresses.
 */
object F32Math {
    
    // ==================== Basic Operations ====================
    
    /**
     * Absolute value: |x|
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun abs(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lw(srcAddr)
        val result = bits and 0x7FFFFFFF  // Clear sign bit
        GlobalHeap.sw(destAddr, result)
    }
    
    /**
     * Square root
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun sqrt(destAddr: Int, srcAddr: Int) {
        val bits = GlobalHeap.lw(srcAddr)
        val result = Float32Math.sqrtBits(bits)
        GlobalHeap.sw(destAddr, result)
    }
    
    /**
     * Copy sign from y to x: |x| * sign(y)
     * @param destAddr Destination heap address
     * @param xAddr Magnitude source address
     * @param yAddr Sign source address
     */
    fun copysign(destAddr: Int, xAddr: Int, yAddr: Int) {
        val xBits = GlobalHeap.lw(xAddr)
        val yBits = GlobalHeap.lw(yAddr)
        val result = (xBits and 0x7FFFFFFF) or (yBits and 0x80000000.toInt())
        GlobalHeap.sw(destAddr, result)
    }
    
    // ==================== Classification ====================
    
    /**
     * Check if value is NaN
     * @param addr Heap address to check
     * @return true if NaN
     */
    fun isNaN(addr: Int): Boolean {
        val bits = GlobalHeap.lw(addr)
        val exp = (bits ushr 23) and 0xFF
        val frac = bits and 0x7FFFFF
        return exp == 0xFF && frac != 0
    }
    
    /**
     * Check if value is infinite
     * @param addr Heap address to check
     * @return true if +Inf or -Inf
     */
    fun isInf(addr: Int): Boolean {
        val bits = GlobalHeap.lw(addr)
        val exp = (bits ushr 23) and 0xFF
        val frac = bits and 0x7FFFFF
        return exp == 0xFF && frac == 0
    }
    
    /**
     * Check if value is finite (not NaN, not Inf)
     * @param addr Heap address to check
     * @return true if finite
     */
    fun isFinite(addr: Int): Boolean {
        val bits = GlobalHeap.lw(addr)
        val exp = (bits ushr 23) and 0xFF
        return exp != 0xFF
    }
    
    /**
     * Check if value is zero (+0.0 or -0.0)
     * @param addr Heap address to check
     * @return true if zero
     */
    fun isZero(addr: Int): Boolean {
        val bits = GlobalHeap.lw(addr)
        return (bits and 0x7FFFFFFF) == 0
    }
    
    /**
     * Check if value is subnormal (denormalized)
     * @param addr Heap address to check
     * @return true if subnormal
     */
    fun isSubnormal(addr: Int): Boolean {
        val bits = GlobalHeap.lw(addr)
        val exp = (bits ushr 23) and 0xFF
        val frac = bits and 0x7FFFFF
        return exp == 0 && frac != 0
    }
    
    /**
     * Check if value is normal (not zero, subnormal, infinite, or NaN)
     * @param addr Heap address to check
     * @return true if normal
     */
    fun isNormal(addr: Int): Boolean {
        val bits = GlobalHeap.lw(addr)
        val exp = (bits ushr 23) and 0xFF
        return exp != 0 && exp != 0xFF
    }
    
    /**
     * Extract sign bit (0 for positive/+0, 1 for negative/-0)
     * @param addr Heap address to check
     * @return 0 or 1
     */
    fun signbit(addr: Int): Int {
        val bits = GlobalHeap.lw(addr)
        return (bits ushr 31) and 1
    }
    
    // ==================== Comparison ====================
    
    /**
     * Maximum of two values (NaN-aware)
     * Returns first argument if either is NaN
     * @param destAddr Destination heap address
     * @param aAddr First value address
     * @param bAddr Second value address
     */
    fun max(destAddr: Int, aAddr: Int, bAddr: Int) {
        val a = GlobalHeap.lwf(aAddr)
        val b = GlobalHeap.lwf(bAddr)
        val result = when {
            a.isNaN() -> a
            b.isNaN() -> a
            a >= b -> a
            else -> b
        }
        GlobalHeap.swf(destAddr, result)
    }
    
    /**
     * Minimum of two values (NaN-aware)
     * Returns first argument if either is NaN
     * @param destAddr Destination heap address
     * @param aAddr First value address
     * @param bAddr Second value address
     */
    fun min(destAddr: Int, aAddr: Int, bAddr: Int) {
        val a = GlobalHeap.lwf(aAddr)
        val b = GlobalHeap.lwf(bAddr)
        val result = when {
            a.isNaN() -> a
            b.isNaN() -> a
            a <= b -> a
            else -> b
        }
        GlobalHeap.swf(destAddr, result)
    }
    
    /**
     * Positive difference: max(x - y, 0)
     * @param destAddr Destination heap address
     * @param xAddr First value address
     * @param yAddr Second value address
     */
    fun dim(destAddr: Int, xAddr: Int, yAddr: Int) {
        val xBits = GlobalHeap.lw(xAddr)
        val yBits = GlobalHeap.lw(yAddr)
        val diffBits = Float32Math.subBits(xBits, yBits)
        val diff = Float.fromBits(diffBits)
        val result = if (diff > 0.0f) diff else 0.0f
        GlobalHeap.swf(destAddr, result)
    }
    
    // ==================== Rounding (to be implemented with BSD libm) ====================
    
    /**
     * Floor: largest integer <= x
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun floor(destAddr: Int, srcAddr: Int) {
        val value = GlobalHeap.lwf(srcAddr)
        GlobalHeap.swf(destAddr, kotlin.math.floor(value))
    }
    
    /**
     * Ceiling: smallest integer >= x
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun ceil(destAddr: Int, srcAddr: Int) {
        val value = GlobalHeap.lwf(srcAddr)
        GlobalHeap.swf(destAddr, kotlin.math.ceil(value))
    }
    
    /**
     * Truncate towards zero
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun trunc(destAddr: Int, srcAddr: Int) {
        val value = GlobalHeap.lwf(srcAddr)
        GlobalHeap.swf(destAddr, kotlin.math.truncate(value))
    }
    
    /**
     * Round to nearest integer (ties to even)
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun round(destAddr: Int, srcAddr: Int) {
        val value = GlobalHeap.lwf(srcAddr)
        GlobalHeap.swf(destAddr, kotlin.math.round(value))
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
            val offset = i * 4  // 4 bytes per float
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
            val offset = i * 4
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
            val offset = i * 4
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
            val offset = i * 4
            min(destAddr + offset, aAddr + offset, bAddr + offset)
        }
    }
}

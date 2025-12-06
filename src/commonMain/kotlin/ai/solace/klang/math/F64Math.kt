/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.math

import ai.solace.klang.mem.GlobalHeap

/**
 * F64Math: Heap-native math operations for 64-bit doubles.
 * 
 * All functions operate directly on heap memory addresses, enabling:
 * - Zero-copy operations
 * - Pointer arithmetic
 * - Efficient bulk processing
 * - C-compatible memory layouts
 * 
 * Functions take destination and source addresses, reading from and writing
 * to heap memory without intermediate allocations.
 */
object F64Math {
    
    // ==================== Basic Operations ====================
    
    /**
     * Absolute value: |x|
     * @param destAddr Destination heap address
     * @param srcAddr Source heap address
     */
    fun abs(destAddr: Int, srcAddr: Int) {
        val value = GlobalHeap.ldf(srcAddr)
        GlobalHeap.sdf(destAddr, KMath.fabs(value))
    }
    
    /**
     * Copy sign from y to x: |x| * sign(y)
     * @param destAddr Destination heap address
     * @param xAddr Magnitude source address
     * @param yAddr Sign source address
     */
    fun copysign(destAddr: Int, xAddr: Int, yAddr: Int) {
        val x = GlobalHeap.ldf(xAddr)
        val y = GlobalHeap.ldf(yAddr)
        GlobalHeap.sdf(destAddr, KMath.copysign(x, y))
    }
    
    // ==================== Classification ====================
    
    /**
     * Check if value is NaN
     * @param addr Heap address to check
     * @return true if NaN
     */
    fun isNaN(addr: Int): Boolean {
        return Classification.isnan(GlobalHeap.ldf(addr))
    }
    
    /**
     * Check if value is infinite
     * @param addr Heap address to check
     * @return true if +Inf or -Inf
     */
    fun isInf(addr: Int): Boolean {
        return Classification.isinf(GlobalHeap.ldf(addr))
    }
    
    /**
     * Check if value is finite (not NaN, not Inf)
     * @param addr Heap address to check
     * @return true if finite
     */
    fun isFinite(addr: Int): Boolean {
        return Classification.isfinite(GlobalHeap.ldf(addr))
    }
    
    /**
     * Check if value is zero (+0.0 or -0.0)
     * @param addr Heap address to check
     * @return true if zero
     */
    fun isZero(addr: Int): Boolean {
        return Classification.iszero(GlobalHeap.ldf(addr))
    }
    
    /**
     * Check if value is subnormal (denormalized)
     * @param addr Heap address to check
     * @return true if subnormal
     */
    fun isSubnormal(addr: Int): Boolean {
        return Classification.issubnormal(GlobalHeap.ldf(addr))
    }
    
    /**
     * Check if value is normal (not zero, subnormal, infinite, or NaN)
     * @param addr Heap address to check
     * @return true if normal
     */
    fun isNormal(addr: Int): Boolean {
        return Classification.isnormal(GlobalHeap.ldf(addr))
    }
    
    /**
     * Extract sign bit (0 for positive/+0, 1 for negative/-0)
     * @param addr Heap address to check
     * @return 0 or 1
     */
    fun signbit(addr: Int): Int {
        return if (KMath.signbit(GlobalHeap.ldf(addr))) 1 else 0
    }
    
    // ==================== Comparison ====================
    
    /**
     * Maximum of two values (NaN-aware)
     * @param destAddr Destination heap address
     * @param aAddr First value address
     * @param bAddr Second value address
     */
    fun max(destAddr: Int, aAddr: Int, bAddr: Int) {
        val a = GlobalHeap.ldf(aAddr)
        val b = GlobalHeap.ldf(bAddr)
        GlobalHeap.sdf(destAddr, Comparison.fmax(a, b))
    }
    
    /**
     * Minimum of two values (NaN-aware)
     * @param destAddr Destination heap address
     * @param aAddr First value address
     * @param bAddr Second value address
     */
    fun min(destAddr: Int, aAddr: Int, bAddr: Int) {
        val a = GlobalHeap.ldf(aAddr)
        val b = GlobalHeap.ldf(bAddr)
        GlobalHeap.sdf(destAddr, Comparison.fmin(a, b))
    }
    
    /**
     * Positive difference: max(x - y, 0)
     * @param destAddr Destination heap address
     * @param xAddr First value address
     * @param yAddr Second value address
     */
    fun dim(destAddr: Int, xAddr: Int, yAddr: Int) {
        val x = GlobalHeap.ldf(xAddr)
        val y = GlobalHeap.ldf(yAddr)
        GlobalHeap.sdf(destAddr, Comparison.fdim(x, y))
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
            val offset = i * 8  // 8 bytes per double
            abs(destAddr + offset, srcAddr + offset)
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
            val offset = i * 8
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
            val offset = i * 8
            min(destAddr + offset, aAddr + offset, bAddr + offset)
        }
    }
}

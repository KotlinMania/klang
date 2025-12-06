/*
 * Copyright (c) 2024 KLang Contributors
 * SPDX-License-Identifier: BSD-2-Clause
 */

package ai.solace.klang.fp

import ai.solace.klang.mem.CAutos
import ai.solace.klang.mem.CGlobals
import ai.solace.klang.mem.CHeapVars
import ai.solace.klang.mem.GlobalData
import ai.solace.klang.mem.KStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for ML-oriented types (CFloat16Var, CBF16Var).
 * 
 * Tests cover:
 * - Heap-backed storage and retrieval
 * - Stack allocation with CAutos
 * - Global/static allocation with CGlobals
 * - Heap allocation with CHeapVars
 * - Round-trip conversions
 * - Precision characteristics
 * 
 * @since 0.2.0
 */
class MLTypesTest {
    
    @Test
    fun testCFloat16VarStackStorage() {
        KStack.init()
        
        KStack.withFrame {
            // Create float16 on stack
            val half = CAutos.float16(CFloat16.fromFloat(1.5f))
            
            // Verify initial value
            assertEquals(1.5f, half.value.toFloat())
            
            // Modify value
            half.value = CFloat16.fromFloat(2.5f)
            assertEquals(2.5f, half.value.toFloat())
            
            // Test special values
            half.value = CFloat16.ZERO
            assertEquals(0.0f, half.value.toFloat())
            
            half.value = CFloat16.ONE
            assertEquals(1.0f, half.value.toFloat())
        }
        
        KStack.reset()
    }
    
    @Test
    fun testCBF16VarStackStorage() {
        KStack.init()
        
        KStack.withFrame {
            // Create bfloat16 on stack
            val bf = CAutos.bfloat16(CBF16.fromFloat(3.14159f))
            
            // Verify initial value (bfloat16 has reduced precision)
            val retrieved = bf.value.toFloat()
            assertTrue((retrieved - 3.14f).let { it * it } < 0.01f, "bfloat16 should be ~3.14")
            
            // Modify value
            bf.value = CBF16.fromFloat(2.71828f)
            val retrieved2 = bf.value.toFloat()
            assertTrue((retrieved2 - 2.72f).let { it * it } < 0.01f, "bfloat16 should be ~2.72")
        }
        
        KStack.reset()
    }
    
    @Test
    fun testCFloat16VarGlobalStorage() {
        GlobalData.init()
        
        // Define global float16
        val g1 = CGlobals.float16("ml_weight", CFloat16.fromFloat(0.5f))
        assertEquals(0.5f, g1.value.toFloat())
        
        // Modify global
        g1.value = CFloat16.fromFloat(0.75f)
        assertEquals(0.75f, g1.value.toFloat())
        
        // Access same global again (should persist)
        val g2 = CGlobals.float16("ml_weight2", CFloat16.fromFloat(0.25f))
        assertEquals(0.25f, g2.value.toFloat())
        
        
    }
    
    @Test
    fun testCBF16VarGlobalStorage() {
        GlobalData.init()
        
        // Define global bfloat16
        val g = CGlobals.bfloat16("model_param", CBF16.fromFloat(1.0f))
        assertEquals(1.0f, g.value.toFloat())
        
        // Test update
        g.value = CBF16.fromFloat(-1.0f)
        assertEquals(-1.0f, g.value.toFloat())
        
        
    }
    
    @Test
    fun testCFloat16VarHeapStorage() {
        // Allocate float16 on heap
        val h = CHeapVars.float16(CFloat16.fromFloat(1.5f))
        
        assertEquals(1.5f, h.value.toFloat())
        
        // Modify
        h.value = CFloat16.fromFloat(2.5f)
        assertEquals(2.5f, h.value.toFloat())
        
        // Free
        CHeapVars.free(h)
    }
    
    @Test
    fun testCBF16VarHeapStorage() {
        // Allocate bfloat16 on heap
        val h = CHeapVars.bfloat16(CBF16.fromFloat(3.14159f))
        
        val retrieved = h.value.toFloat()
        assertTrue((retrieved - 3.14f).let { it * it } < 0.01f)
        
        // Free
        CHeapVars.free(h)
    }
    
    @Test
    fun testCFloat16PrecisionCharacteristics() {
        KStack.init()
        
        KStack.withFrame {
            // Test precision limits
            val h = CAutos.float16()
            
            // Max value: 65504
            h.value = CFloat16.fromFloat(65504.0f)
            assertEquals(65504.0f, h.value.toFloat())
            
            // Min positive normal: ~6.1e-5
            h.value = CFloat16.fromFloat(0.00006103515625f)
            assertTrue(h.value.toFloat() > 0.0f)
            
            // Test that precision is limited (~3.3 decimal digits)
            h.value = CFloat16.fromFloat(1.234f)
            val retrieved = h.value.toFloat()
            // Should be close but not exact
            assertTrue((retrieved - 1.234f).let { kotlin.math.abs(it) } < 0.001f)
        }
        
        KStack.reset()
    }
    
    @Test
    fun testCBF16ExponentRange() {
        KStack.init()
        
        KStack.withFrame {
            // bfloat16 has same exponent range as float32
            val bf = CAutos.bfloat16()
            
            // Large value (within float32 range)
            bf.value = CBF16.fromFloat(1e10f)
            assertTrue(bf.value.toFloat() > 1e9f)
            
            // Small value
            bf.value = CBF16.fromFloat(1e-10f)
            assertTrue(bf.value.toFloat() < 1e-9f)
            
            // Test negative
            bf.value = CBF16.fromFloat(-123.456f)
            assertTrue(bf.value.toFloat() < -123.0f)
        }
        
        KStack.reset()
    }
    
    @Test
    fun testMultipleFloat16Variables() {
        KStack.init()
        
        KStack.withFrame {
            // Allocate multiple float16 variables
            val a = CAutos.float16(CFloat16.fromFloat(1.0f))
            val b = CAutos.float16(CFloat16.fromFloat(2.0f))
            val c = CAutos.float16(CFloat16.fromFloat(3.0f))
            
            // Verify they're independent
            assertEquals(1.0f, a.value.toFloat())
            assertEquals(2.0f, b.value.toFloat())
            assertEquals(3.0f, c.value.toFloat())
            
            // Modify one
            b.value = CFloat16.fromFloat(10.0f)
            
            // Others should be unchanged
            assertEquals(1.0f, a.value.toFloat())
            assertEquals(10.0f, b.value.toFloat())
            assertEquals(3.0f, c.value.toFloat())
        }
        
        KStack.reset()
    }
    
    @Test
    fun testMLTypesUsagePattern() {
        KStack.init()
        
        // Simulate ML inference pattern: weights in global, activations on stack
        GlobalData.init()
        
        // Define model weights as globals (persistent)
        val weight1 = CGlobals.float16("layer1_w1", CFloat16.fromFloat(0.5f))
        val weight2 = CGlobals.float16("layer1_w2", CFloat16.fromFloat(0.3f))
        
        KStack.withFrame {
            // Allocate activations on stack (temporary)
            val input = CAutos.float16(CFloat16.fromFloat(1.0f))
            val hidden = CAutos.bfloat16() // Intermediate in bfloat16
            val output = CAutos.float16()
            
            // Simulate simple computation: hidden = input * weight1
            val inputVal = input.value.toFloat()
            val w1Val = weight1.value.toFloat()
            hidden.value = CBF16.fromFloat(inputVal * w1Val)
            
            // output = hidden * weight2
            val hiddenVal = hidden.value.toFloat()
            val w2Val = weight2.value.toFloat()
            output.value = CFloat16.fromFloat(hiddenVal * w2Val)
            
            // Verify computation
            val expected = 1.0f * 0.5f * 0.3f // 0.15
            val actual = output.value.toFloat()
            assertTrue((actual - expected).let { kotlin.math.abs(it) } < 0.01f)
        }
        
        KStack.reset()
    }
}

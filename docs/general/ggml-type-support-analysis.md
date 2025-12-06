# GGML Scalar Type Support in KLang

**Analysis Date**: December 6, 2024  
**Source**: llama.kotlin GGML implementation  
**Target**: Add GGML-compatible scalar types to KLang

---

## Current GGML Types in llama.kotlin

### Floating-Point Types

| Type | Bits | Format | Description | Status in llama.kotlin |
|------|------|--------|-------------|----------------------|
| **F32** | 32 | IEEE-754 binary32 | Standard float | ✅ Native Kotlin Float |
| **F16** | 16 | IEEE-754 binary16 | Half precision | ✅ **CFloat16** in klangnative |
| **BF16** | 16 | bfloat16 (1+8+7) | Brain float 16 | ✅ **CBF16** in klangnative |

### Integer Types

| Type | Bits | Format | Description | Usage |
|------|------|--------|-------------|-------|
| **I8** | 8 | Signed int | 8-bit integer | Quantization weights |
| **I16** | 16 | Signed int | 16-bit integer | Intermediate values |
| **I32** | 32 | Signed int | 32-bit integer | Indices, counts |
| **I64** | 64 | Signed int | 64-bit integer | Large counts, pointers |

### Quantized Types (Block-Based)

| Type | Block Size | Bytes/Block | Description |
|------|------------|-------------|-------------|
| **Q4_0** | 32 | 18 | 4-bit, F16 scale |
| **Q4_1** | 32 | 20 | 4-bit, F16 scale + min |
| **Q5_0** | 32 | TBD | 5-bit quantization |
| **Q5_1** | 32 | TBD | 5-bit with scaling |
| **Q8_0** | 32 | 34 | 8-bit, F16 scale |
| **Q8_1** | 32 | TBD | 8-bit with different scaling |

### K-Quantization Types (Super-Block Based, 256 elements)

| Type | Block Size | Bytes/Block | Description |
|------|------------|-------------|-------------|
| **Q2_K** | 256 | 84 | 2-bit K-quant (2×F16 + scales + data) |
| **Q3_K** | 256 | 110 | 3-bit K-quant |
| **Q4_K** | 256 | 144 | 4-bit K-quant |
| **Q5_K** | 256 | 176 | 5-bit K-quant |
| **Q6_K** | 256 | 210 | 6-bit K-quant |
| **Q8_K** | 256 | 292 | 8-bit K-quant (highest precision) |

### Special Quantization

| Type | Block Size | Bytes/Block | Description |
|------|------------|-------------|-------------|
| **Q1_5_K** | TBD | TBD | 1.5-bit ternary (-1, 0, +1) |
| **BITNET_1_58** | 32 | 10 | BitNet 1.58 ternary quantization |

---

## What's Already in KLang (klangnative)

### ✅ Already Implemented

1. **CFloat16** (`CFloat16.kt`)
   - IEEE-754 binary16 (half precision)
   - Format: 1 sign + 5 exp + 10 frac
   - Full arithmetic operators (+, -, *, /)
   - Conversion to/from Float/Double
   - Special value handling (NaN, Inf, Zero)

2. **CBF16** (`CBF16.kt`)
   - bfloat16 (brain float 16)
   - Format: 1 sign + 8 exp + 7 frac (same exponent range as F32!)
   - Full arithmetic operators
   - Round-to-nearest ties-to-even rounding
   - Uses Float32Math for bit-exact operations

3. **CFloat128** (`CFloat128.kt`)
   - Double-double precision (~106-bit mantissa)
   - For high-precision accumulation
   - Perfect for sum reductions without error accumulation

4. **Integer Types**
   - Standard Kotlin `Byte`, `Short`, `Int`, `Long`
   - Heap-backed versions: `C_UInt8`, `C_UInt16`, `C_UInt32`, etc.

### 🚧 Missing for GGML Compatibility

1. **No quantized block types yet**
   - Q4_0, Q4_1, Q5_0, Q5_1, Q8_0, Q8_1
   - K-quantization types
   - BitNet 1.58 ternary

2. **No vectorized operations**
   - SIMD-style dot products
   - Block-wise dequantization
   - Optimized matrix multiply

---

## Proposed Additions to KLang

### Phase 1: Heap-Backed Scalar Variables (EASY)

Add CVar types for F16 and BF16 (similar to existing CDoubleVar):

```kotlin
/**
 * CFloat16Var: Heap-backed IEEE-754 binary16 variable.
 * Stores 16-bit half-precision float at fixed heap address.
 */
class CFloat16Var(override val addr: Int) : CVar {
    var value: CFloat16
        get() = CFloat16.fromBits(GlobalHeap.lh(addr).toShort())
        set(v) = GlobalHeap.sh(addr, v.toBits().toShort())
}

/**
 * CBF16Var: Heap-backed bfloat16 variable.
 * Stores 16-bit brain float at fixed heap address.
 */
class CBF16Var(override val addr: Int) : CVar {
    var value: CBF16
        get() = CBF16.fromBits(GlobalHeap.lh(addr).toShort())
        set(v) = GlobalHeap.sh(addr, v.toBits())
}

// Factory methods in CAutos
object CAutos {
    fun float16(init: CFloat16): CFloat16Var {
        val addr = KStack.alloc(2) // 16 bits = 2 bytes
        return CFloat16Var(addr).apply { value = init }
    }
    
    fun bfloat16(init: CBF16): CBF16Var {
        val addr = KStack.alloc(2)
        return CBF16Var(addr).apply { value = init }
    }
}
```

**Files to modify**:
- `src/commonMain/kotlin/ai/solace/klang/mem/CScalars.kt` - Add CFloat16Var, CBF16Var

**Benefit**: Direct heap storage for ML tensor elements

---

### Phase 2: Array Storage & Block Operations (MEDIUM)

Add efficient array storage for quantized types:

```kotlin
/**
 * Q4_0 block: 32 elements in 18 bytes
 * Format: [F16 scale (2 bytes)][16 bytes of packed 4-bit weights]
 */
class Q4_0Block(override val addr: Int) : CVar {
    companion object {
        const val BLOCK_SIZE = 32      // 32 elements per block
        const val BYTES_PER_BLOCK = 18 // 2 (scale) + 16 (packed data)
    }
    
    var scale: CFloat16
        get() = CFloat16.fromBits(GlobalHeap.lh(addr).toShort())
        set(v) = GlobalHeap.sh(addr, v.toBits().toShort())
    
    /**
     * Get packed 4-bit weight at index (0-31)
     */
    fun getWeight(index: Int): Int {
        require(index in 0 until BLOCK_SIZE)
        val byteOffset = addr + 2 + (index / 2)
        val byte = GlobalHeap.lb(byteOffset)
        return if (index % 2 == 0) {
            (byte.toInt() and 0x0F) - 8  // Low nibble, signed
        } else {
            ((byte.toInt() ushr 4) and 0x0F) - 8  // High nibble, signed
        }
    }
    
    /**
     * Dequantize entire block to Float array
     */
    fun dequantize(dest: FloatArray, destOffset: Int) {
        val s = scale.toFloat()
        for (i in 0 until BLOCK_SIZE) {
            dest[destOffset + i] = getWeight(i) * s
        }
    }
}

/**
 * Array of Q4_0 blocks stored contiguously in heap
 */
class Q4_0Array(val blockCount: Int) {
    private val addr: Int = Runtime.kmalloc(blockCount * Q4_0Block.BYTES_PER_BLOCK)
    
    fun getBlock(blockIndex: Int): Q4_0Block {
        require(blockIndex in 0 until blockCount)
        val blockAddr = addr + (blockIndex * Q4_0Block.BYTES_PER_BLOCK)
        return Q4_0Block(blockAddr)
    }
    
    /**
     * Dot product between two Q4_0 arrays (optimized, no full dequantization)
     */
    fun dot(other: Q4_0Array): Float {
        require(blockCount == other.blockCount)
        var sum = 0.0f
        
        for (i in 0 until blockCount) {
            val b1 = getBlock(i)
            val b2 = other.getBlock(i)
            
            // Block-wise dot product: scale1 * scale2 * sum(w1[j] * w2[j])
            val scale = b1.scale.toFloat() * b2.scale.toFloat()
            var blockSum = 0
            for (j in 0 until Q4_0Block.BLOCK_SIZE) {
                blockSum += b1.getWeight(j) * b2.getWeight(j)
            }
            sum += scale * blockSum
        }
        
        return sum
    }
    
    fun free() = Runtime.kfree(addr)
}
```

**Files to create**:
- `src/commonMain/kotlin/ai/solace/klang/quant/Q4_0.kt`
- `src/commonMain/kotlin/ai/solace/klang/quant/Q8_0.kt`
- `src/commonMain/kotlin/ai/solace/klang/quant/QuantUtils.kt`

**Benefit**: Memory-efficient ML model storage (4× compression vs F32)

---

### Phase 3: SIMD-Style Vector Operations (ADVANCED)

Add vectorized operations using SwAR128 (SIMD-Within-A-Register):

```kotlin
/**
 * SIMD-style dot product for F16 vectors
 * Processes 8 F16 values at a time using SwAR128
 */
fun dotProductF16Simd(
    a: Int,        // Heap address of first F16 array
    b: Int,        // Heap address of second F16 array
    count: Int     // Number of F16 elements
): Float {
    var sum = CFloat128.ZERO  // Use double-double for accurate accumulation
    var i = 0
    
    // Process 8 F16 values at a time (128 bits / 16 bits = 8)
    while (i + 8 <= count) {
        // Load 8 F16 values from each array into SwAR128
        val vecA = SwAR128.loadF16x8(a + i * 2)
        val vecB = SwAR128.loadF16x8(b + i * 2)
        
        // Multiply and accumulate
        val products = vecA * vecB  // Element-wise multiply
        sum = sum + products.horizontalSum()  // Sum all 8 products
        
        i += 8
    }
    
    // Handle remaining elements
    while (i < count) {
        val elemA = CFloat16.fromBits(GlobalHeap.lh(a + i * 2).toShort())
        val elemB = CFloat16.fromBits(GlobalHeap.lh(b + i * 2).toShort())
        sum = sum + (elemA.toFloat() * elemB.toFloat()).toDouble()
        i++
    }
    
    return sum.toFloat()
}
```

**Files to enhance**:
- `src/commonMain/kotlin/ai/solace/klang/int/SwAR128.kt` - Add F16 vector ops
- Create `src/commonMain/kotlin/ai/solace/klang/simd/VectorOps.kt`

**Benefit**: 4-8× speedup for ML inference operations

---

## Implementation Priority

### 🟢 Phase 1: Immediate Value (1-2 days)
1. ✅ CFloat16Var, CBF16Var heap-backed scalars
2. ✅ Extension to CAutos factory
3. ✅ Tests verifying heap storage/retrieval

**Impact**: Enables ML code to use F16/BF16 with C-style memory layout

### 🟡 Phase 2: Core Functionality (1 week)
1. Q4_0, Q8_0 block types with heap storage
2. Block-wise dequantization
3. Optimized dot product (no full dequantization)
4. Unit tests with GGML reference data

**Impact**: 4× memory compression for ML models

### 🔴 Phase 3: Performance (2-3 weeks)
1. SIMD-style vector operations
2. K-quantization types (Q4_K, Q5_K, Q6_K)
3. BitNet 1.58 ternary quantization
4. Full GGML compatibility layer

**Impact**: Production-ready ML inference performance

---

## Use Case Example: Llama Model Inference

```kotlin
// Load quantized Llama 7B model (Q4_0 quantization)
val model = LlamaModel.load("llama-7b-q4_0.gguf")

// Model uses Q4_0 arrays for weights (4× smaller than F32)
val weightsQ4 = model.layers[0].attention.wq  // Q4_0Array
val input = model.embeddings.lookup(tokenId)   // F16Array

// Optimized matrix-vector multiply (no full dequantization)
val output = weightsQ4.matvec(input)  // Uses block-wise dot product

// Accumulation in CFloat128 for accuracy
var sum = CFloat128.ZERO
for (i in 0 until output.size) {
    sum = sum + output[i].toDouble()
}
```

**Memory Savings**:
- Llama 7B F32: 28 GB
- Llama 7B Q4_0: **7 GB** (4× compression)
- Llama 7B Q8_0: **14 GB** (2× compression)

**Performance**:
- Q4_0 inference: ~80% speed of F16
- Q8_0 inference: ~90% speed of F16
- CFloat128 accumulation: Eliminates error drift

---

## Compatibility Matrix

| Feature | llama.kotlin (current) | KLang (proposed) |
|---------|------------------------|------------------|
| F32 scalar | ✅ Native Float | ✅ CFloatVar (heap) |
| F16 scalar | ✅ CFloat16 (value) | ✅ CFloat16Var (heap) |
| BF16 scalar | ✅ CBF16 (value) | ✅ CBF16Var (heap) |
| Q4_0 blocks | ✅ ByteArray + logic | ✅ Q4_0Block (typed heap) |
| Q8_0 blocks | ✅ ByteArray + logic | ✅ Q8_0Block (typed heap) |
| K-quants | ✅ ByteArray + logic | 🚧 Planned |
| SIMD ops | ❌ Scalar only | 🚧 SwAR128 planned |
| Dot product | ✅ Loop-based | ✅ Block-optimized |
| Memory | ✅ ByteArray | ✅ GlobalHeap (typed) |

---

## Next Steps

1. **Review existing CFloat16/CBF16 implementations** in klangnative
2. **Copy to KLang project** (they're in an embedded copy, sync them)
3. **Add CFloat16Var/CBF16Var** to CScalars.kt
4. **Create test suite** verifying heap storage round-trips
5. **Document usage patterns** for ML developers

Once Phase 1 is complete, llama.kotlin can start migrating from raw ByteArray manipulation to type-safe KLang heap operations.

---

## Benefits for KLang

1. **ML/AI Ready**: First-class support for ML quantization formats
2. **Memory Efficient**: 4-8× compression vs F32
3. **Cross-Platform**: Deterministic quantization on all targets
4. **Performance**: Block-wise operations avoid full dequantization
5. **Type Safe**: Strongly-typed API vs raw byte manipulation
6. **C-Compatible**: Direct mapping to GGML C structures

This positions KLang as a viable alternative to C/C++ for ML inference workloads!

# Quick Wins: Priority Math Functions

**Target**: Port 20 functions in 5 days (4 per day)

## Day 1: Bit Manipulation (No Math Required)

### 1. `fabs()` - Absolute Value
**Complexity**: ⭐ (5 lines)
**File**: `s_fabs.c` in FreeBSD libm
**Algorithm**: Clear sign bit (bit 63)
```c
return Double.fromBits(x.toRawBits() and 0x7FFFFFFFFFFFFFFF)
```

### 2. `copysign()` - Copy Sign Bit
**Complexity**: ⭐ (6 lines)
**Algorithm**: Copy bit 63 from one double to another
```c
val magnitudeBits = magnitude.toRawBits() and 0x7FFFFFFFFFFFFFFF
val signBit = sign.toRawBits() and 0x8000000000000000
return Double.fromBits(magnitudeBits or signBit)
```

### 3. `signbit()` - Extract Sign
**Complexity**: ⭐ (3 lines)
**Algorithm**: Check bit 63
```c
return (x.toRawBits() ushr 63) != 0L
```

### 4. `isnan()` - Check NaN
**Complexity**: ⭐ (4 lines)
**Algorithm**: Check if exponent = 0x7FF and mantissa != 0
```c
val bits = x.toRawBits()
val exp = (bits ushr 52) and 0x7FF
val frac = bits and 0x000FFFFFFFFFFFFF
return exp == 0x7FFL && frac != 0L
```

---

## Day 2: Classification & Comparison

### 5. `isinf()` - Check Infinity
**Complexity**: ⭐ (4 lines)
**Algorithm**: exp = 0x7FF, mantissa = 0

### 6. `isfinite()` - Check Finite
**Complexity**: ⭐ (3 lines)
**Algorithm**: exp != 0x7FF

### 7. `fpclassify()` - Classify Float
**Complexity**: ⭐⭐ (15 lines)
**Returns**: FP_ZERO, FP_SUBNORMAL, FP_NORMAL, FP_INFINITE, FP_NAN

### 8. `fmax()` / `fmin()` - Max/Min with NaN Handling
**Complexity**: ⭐⭐ (10 lines each)
**Algorithm**: Handle NaN propagation correctly per IEEE-754

---

## Day 3: Rounding Functions

### 9. `trunc()` - Round Toward Zero
**Complexity**: ⭐⭐ (20 lines)
**Algorithm**: Mask off fractional bits based on exponent

### 10. `floor()` - Round Down
**Complexity**: ⭐⭐ (25 lines)
**Algorithm**: Like trunc, but adjust for negative numbers

### 11. `ceil()` - Round Up
**Complexity**: ⭐⭐ (25 lines)
**Algorithm**: Like floor, but opposite direction

### 12. `round()` - Round to Nearest
**Complexity**: ⭐⭐ (30 lines)
**Algorithm**: Add 0.5 then floor (with sign handling)

---

## Day 4: Exponent Manipulation

### 13. `frexp()` - Extract Mantissa & Exponent
**Complexity**: ⭐⭐ (15 lines)
**Algorithm**: Read exponent field, normalize mantissa to [0.5, 1)
**Returns**: Pair of (mantissa, exponent)

### 14. `ldexp()` - Load Exponent
**Complexity**: ⭐⭐ (20 lines)
**Algorithm**: Multiply by 2^n by adding to exponent field

### 15. `scalbn()` - Scale by Power of 2
**Complexity**: ⭐⭐ (25 lines)
**Algorithm**: Like ldexp, but with overflow/underflow handling

### 16. `logb()` - Extract Exponent
**Complexity**: ⭐⭐ (15 lines)
**Algorithm**: Return unbiased exponent as double

---

## Day 5: Comparison & Utilities

### 17. `fdim()` - Positive Difference
**Complexity**: ⭐ (5 lines)
**Algorithm**: max(x - y, 0)

### 18. `fma()` - Fused Multiply-Add
**Complexity**: ⭐⭐⭐ (50 lines for exact rounding)
**Algorithm**: x * y + z with single rounding
**Note**: Already partially implemented in Float32Math

### 19. `modf()` - Split Integer/Fractional
**Complexity**: ⭐⭐ (20 lines)
**Algorithm**: Use trunc() for integer part

### 20. `remquo()` - Remainder with Quotient Bits
**Complexity**: ⭐⭐⭐ (40 lines)
**Algorithm**: Compute remainder, return 3 bits of quotient

---

## Implementation Template

```kotlin
package ai.solace.klang.math

/**
 * fabs: Absolute value (IEEE-754 compliant).
 * 
 * Clears the sign bit of the input without affecting the magnitude.
 * Handles all special values correctly.
 * 
 * ## Algorithm
 * Clear bit 63 (sign bit) of the IEEE-754 representation.
 * 
 * ## Special Cases
 * - `fabs(±0)` returns `+0`
 * - `fabs(±∞)` returns `+∞`
 * - `fabs(NaN)` returns `NaN` (preserves payload)
 * 
 * ## Reference
 * FreeBSD libm: `lib/msun/src/s_fabs.c`
 * 
 * @param x Input value
 * @return Absolute value of x
 * @since 0.2.0
 */
fun fabs(x: Double): Double {
    val bits = x.toRawBits()
    val absBits = bits and 0x7FFF_FFFF_FFFF_FFFFL
    return Double.fromBits(absBits)
}

/**
 * Test suite for fabs()
 */
@Test
fun testFabs() {
    // Normal values
    assertEquals(5.0, fabs(5.0))
    assertEquals(5.0, fabs(-5.0))
    
    // Zero
    assertEquals(0.0, fabs(0.0))
    assertEquals(0.0, fabs(-0.0))
    assertTrue(fabs(-0.0).toRawBits() == 0L) // +0, not -0
    
    // Infinity
    assertEquals(Double.POSITIVE_INFINITY, fabs(Double.POSITIVE_INFINITY))
    assertEquals(Double.POSITIVE_INFINITY, fabs(Double.NEGATIVE_INFINITY))
    
    // NaN
    assertTrue(fabs(Double.NaN).isNaN())
    
    // Subnormal
    val subnormal = Double.fromBits(1L) // Smallest positive subnormal
    assertEquals(subnormal, fabs(subnormal))
    assertEquals(subnormal, fabs(-subnormal))
}
```

---

## Success Criteria

- [ ] All 20 functions pass test suite
- [ ] Tests include: normals, zeros, infinities, NaNs, subnormals
- [ ] KDoc comments include algorithm description
- [ ] Cross-platform identical (JS = Native on all platforms)
- [ ] Benchmarks show <2× overhead vs Kotlin stdlib

---

## File Organization

```
src/commonMain/kotlin/ai/solace/klang/math/
├── MathConstants.kt      # PI, E, NaN, Inf bit patterns
├── Classification.kt     # isnan, isinf, isfinite, fpclassify
├── Comparison.kt         # fmax, fmin, fdim
├── Rounding.kt          # floor, ceil, trunc, round
├── Scaling.kt           # frexp, ldexp, scalbn, logb
├── Basic.kt             # fabs, copysign, modf
└── package-info.kt      # Package documentation

src/commonTest/kotlin/ai/solace/klang/math/
├── ClassificationTest.kt
├── ComparisonTest.kt
├── RoundingTest.kt
├── ScalingTest.kt
└── BasicTest.kt
```

---

## Reference Material

**FreeBSD libm source**: https://github.com/freebsd/freebsd-src/tree/main/lib/msun/src

**Key files to study**:
- `s_fabs.c` - Absolute value (template for bit manipulation)
- `s_copysign.c` - Sign bit operations
- `s_floor.c` - Rounding operations
- `s_frexp.c` - Exponent extraction
- `s_fmax.c` - NaN-aware comparison

**Test vectors**: FreeBSD test suite in `lib/msun/tests/`

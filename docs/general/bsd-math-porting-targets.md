# BSD Math Library Porting Targets

**Goal**: Port BSD-licensed C math functions to KLang for cross-platform deterministic arithmetic

## Selection Criteria

1. **BSD/ISC/MIT License** - Clean licensing for inclusion
2. **Self-contained** - Minimal dependencies
3. **Well-tested** - Production-quality implementations
4. **Educational value** - Good examples for C-to-Kotlin porting
5. **Practical utility** - Functions users actually need
6. **Platform compatibility** - Pure Kotlin (JS + Native, no JVM dependency)

---

## Priority 1: Core Math Functions (libm basics)

### Source: FreeBSD/OpenBSD/NetBSD libm
**License**: BSD 2-Clause/3-Clause
**Repository**: https://github.com/freebsd/freebsd-src/tree/main/lib/msun/src

### Easy Wins (10-50 lines each)

#### Basic Operations
- [ ] **`fabs()`** - Absolute value (bit manipulation only)
- [ ] **`copysign()`** - Copy sign bit (trivial)
- [ ] **`fmax()`** / **`fmin()`** - Min/max with NaN handling
- [ ] **`fdim()`** - Positive difference
- [ ] **`trunc()`** - Round toward zero
- [ ] **`round()`** - Round to nearest integer
- [ ] **`floor()`** - Round down
- [ ] **`ceil()`** - Round up
- [ ] **`rint()`** - Round to nearest (respecting rounding mode)
- [ ] **`nearbyint()`** - Like rint but no FP exceptions

**Why these**: Pure bit manipulation, no Taylor series, great for learning KLang patterns

#### Classification & Comparison
- [ ] **`isnan()`** - Check for NaN
- [ ] **`isinf()`** - Check for infinity
- [ ] **`isfinite()`** - Check for finite value
- [ ] **`signbit()`** - Extract sign bit
- [ ] **`fpclassify()`** - Classify floating point value

**Why these**: Already partially implemented in Float64Math, formalize them

#### Scaling & Manipulation
- [ ] **`frexp()`** - Extract mantissa and exponent
- [ ] **`ldexp()`** - Multiply by power of 2
- [ ] **`scalbn()`** - Scale by 2^n
- [ ] **`logb()`** - Extract exponent
- [ ] **`ilogb()`** - Integer exponent
- [ ] **`modf()`** - Split into integer and fractional parts

**Why these**: Direct mapping to IEEE-754 fields, exercises heap-backed returns

---

## Priority 2: Elementary Functions (Medium Difficulty)

### Power & Root Functions
- [ ] **`sqrt()`** - Square root (Newton-Raphson, well understood)
- [ ] **`cbrt()`** - Cube root
- [ ] **`hypot()`** - Hypotenuse (sqrt(x²+y²) without overflow)
- [ ] **`pow()`** - Power function (x^y)

**Estimated effort**: 50-150 lines each, good test cases available

### Exponential & Logarithm
- [ ] **`exp()`** - e^x (minimax polynomial)
- [ ] **`exp2()`** - 2^x
- [ ] **`expm1()`** - e^x - 1 (accurate for small x)
- [ ] **`log()`** - Natural logarithm
- [ ] **`log2()`** - Base-2 logarithm
- [ ] **`log10()`** - Base-10 logarithm
- [ ] **`log1p()`** - log(1+x) (accurate for small x)

**Note**: FreeBSD uses range reduction + polynomial approximation
**Why these**: Core scientific computing, demonstrates KLang's precision capabilities

---

## Priority 3: Trigonometric Functions

### Basic Trig
- [ ] **`sin()`** - Sine (range reduction + polynomial)
- [ ] **`cos()`** - Cosine
- [ ] **`tan()`** - Tangent
- [ ] **`asin()`** - Arcsine
- [ ] **`acos()`** - Arccosine
- [ ] **`atan()`** - Arctangent
- [ ] **`atan2()`** - Two-argument arctangent

### Hyperbolic Functions
- [ ] **`sinh()`** - Hyperbolic sine
- [ ] **`cosh()`** - Hyperbolic cosine
- [ ] **`tanh()`** - Hyperbolic tangent
- [ ] **`asinh()`** - Inverse hyperbolic sine
- [ ] **`acosh()`** - Inverse hyperbolic cosine
- [ ] **`atanh()`** - Inverse hyperbolic tangent

**Estimated effort**: 100-300 lines each
**Why these**: Complete the math library, needed for scientific computing

---

## Priority 4: Special Functions (Advanced)

### Source: Cephes Math Library
**License**: BSD-style (public domain in some versions)
**Repository**: http://www.netlib.org/cephes/

- [ ] **`erf()`** - Error function
- [ ] **`erfc()`** - Complementary error function
- [ ] **`gamma()`** - Gamma function
- [ ] **`lgamma()`** - Log-gamma function
- [ ] **`j0()`, `j1()`, `jn()`** - Bessel functions (first kind)
- [ ] **`y0()`, `y1()`, `yn()`** - Bessel functions (second kind)

**Why these**: Advanced numerical computing, demonstrates KLang's high-precision capabilities

---

## Priority 5: Multi-Precision Arithmetic

### Source: OpenBSD's bn (bignum)
**License**: ISC/BSD
**Path**: `src/lib/libc/crypt/arc4random.c` uses ChaCha20

Alternative: **mini-gmp** subset from GMP
**License**: LGPL/GPL (need BSD alternative)

Better option: **libtommath**
**License**: Public Domain / Unlicense
**Repository**: https://github.com/libtom/libtommath

- [ ] **Basic arithmetic**: Add, subtract, multiply, divide for arbitrary precision
- [ ] **Modular arithmetic**: `mod()`, `powmod()`
- [ ] **GCD/LCM**: Greatest common divisor, least common multiple
- [ ] **Prime testing**: Miller-Rabin, trial division

**Why these**: Extends HeapUInt128 to arbitrary precision, enables cryptography

---

## Reference Implementations to Study

### 1. **FreeBSD libm** (Best starting point)
- **License**: BSD 2-Clause
- **URL**: https://github.com/freebsd/freebsd-src/tree/main/lib/msun
- **Quality**: Production-grade, well-commented
- **Coverage**: Complete C99/C11 math.h

### 2. **OpenBSD libm**
- **License**: ISC/BSD
- **URL**: https://github.com/openbsd/src/tree/master/lib/libm
- **Quality**: Security-focused, clean code
- **Notable**: Conservative, well-audited

### 3. **fdlibm** (Sun/Oracle)
- **License**: BSD-style
- **URL**: https://www.netlib.org/fdlibm/
- **Quality**: Reference implementation for Java's StrictMath
- **Coverage**: Core transcendental functions

### 4. **musl libc**
- **License**: MIT
- **URL**: https://git.musl-libc.org/cgit/musl/tree/src/math
- **Quality**: Minimal, efficient, well-tested
- **Coverage**: Full C99 math

### 5. **Cephes**
- **License**: BSD/Public Domain
- **URL**: http://www.netlib.org/cephes/
- **Quality**: High-precision special functions
- **Coverage**: Advanced numerical analysis

---

## Porting Strategy

### Phase 1: Infrastructure (Week 1)
1. Create `src/commonMain/kotlin/ai/solace/klang/math/` package
2. Port `fabs()`, `copysign()`, `fmax()` as examples
3. Write test harness comparing against Kotlin's stdlib
4. Document porting patterns in `docs/components/porting/`

### Phase 2: Core Functions (Week 2-3)
1. Port all Priority 1 functions (classification, rounding, scaling)
2. Add comprehensive tests with edge cases (±0, ±Inf, NaN, subnormals)
3. Benchmark against native Kotlin math

### Phase 3: Elementary Functions (Week 4-6)
1. Port `sqrt()`, `exp()`, `log()` using FreeBSD algorithms
2. Implement range reduction for trig functions
3. Add double-double versions for high precision

### Phase 4: Special Functions (Month 2-3)
1. Port Cephes special functions
2. Optimize with KLang-specific techniques (HeapUInt128, CFloat128)
3. Write academic-quality documentation

---

## Success Metrics

1. **Correctness**: Pass all test vectors from original C implementations
2. **Precision**: Match or exceed C implementation accuracy
3. **Cross-platform**: Identical results on JVM, JS, Native
4. **Performance**: Within 2-3× of native Kotlin stdlib
5. **Documentation**: Every function has KDoc with algorithm description

---

## First Implementation Target: `fabs()`

```c
// FreeBSD: lib/msun/src/s_fabs.c
double fabs(double x) {
    uint64_t ix;
    memcpy(&ix, &x, sizeof(ix));
    ix &= 0x7fffffffffffffffULL;  // Clear sign bit
    memcpy(&x, &ix, sizeof(ix));
    return x;
}
```

**Kotlin port** (using KLang):
```kotlin
/**
 * fabs: IEEE-754 absolute value via bit manipulation.
 * 
 * Algorithm: Clear the sign bit (bit 63) of the double's binary representation.
 * Handles all special values correctly:
 * - fabs(±0) = +0
 * - fabs(±Inf) = +Inf
 * - fabs(NaN) = NaN (preserves payload)
 * 
 * @param x Input value
 * @return Absolute value of x
 * @since 0.2.0
 */
fun fabs(x: Double): Double {
    val bits = x.toRawBits()
    val absBits = bits and 0x7FFF_FFFF_FFFF_FFFFL  // Clear sign bit
    return Double.fromBits(absBits)
}
```

**Why this is perfect**:
- 5 lines of code
- Pure bit manipulation (no arithmetic)
- Exercises KLang's bit operations
- Complete test coverage in 10 lines
- Foundation for more complex functions

---

## Next Steps

1. **Review this list** - Get feedback on priorities
2. **Create issue tracker** - GitHub issues for each function
3. **Write porting guide** - Document C → Kotlin patterns
4. **Start with fabs()** - First pull request
5. **Build momentum** - One function per day for easy wins

---

**License Note**: All ported code will maintain BSD compatibility and include:
- Original copyright notices
- Algorithm references
- Test vectors from original implementations
- Citations to papers/algorithms used

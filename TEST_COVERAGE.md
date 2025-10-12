# Test Coverage and Conventions

## Naming Conventions

### Test File Names
- **Pattern**: `{SourceFileName}Test.kt`
- **Example**: `CDouble.kt` → `CDoubleTest.kt`
- **Location**: Mirror source structure: `src/commonMain/kotlin/ai/solace/klang/fp/CDouble.kt` → `src/commonTest/kotlin/ai/solace/klang/fp/CDoubleTest.kt`

### Test Class Names
- **Pattern**: `{SourceClassName}Test`
- **Example**: `class CDouble` → `class CDoubleTest`
- **Multiple test classes per source**: Add descriptive suffix
  - `KMallocTest` (basic functionality)
  - `KMallocCoalesceTest` (specific feature)
  - `KMallocReuseTest` (specific feature)

### Test Method Names
- **Pattern**: Descriptive camelCase starting with lowercase
- **Good examples**:
  - `fun basicArithmetic()`
  - `fun additionWithCarry()`
  - `fun zeroCopyOperations()`
  - `fun comparisonOperators()`
- **Avoid**: `test1()`, `testAdd()` (too generic)

### Test Organization
- Group related tests in the same file
- Use `@Test` annotation for all test methods
- Initialize required resources (GlobalHeap, KMalloc) in each test
- Keep tests independent (no shared mutable state)

## Current Test Coverage

### ✅ Well-Tested (>80% coverage)

#### Memory Management (`mem/`)
- ✅ `KMallocTest.kt` - Basic allocation
- ✅ `KMallocCoalesceTest.kt` - Memory coalescing
- ✅ `KMallocReuseTest.kt` - Block reuse
- ✅ `KStackTest.kt` - Stack allocator
- ✅ `GlobalDataTest.kt` - Global/static storage
- ✅ `CScalarsTest.kt` - Scalar variables
- ✅ `KAlignedTest.kt` - Alignment tests
- ✅ `CLibEdgeCasesTest.kt` - CLib edge cases
- ✅ `CLibStrnTests.kt` - String function tests
- ✅ `FastMemStringTest.kt` - Fast memory operations

**Coverage**: 10/13 files (77%)

#### Integer Types (`int/hpc/`)
- ✅ `HeapUInt128Test.kt` - 128-bit integers

**Coverage**: 1/1 files (100%)

#### Bitwise Operations (`bitwise/`)
- ✅ `ArrayBitShiftsHeapTest.kt`
- ✅ `ArrayBitShiftsRightShiftHeapTest.kt`
- ✅ `ArrayBitShiftsWordShiftTest.kt`
- ✅ `BitShiftEngineParityTest.kt`

**Coverage**: 4/18 files (22%)

### ❌ Untested (0% coverage)

#### Floating Point (`fp/`) - PRIORITY
- ❌ `CDouble.kt` - **CRITICAL**: Core double type
- ❌ `CFloat128.kt` - **CRITICAL**: Extended precision
- ❌ `CLongDouble.kt` - Long double abstraction
- ❌ `CFloat16.kt` - Half precision
- ❌ `CBF16.kt` - BFloat16
- ❌ `VectorOps.kt` - Vector operations

**Coverage**: 0/6 files (0%)

#### Other
- ❌ `float128/Float128.kt`
- ❌ `stringshift/HexShift.kt`

## Test Priority Matrix

### Priority 1: CRITICAL (Implement First)
Must have comprehensive tests for production readiness:

1. **`CDoubleTest.kt`** - Core floating point operations
   - Basic arithmetic (+, -, *, /)
   - Comparisons
   - Special values (NaN, Infinity)
   - Bit representation
   - Conversions (from/to Float, Int, Long)

2. **`CFloat128Test.kt`** - Extended precision
   - Double-double arithmetic
   - Error-free transformations
   - High-precision accumulation
   - Comparison with double precision

3. **`VectorOpsTest.kt`** - Vector operations
   - Dot product accuracy
   - AXPY operations
   - Deterministic results across platforms

### Priority 2: HIGH (Implement Soon)
Important for mathematical operations:

4. **`CLongDoubleTest.kt`** - Long double abstraction
   - Flavor selection (DOUBLE64, EXTENDED80, IEEE128)
   - Cross-platform consistency

5. **`CFloat16Test.kt`** - Half precision
   - Conversions to/from other formats
   - Range and precision limits

6. **`SwAR128Test.kt`** - Multi-limb arithmetic
   - Heap-native operations
   - Add/sub/shift correctness
   - Comparison operations

### Priority 3: MEDIUM (Fill Gaps)
Complete coverage of existing functionality:

7. **`Float32MathTest.kt`** - Soft-float operations
8. **`Float64MathTest.kt`** - Double math operations
9. **`BitwiseOpsTest.kt`** - Bitwise primitives
10. **`BitShiftEngineTest.kt`** - Shift engine modes

### Priority 4: LOW (Nice to Have)
Edge cases and specialized features:

11. **`CBF16Test.kt`** - BFloat16 format
12. **`HexShiftTest.kt`** - String utilities
13. **Comprehensive edge case tests** for all modules

## Test Implementation Guidelines

### Minimal Test Template

```kotlin
package ai.solace.klang.fp

import ai.solace.klang.mem.GlobalHeap
import ai.solace.klang.mem.KMalloc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CDoubleTest {
    private fun setup() {
        GlobalHeap.init(1 shl 20)  // 1MB
        KMalloc.init(1 shl 18)      // 256KB
    }
    
    @Test
    fun basicArithmetic() {
        setup()
        val a = CDouble.fromDouble(10.0)
        val b = CDouble.fromDouble(20.0)
        
        val sum = a + b
        assertEquals(30.0, sum.toDouble(), 1e-10)
        
        val diff = b - a
        assertEquals(10.0, diff.toDouble(), 1e-10)
        
        val prod = a * b
        assertEquals(200.0, prod.toDouble(), 1e-10)
        
        val quot = b / a
        assertEquals(2.0, quot.toDouble(), 1e-10)
    }
    
    @Test
    fun specialValues() {
        setup()
        
        val nan = CDouble.NaN
        assertTrue(nan.toDouble().isNaN())
        
        val inf = CDouble.POSITIVE_INFINITY
        assertTrue(inf.toDouble().isInfinite())
        
        val zero = CDouble.ZERO
        assertEquals(0.0, zero.toDouble())
    }
}
```

### Test Best Practices

1. **Independence**: Each test should initialize its own resources
2. **Clarity**: Use descriptive test names that explain what's being tested
3. **Assertions**: Use specific assertions with helpful messages
4. **Edge Cases**: Test boundaries, special values, overflow, underflow
5. **Cross-Platform**: Ensure tests pass on all targets (JS, Native)
6. **Performance**: Keep tests fast (< 100ms each ideally)

## Coverage Metrics (Target)

- **Overall Target**: 80%+ line coverage
- **Critical Paths**: 100% coverage (arithmetic, memory allocation)
- **Edge Cases**: Comprehensive coverage of error conditions
- **Integration**: Test interactions between modules

## Next Steps

1. ✅ Create `CDoubleTest.kt` (Priority 1)
2. ✅ Create `CFloat128Test.kt` (Priority 1)
3. ✅ Create `VectorOpsTest.kt` (Priority 1)
4. 📋 Create `CLongDoubleTest.kt` (Priority 2)
5. 📋 Create `SwAR128Test.kt` (Priority 2)
6. 📋 Run coverage analysis tools
7. 📋 Document untested edge cases
8. 📋 Add integration tests

## Running Tests

```bash
# All tests
./gradlew test

# Specific target
./gradlew jsTest
./gradlew macosArm64Test

# With verbose output
./gradlew test --info

# Specific test class (not supported by Kotlin MPP directly)
# Use test filtering in Gradle if needed
```

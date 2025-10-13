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

#### Floating Point (`fp/`)
- ✅ `CDoubleTest.kt` - Core double operations (18 tests)
- ✅ `CFloat128Test.kt` - Extended precision (19 tests)
- ✅ `VectorOpsTest.kt` - Vector operations (14 tests)
- ✅ `CLongDoubleTest.kt` - Long double abstraction (13 tests, 9 TODO)

**Coverage**: 4/6 files (67%)

#### Integer Types (`int/hpc/`)
- ✅ `HeapUInt128Test.kt` - 128-bit integers (4 tests)

**Coverage**: 1/1 files (100%)

#### Bitwise Operations (`bitwise/`)
- ✅ `ArrayBitShiftsHeapTest.kt` - Heap-based shifts
- ✅ `ArrayBitShiftsRightShiftHeapTest.kt` - Right shift heap
- ✅ `ArrayBitShiftsWordShiftTest.kt` - Word shifts
- ✅ `BitShiftEngineParityTest.kt` - Engine parity
- ✅ `SwAR128Test.kt` - Multi-limb arithmetic (33 tests)
- ✅ `Float32MathTest.kt` - 32-bit soft-float (31 tests)
- ✅ `Float64MathTest.kt` - 64-bit operations (36 tests)
- ✅ `BitwiseOpsTest.kt` - Bitwise utilities (34 tests)

**Coverage**: 8/18 files (44%)

### ⚠️ Partially Tested (40-79% coverage)

Currently none - all tested modules have good coverage!

### ❌ Untested (0% coverage)

#### Floating Point (`fp/`) - Lower Priority
- ❌ `CFloat16.kt` - Half precision (basic tests exist in other areas)
- ❌ `CBF16.kt` - BFloat16

#### Other
- ❌ `float128/Float128.kt` - Legacy float128 module
- ❌ `stringshift/HexShift.kt` - String utilities

**Remaining Untested**: 4 files

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

### ✅ Priority 1: CRITICAL (COMPLETE)
Must have comprehensive tests for production readiness:

1. ✅ **`CDoubleTest.kt`** - Core floating point operations (18 tests)
2. ✅ **`CFloat128Test.kt`** - Extended precision (19 tests)
3. ✅ **`VectorOpsTest.kt`** - Vector operations (14 tests)

### ✅ Priority 2: HIGH (COMPLETE)
Important for mathematical operations:

4. ✅ **`CLongDoubleTest.kt`** - Long double abstraction (13 tests)
5. ✅ **`SwAR128Test.kt`** - Multi-limb arithmetic (33 tests)

### ✅ Priority 3: MEDIUM (COMPLETE)
Complete coverage of existing functionality:

6. ✅ **`Float32MathTest.kt`** - Soft-float operations (31 tests)
7. ✅ **`Float64MathTest.kt`** - Double math operations (36 tests)
8. ✅ **`BitwiseOpsTest.kt`** - Bitwise primitives (34 tests)

### 📋 Priority 4: LOW (Optional)
Edge cases and specialized features:

9. 📋 **`CFloat16Test.kt`** - Half precision enhancements
10. 📋 **`CBF16Test.kt`** - BFloat16 format
11. 📋 **`HexShiftTest.kt`** - String utilities
12. 📋 **Comprehensive edge case tests** for all modules

## Test Statistics

**Total Tests**: 198
- Memory Management: 26 tests
- Floating Point: 64 tests
- Bitwise Operations: 104 tests
- Integer Types: 4 tests

**Pass Rate**: 100% on macOS ARM64

**Lines of Test Code**: ~6,500 lines

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

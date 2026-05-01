package ai.solace.klang.mem

import ai.solace.klang.fp.CBF16
import ai.solace.klang.fp.CFloat128
import ai.solace.klang.fp.CFloat16
import ai.solace.klang.fp.CFloat32
import ai.solace.klang.fp.CFloat64
import ai.solace.klang.fp.CLongDouble
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the heap-backed FP arithmetic surface added in `CFloatVarOps.kt`.
 *
 * Each test runs inside a `KStack.withFrame { … }` so the binary operators
 * (`+`, `-`, `*`, `/`) — which allocate a fresh heap cell on every call —
 * are reclaimed when the frame pops.
 */
class CFloatVarOpsTest {

    @BeforeTest
    fun setup() {
        // Idempotent stack/heap init so each test runs in a known state.
        KMalloc.init(64 * 1024)
        KStack.init(8 * 1024)
    }

    // ------------------------------------------------------------------
    // CFloat64Var
    // ------------------------------------------------------------------

    @Test
    fun cfloat64Var_compoundAssign() {
        KStack.withFrame {
            val a = CAutos.double(10.0)
            val b = CAutos.double(3.0)
            a += b; assertEquals(13.0, a.value, 1e-12, "+= failed")
            a -= b; assertEquals(10.0, a.value, 1e-12, "-= failed")
            a *= b; assertEquals(30.0, a.value, 1e-12, "*= failed")
            a /= b; assertEquals(10.0, a.value, 1e-12, "/= failed")
        }
    }

    @Test
    fun cfloat64Var_binaryOps_returnFreshHeapCells() {
        KStack.withFrame {
            val a = CAutos.double(7.0)
            val b = CAutos.double(2.0)

            val sum = a + b
            val diff = a - b
            val prod = a * b
            val quot = a / b

            // Original operands untouched.
            assertEquals(7.0, a.value, 1e-12)
            assertEquals(2.0, b.value, 1e-12)

            // Results live in distinct heap cells.
            assertEquals(9.0, sum.value, 1e-12)
            assertEquals(5.0, diff.value, 1e-12)
            assertEquals(14.0, prod.value, 1e-12)
            assertEquals(3.5, quot.value, 1e-12)

            assertTrue(sum.addr != a.addr && sum.addr != b.addr, "sum must allocate fresh cell")
            assertTrue(diff.addr != sum.addr, "diff must allocate fresh cell")
        }
    }

    @Test
    fun cfloat64Var_unaryMinus() {
        KStack.withFrame {
            val a = CAutos.double(3.5)
            val n = -a
            assertEquals(3.5, a.value, 1e-12, "operand untouched")
            assertEquals(-3.5, n.value, 1e-12)
        }
    }

    // ------------------------------------------------------------------
    // CFloat128Var
    // ------------------------------------------------------------------

    @Test
    fun cfloat128Var_compoundAssign() {
        KStack.withFrame {
            val a = CAutos.float128(CFloat128.fromDouble(100.0))
            val b = CAutos.float128(CFloat128.fromDouble(4.0))

            a += b; assertEquals(104.0, a.value.toDouble(), 1e-10)
            a -= b; assertEquals(100.0, a.value.toDouble(), 1e-10)
            a *= b; assertEquals(400.0, a.value.toDouble(), 1e-10)
            a /= b; assertEquals(100.0, a.value.toDouble(), 1e-10)
        }
    }

    @Test
    fun cfloat128Var_doubleDoublePrecisionPreserved() {
        // (1/3) * 3 should round-trip to within ~1e-28 in dd precision,
        // which means the binary-op path is genuinely going through the
        // CFloat128 algorithm (not a Double shortcut).
        KStack.withFrame {
            val three = CAutos.float128(CFloat128.fromDouble(3.0))
            val one = CAutos.float128(CFloat128.fromDouble(1.0))
            val third = one / three
            val tripled = third * three
            val errAbs = abs(tripled.value.toDouble() - 1.0)
            assertTrue(errAbs < 1e-28, "dd precision lost: |error|=$errAbs")
        }
    }

    @Test
    fun cfloat128Var_unaryMinus() {
        KStack.withFrame {
            val a = CAutos.float128(CFloat128.fromDouble(2.5))
            val n = -a
            assertEquals(2.5, a.value.toDouble(), 1e-12)
            assertEquals(-2.5, n.value.toDouble(), 1e-12)
        }
    }

    // ------------------------------------------------------------------
    // CFloat32Var
    // ------------------------------------------------------------------

    @Test
    fun cfloat32Var_compoundAndBinary() {
        KStack.withFrame {
            val a = CAutos.float32(CFloat32.fromFloat(8.0f))
            val b = CAutos.float32(CFloat32.fromFloat(2.0f))

            val sum = a + b
            assertEquals(10.0f, sum.value.toFloat(), "+ failed")

            a += b; assertEquals(10.0f, a.value.toFloat(), "+= failed")
            a /= b; assertEquals(5.0f, a.value.toFloat(), "/= failed")
        }
    }

    // ------------------------------------------------------------------
    // CFloat16Var
    // ------------------------------------------------------------------

    @Test
    fun cfloat16Var_compoundAndBinary() {
        KStack.withFrame {
            val a = CAutos.float16(CFloat16.fromFloat(4.0f))
            val b = CAutos.float16(CFloat16.fromFloat(2.0f))

            val sum = a + b
            assertEquals(6.0f, sum.value.toFloat(), "+ failed")

            a *= b; assertEquals(8.0f, a.value.toFloat(), "*= failed")
            a -= b; assertEquals(6.0f, a.value.toFloat(), "-= failed")
        }
    }

    // ------------------------------------------------------------------
    // CBF16Var
    // ------------------------------------------------------------------

    @Test
    fun cbf16Var_compoundAndBinary() {
        KStack.withFrame {
            val a = CAutos.bfloat16(CBF16.fromFloat(2.0f))
            val b = CAutos.bfloat16(CBF16.fromFloat(3.0f))

            val prod = a * b
            // bf16 has only ~2 decimal digits of precision; 6.0 representable exactly.
            assertEquals(6.0f, prod.value.toFloat(), 1e-2f, "* failed")

            a += b; assertEquals(5.0f, a.value.toFloat(), 1e-2f, "+= failed")
        }
    }

    // ------------------------------------------------------------------
    // CLongDoubleVar
    // ------------------------------------------------------------------

    @Test
    fun clongDoubleVar_compoundAndBinary() {
        KStack.withFrame {
            val a = CAutos.longdouble(CLongDouble.ofDouble(10.0))
            val b = CAutos.longdouble(CLongDouble.ofDouble(2.5))

            val sum = a + b
            val diff = a - b
            assertEquals(12.5, sum.value.toCFloat128().toDouble(), 1e-12)
            assertEquals(7.5, diff.value.toCFloat128().toDouble(), 1e-12)

            a /= b; assertEquals(4.0, a.value.toCFloat128().toDouble(), 1e-12)
        }
    }

    // ------------------------------------------------------------------
    // CFloatVar (legacy native-Float storage)
    // ------------------------------------------------------------------

    @Test
    fun cfloatVar_compoundAndBinary() {
        KStack.withFrame {
            val a = CAutos.float(6.0f)
            val b = CAutos.float(2.0f)

            val sum = a + b
            assertEquals(8.0f, sum.value, "+ failed")

            a *= b; assertEquals(12.0f, a.value, "*= failed")
            a /= b; assertEquals(6.0f, a.value, "/= failed")
        }
    }
}

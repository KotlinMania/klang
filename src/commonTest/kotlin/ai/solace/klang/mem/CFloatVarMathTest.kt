package ai.solace.klang.mem

import ai.solace.klang.fp.CBF16
import ai.solace.klang.fp.CFloat128
import ai.solace.klang.fp.CFloat16
import ai.solace.klang.fp.CFloat32
import ai.solace.klang.fp.CLongDouble
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the heap-backed FP math surface added in `CFloatVarMath.kt`
 * (Slice 3) plus the free-function layer in `math/HeapFp.kt`.
 *
 * Each test runs inside a `KStack.withFrame { … }` because the
 * allocating forms claim a fresh heap cell per call.
 */
class CFloatVarMathTest {

    @BeforeTest
    fun setup() {
        KMalloc.init(64 * 1024)
        KStack.init(8 * 1024)
    }

    // ===================================================================
    // CFloat64Var — primary type, exercises every op.
    // ===================================================================

    @Test
    fun cfloat64_sqrt_inPlaceAndAllocating() {
        KStack.withFrame {
            val a = CAutos.double(2.0)
            val r = a.sqrt()
            assertEquals(2.0, a.value, 0.0, "operand untouched")
            assertEquals(kotlin.math.sqrt(2.0), r.value, 0.0)
            assertTrue(r.addr != a.addr, "must allocate fresh cell")

            a.sqrtAssign()
            assertEquals(kotlin.math.sqrt(2.0), a.value, 0.0)
        }
    }

    @Test
    fun cfloat64_floorCeilTruncRound_negativeHalf() {
        KStack.withFrame {
            val a = CAutos.double(-1.5)
            assertEquals(-2.0, a.floor().value, 0.0)
            assertEquals(-1.0, a.ceil().value, 0.0)
            assertEquals(-1.0, a.trunc().value, 0.0)
            // C99 round: half-away-from-zero ⇒ -2.0
            assertEquals(-2.0, a.round().value, 0.0)
        }
    }

    @Test
    fun cfloat64_absAndCopysign() {
        KStack.withFrame {
            val a = CAutos.double(-3.5)
            assertEquals(3.5, a.abs().value, 0.0)

            val mag = CAutos.double(3.0)
            val neg = CAutos.double(-1.0)
            val r = mag.copysign(neg)
            assertEquals(-3.0, r.value, 0.0)

            mag.copysignAssign(neg)
            assertEquals(-3.0, mag.value, 0.0)
        }
    }

    @Test
    fun cfloat64_frexpAndLdexp() {
        KStack.withFrame {
            val a = CAutos.double(12.0)
            val (m, e) = a.frexp()
            assertEquals(0.75, m.value, 0.0)
            assertEquals(4, e)

            val a2 = CAutos.double(1.5)
            assertEquals(12.0, a2.ldexp(3).value, 0.0)
            a2.ldexpAssign(3)
            assertEquals(12.0, a2.value, 0.0)
        }
    }

    @Test
    fun cfloat64_modf() {
        KStack.withFrame {
            val a = CAutos.double(3.75)
            val (i, f) = a.modf()
            assertEquals(3.0, i.value, 0.0)
            assertEquals(0.75, f.value, 0.0)
        }
    }

    @Test
    fun cfloat64_sqrtEdgeCases() {
        KStack.withFrame {
            val nan = CAutos.double(Double.NaN)
            assertTrue(nan.sqrt().value.isNaN())

            val negZero = CAutos.double(-0.0)
            val r = negZero.sqrt()
            assertEquals(-0.0, r.value, 0.0)
            assertTrue(r.value.toRawBits() == (-0.0).toRawBits(), "preserves -0.0 bit-pattern")

            val posInf = CAutos.double(Double.POSITIVE_INFINITY)
            assertEquals(Double.POSITIVE_INFINITY, posInf.sqrt().value, 0.0)
        }
    }

    // ===================================================================
    // CFloat128Var — precision-sensitive (double-double).
    // ===================================================================

    @Test
    fun cfloat128_sqrtPreservesDoubleDoublePrecision() {
        KStack.withFrame {
            val two = CAutos.float128(CFloat128.fromDouble(2.0))
            val r = two.sqrt()
            // (sqrt(2))² should round-trip to within ~1e-28 in dd precision.
            val sq = r * r
            val errAbs = abs(sq.value.toDouble() - 2.0)
            assertTrue(errAbs < 1e-28, "dd precision lost on sqrt: |error|=$errAbs")
        }
    }

    @Test
    fun cfloat128_floorAndFrexpAndModf() {
        KStack.withFrame {
            val a = CAutos.float128(CFloat128.fromDouble(-1.5))
            assertEquals(-2.0, a.floor().value.toDouble(), 1e-15)

            val twelve = CAutos.float128(CFloat128.fromDouble(12.0))
            val (_, e) = twelve.frexp()
            assertEquals(4, e)

            val three75 = CAutos.float128(CFloat128.fromDouble(3.75))
            val (i, f) = three75.modf()
            assertEquals(3.0, i.value.toDouble(), 1e-15)
            assertEquals(0.75, f.value.toDouble(), 1e-15)
        }
    }

    @Test
    fun cfloat128_absAndCopysign() {
        KStack.withFrame {
            val a = CAutos.float128(CFloat128.fromDouble(-7.25))
            assertEquals(7.25, a.abs().value.toDouble(), 1e-15)

            val pos = CAutos.float128(CFloat128.fromDouble(2.0))
            val negSign = CAutos.float128(CFloat128.fromDouble(-1.0))
            assertEquals(-2.0, pos.copysign(negSign).value.toDouble(), 1e-15)
        }
    }

    // ===================================================================
    // CFloat32Var — heap-native sqrt + wrapper-routed frexp.
    // ===================================================================

    @Test
    fun cfloat32_sqrtAndFloorAndModf() {
        KStack.withFrame {
            val a = CAutos.float32(CFloat32.fromFloat(2.0f))
            assertEquals(kotlin.math.sqrt(2.0f), a.sqrt().value.toFloat(), 0.0f)

            val b = CAutos.float32(CFloat32.fromFloat(-1.5f))
            assertEquals(-2.0f, b.floor().value.toFloat(), 0.0f)

            val c = CAutos.float32(CFloat32.fromFloat(3.75f))
            val (i, f) = c.modf()
            assertEquals(3.0f, i.value.toFloat(), 0.0f)
            assertEquals(0.75f, f.value.toFloat(), 0.0f)
        }
    }

    // ===================================================================
    // CFloat16Var — F16Math backend.
    // ===================================================================

    @Test
    fun cfloat16_sqrtAndFloor() {
        KStack.withFrame {
            val a = CAutos.float16(CFloat16.fromFloat(4.0f))
            assertEquals(2.0f, a.sqrt().value.toFloat(), 1e-3f)

            val b = CAutos.float16(CFloat16.fromFloat(-1.5f))
            assertEquals(-2.0f, b.floor().value.toFloat(), 1e-3f)
        }
    }

    @Test
    fun cfloat16_frexpAndModf() {
        KStack.withFrame {
            val a = CAutos.float16(CFloat16.fromFloat(12.0f))
            val (_, e) = a.frexp()
            assertEquals(4, e)

            val b = CAutos.float16(CFloat16.fromFloat(3.5f))
            val (i, f) = b.modf()
            assertEquals(3.0f, i.value.toFloat(), 1e-3f)
            assertEquals(0.5f, f.value.toFloat(), 1e-3f)
        }
    }

    // ===================================================================
    // CBF16Var — wrapper-routed.
    // ===================================================================

    @Test
    fun cbf16_smokeTest() {
        KStack.withFrame {
            val a = CAutos.bfloat16(CBF16.fromFloat(4.0f))
            assertEquals(2.0f, a.sqrt().value.toFloat(), 1e-2f)

            val b = CAutos.bfloat16(CBF16.fromFloat(-1.5f))
            assertEquals(-2.0f, b.floor().value.toFloat(), 1e-2f)

            val (m, e) = CAutos.bfloat16(CBF16.fromFloat(12.0f)).frexp()
            assertEquals(4, e)
            assertEquals(0.75f, m.value.toFloat(), 1e-2f)

            val (i, f) = CAutos.bfloat16(CBF16.fromFloat(3.5f)).modf()
            assertEquals(3.0f, i.value.toFloat(), 1e-2f)
            assertEquals(0.5f, f.value.toFloat(), 1e-2f)
        }
    }

    // ===================================================================
    // CLongDoubleVar — regression for Slice-2 NPE; flavor-aware path.
    // ===================================================================

    @Test
    fun clongDouble_sqrtNoNpe() {
        KStack.withFrame {
            // This line would have NPE'd before Slice 2's flavor fix.
            val a = CAutos.longdouble(CLongDouble.ofDouble(2.0))
            val r = a.sqrt()
            assertEquals(kotlin.math.sqrt(2.0), r.value.toDouble(), 1e-15)
        }
    }

    @Test
    fun clongDouble_floorAndFrexpAndModf() {
        KStack.withFrame {
            val a = CAutos.longdouble(CLongDouble.ofDouble(-1.5))
            assertEquals(-2.0, a.floor().value.toDouble(), 1e-15)

            val (_, e) = CAutos.longdouble(CLongDouble.ofDouble(12.0)).frexp()
            assertEquals(4, e)

            val (i, f) = CAutos.longdouble(CLongDouble.ofDouble(3.75)).modf()
            assertEquals(3.0, i.value.toDouble(), 1e-15)
            assertEquals(0.75, f.value.toDouble(), 1e-15)
        }
    }

    // ===================================================================
    // CFloatVar — native Float storage.
    // ===================================================================

    @Test
    fun cfloatVar_smokeTest() {
        KStack.withFrame {
            val a = CAutos.float(2.0f)
            assertEquals(kotlin.math.sqrt(2.0f), a.sqrt().value, 0.0f)

            val b = CAutos.float(-1.5f)
            assertEquals(-2.0f, b.floor().value, 0.0f)
            assertEquals(-1.0f, b.ceil().value, 0.0f)
            assertEquals(-2.0f, b.round().value, 0.0f)
            assertEquals(1.5f, b.abs().value, 0.0f)

            val (m, e) = CAutos.float(12.0f).frexp()
            assertEquals(4, e)
            assertEquals(0.75f, m.value, 0.0f)
        }
    }

    // ===================================================================
    // Free-function form (HeapFp.kt) parity with extension form.
    // ===================================================================

    @Test
    fun freeFunctionParity() {
        KStack.withFrame {
            val a = CAutos.double(2.0)
            val viaExt = a.sqrt().value
            val viaFn = ai.solace.klang.math.sqrt(a).value
            assertEquals(viaExt, viaFn, 0.0)

            val b = CAutos.double(-1.5)
            assertEquals(b.floor().value, ai.solace.klang.math.floor(b).value, 0.0)
            assertEquals(b.ceil().value, ai.solace.klang.math.ceil(b).value, 0.0)
            assertEquals(b.abs().value, ai.solace.klang.math.abs(b).value, 0.0)

            val twelve = CAutos.double(12.0)
            val (_, e) = ai.solace.klang.math.frexp(twelve)
            assertEquals(4, e)

            val onePointFive = CAutos.double(1.5)
            assertEquals(12.0, ai.solace.klang.math.ldexp(onePointFive, 3).value, 0.0)
        }
    }
}

package ai.solace.klang.fp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-FP-type tests for the "basic math" suite:
 *   sqrt, floor, ceil, trunc, round, frexp, ldexp, modf
 * across CFloat64, CFloat128, CFloat32, CFloat16, CBF16, CLongDouble.
 */
class BasicMathFpTest {

    // ---- CFloat64 ----

    @Test
    fun cDoubleSqrt() {
        assertEquals(2.0, CFloat64.fromDouble(4.0).sqrt().toDouble())
        assertEquals(3.0, CFloat64.fromDouble(9.0).sqrt().toDouble())
        assertEquals(0.0, CFloat64.fromDouble(0.0).sqrt().toDouble())
        assertTrue(CFloat64.fromDouble(-1.0).sqrt().toDouble().isNaN())
    }

    @Test
    fun cDoubleRounding() {
        assertEquals(2.0, CFloat64.fromDouble(2.7).floor().toDouble())
        assertEquals(3.0, CFloat64.fromDouble(2.3).ceil().toDouble())
        assertEquals(2.0, CFloat64.fromDouble(2.7).trunc().toDouble())
        assertEquals(-2.0, CFloat64.fromDouble(-2.7).trunc().toDouble())
        assertEquals(3.0, CFloat64.fromDouble(2.5).round().toDouble())
        assertEquals(-3.0, CFloat64.fromDouble(-2.5).round().toDouble())
    }

    @Test
    fun cDoubleFrexpLdexpModf() {
        val x = CFloat64.fromDouble(12.5)
        val (m, e) = x.frexp()
        assertEquals(x.toDouble(), m.ldexp(e).toDouble())

        val (i, f) = CFloat64.fromDouble(-3.75).modf()
        assertEquals(-3.0, i.toDouble())
        assertEquals(-0.75, f.toDouble())

        assertEquals(8.0, CFloat64.fromDouble(1.0).ldexp(3).toDouble())
    }

    // ---- CFloat128 ----

    @Test
    fun cFloat128Sqrt() {
        // sqrt(4) == 2 exactly
        val s4 = CFloat128.fromDouble(4.0).sqrt()
        assertEquals(2.0, s4.toDouble())

        // sqrt(2) — high precision check: sqrt(2)^2 must equal 2 to ~30 digits
        val s2 = CFloat128.fromDouble(2.0).sqrt()
        val s2sq = s2 * s2
        val err = (s2sq - CFloat128.fromDouble(2.0)).toDouble()
        assertTrue(kotlin.math.abs(err) < 1e-28,
            "sqrt(2)^2 - 2 too large: $err (expected < 1e-28)")

        // Negative → NaN
        assertTrue(CFloat128.fromDouble(-1.0).sqrt().hi.isNaN())

        // Zero
        assertEquals(0.0, CFloat128.ZERO.sqrt().toDouble())
    }

    @Test
    fun cFloat128Division() {
        // 1/3, then * 3 — should be ~1.0 with very small dd error
        val third = CFloat128.ONE / CFloat128.fromDouble(3.0)
        val reconstructed = third * CFloat128.fromDouble(3.0)
        val err = (reconstructed - CFloat128.ONE).toDouble()
        assertTrue(kotlin.math.abs(err) < 1e-28,
            "(1/3)*3 deviation too large: $err")

        // Division by zero
        val infResult = CFloat128.fromDouble(1.0) / CFloat128.ZERO
        assertEquals(Double.POSITIVE_INFINITY, infResult.hi)
    }

    @Test
    fun cFloat128Rounding() {
        assertEquals(2.0, CFloat128.fromDouble(2.7).floor().toDouble())
        assertEquals(-3.0, CFloat128.fromDouble(-2.3).floor().toDouble())
        assertEquals(3.0, CFloat128.fromDouble(2.3).ceil().toDouble())
        assertEquals(2.0, CFloat128.fromDouble(2.7).trunc().toDouble())
        assertEquals(-2.0, CFloat128.fromDouble(-2.7).trunc().toDouble())
        assertEquals(3.0, CFloat128.fromDouble(2.5).round().toDouble())
        assertEquals(-3.0, CFloat128.fromDouble(-2.5).round().toDouble())
    }

    @Test
    fun cFloat128FrexpLdexpModf() {
        val x = CFloat128.fromDouble(12.5)
        val (m, e) = x.frexp()
        assertTrue(kotlin.math.abs(m.toDouble()) >= 0.5 && kotlin.math.abs(m.toDouble()) < 1.0)
        val reconstructed = m.ldexp(e)
        assertEquals(x.toDouble(), reconstructed.toDouble())

        val (i, f) = CFloat128.fromDouble(-3.75).modf()
        assertEquals(-3.0, i.toDouble())
        assertEquals(-0.75, f.toDouble())
    }

    // ---- CFloat32 ----

    @Test
    fun cFloat32BasicMath() {
        val x = CFloat32.fromFloat(4.0f)
        assertEquals(2.0f, x.sqrt().toFloat())
        assertEquals(2.0f, CFloat32.fromFloat(2.7f).floor().toFloat())
        assertEquals(3.0f, CFloat32.fromFloat(2.3f).ceil().toFloat())
        assertEquals(2.0f, CFloat32.fromFloat(2.7f).trunc().toFloat())
        assertEquals(3.0f, CFloat32.fromFloat(2.5f).round().toFloat())
        val (m, e) = CFloat32.fromFloat(12.5f).frexp()
        assertEquals(12.5f, m.ldexp(e).toFloat())
        val (i, f) = CFloat32.fromFloat(-3.75f).modf()
        assertEquals(-3.0f, i.toFloat())
        assertEquals(-0.75f, f.toFloat())
    }

    // ---- CFloat16 ----

    @Test
    fun cFloat16BasicMath() {
        assertEquals(2.0f, CFloat16.fromFloat(4.0f).sqrt().toFloat())
        assertEquals(2.0f, CFloat16.fromFloat(2.7f).floor().toFloat())
        assertEquals(3.0f, CFloat16.fromFloat(2.3f).ceil().toFloat())
        assertEquals(2.0f, CFloat16.fromFloat(2.7f).trunc().toFloat())
        assertEquals(3.0f, CFloat16.fromFloat(2.5f).round().toFloat())
        val (m, e) = CFloat16.fromFloat(12.5f).frexp()
        // f16 is low-precision; just check reconstruction equals original f16-rounded value
        val original = CFloat16.fromFloat(12.5f).toFloat()
        assertEquals(original, m.ldexp(e).toFloat())
    }

    // ---- CBF16 ----

    @Test
    fun cbf16BasicMath() {
        assertEquals(2.0f, CBF16.fromFloat(4.0f).sqrt().toFloat())
        assertEquals(2.0f, CBF16.fromFloat(2.7f).floor().toFloat())
        assertEquals(3.0f, CBF16.fromFloat(2.3f).ceil().toFloat())
        assertEquals(2.0f, CBF16.fromFloat(2.7f).trunc().toFloat())
        assertEquals(3.0f, CBF16.fromFloat(2.5f).round().toFloat())
        val (m, e) = CBF16.fromFloat(12.5f).frexp()
        val original = CBF16.fromFloat(12.5f).toFloat()
        assertEquals(original, m.ldexp(e).toFloat())
    }

    // ---- CLongDouble ----

    @Test
    fun cLongDoubleBasicMath_Double64() {
        val flavor = CLongDouble.Flavor.DOUBLE64
        assertEquals(2.0, CLongDouble.ofDouble(4.0, flavor).sqrt().toDouble())
        assertEquals(2.0, CLongDouble.ofDouble(2.7, flavor).floor().toDouble())
        assertEquals(3.0, CLongDouble.ofDouble(2.3, flavor).ceil().toDouble())
        assertEquals(2.0, CLongDouble.ofDouble(2.7, flavor).trunc().toDouble())
        assertEquals(3.0, CLongDouble.ofDouble(2.5, flavor).round().toDouble())
        val (m, e) = CLongDouble.ofDouble(12.5, flavor).frexp()
        assertEquals(12.5, m.ldexp(e).toDouble())
        val (i, f) = CLongDouble.ofDouble(-3.75, flavor).modf()
        assertEquals(-3.0, i.toDouble())
        assertEquals(-0.75, f.toDouble())
    }

    @Test
    fun cLongDoubleBasicMath_Extended80() {
        val flavor = CLongDouble.Flavor.EXTENDED80
        assertEquals(2.0, CLongDouble.ofDouble(4.0, flavor).sqrt().toDouble())
        assertEquals(2.0, CLongDouble.ofDouble(2.7, flavor).floor().toDouble())
        assertEquals(3.0, CLongDouble.ofDouble(2.3, flavor).ceil().toDouble())
        assertEquals(3.0, CLongDouble.ofDouble(2.5, flavor).round().toDouble())
        val (m, e) = CLongDouble.ofDouble(12.5, flavor).frexp()
        assertEquals(12.5, m.ldexp(e).toDouble())
        val (i, f) = CLongDouble.ofDouble(-3.75, flavor).modf()
        assertEquals(-3.0, i.toDouble())
        assertEquals(-0.75, f.toDouble())
    }

    @Test
    fun cLongDoubleBasicMath_Ieee128() {
        val flavor = CLongDouble.Flavor.IEEE128
        assertEquals(2.0, CLongDouble.ofDouble(4.0, flavor).sqrt().toDouble())
        assertEquals(2.0, CLongDouble.ofDouble(2.7, flavor).trunc().toDouble())
        assertEquals(3.0, CLongDouble.ofDouble(2.5, flavor).round().toDouble())
    }
}

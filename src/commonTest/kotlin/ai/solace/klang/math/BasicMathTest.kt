package ai.solace.klang.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.pow

class BasicMathTest {

    @Test
    fun sqrtBasics() {
        assertEquals(0.0, BasicMath.sqrt(0.0))
        assertEquals(1.0, BasicMath.sqrt(1.0))
        assertEquals(2.0, BasicMath.sqrt(4.0))
        assertEquals(3.0, BasicMath.sqrt(9.0))
        assertTrue(BasicMath.sqrt(-1.0).isNaN())
        assertTrue(BasicMath.sqrt(Double.NaN).isNaN())
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.sqrt(Double.POSITIVE_INFINITY))
    }

    @Test
    fun floorCeilTrunc() {
        assertEquals(2.0, BasicMath.floor(2.7))
        assertEquals(-3.0, BasicMath.floor(-2.3))
        assertEquals(3.0, BasicMath.ceil(2.3))
        assertEquals(-2.0, BasicMath.ceil(-2.7))
        assertEquals(2.0, BasicMath.trunc(2.7))
        assertEquals(-2.0, BasicMath.trunc(-2.7))
        // trunc preserves sign of zero on the result, so compare magnitude only here
        assertEquals(0.0, kotlin.math.abs(BasicMath.trunc(0.5)))
        assertEquals(0.0, kotlin.math.abs(BasicMath.trunc(-0.5)))
    }

    @Test
    fun roundHalfAwayFromZero() {
        assertEquals(1.0, BasicMath.round(0.5))
        assertEquals(-1.0, BasicMath.round(-0.5))
        assertEquals(3.0, BasicMath.round(2.5))
        assertEquals(-3.0, BasicMath.round(-2.5))
        assertEquals(2.0, BasicMath.round(2.4))
        assertEquals(3.0, BasicMath.round(2.6))
        assertEquals(0.0, BasicMath.round(0.0))
    }

    @Test
    fun frexpRoundTrip() {
        val testValues = doubleArrayOf(1.0, 2.0, 0.5, 3.14159, 1e100, 1e-100, -2.5, 65536.0)
        for (x in testValues) {
            val (m, e) = BasicMath.frexp(x)
            assertTrue(kotlin.math.abs(m) >= 0.5 && kotlin.math.abs(m) < 1.0,
                "frexp($x): mantissa $m not in [0.5, 1)")
            // Reconstruct: m * 2^e should equal x
            val reconstructed = BasicMath.ldexp(m, e)
            assertEquals(x, reconstructed, "frexp/ldexp round-trip failed for $x")
        }
    }

    @Test
    fun frexpSpecialValues() {
        val (m0, e0) = BasicMath.frexp(0.0)
        assertEquals(0.0, m0)
        assertEquals(0, e0)
        val (mInf, eInf) = BasicMath.frexp(Double.POSITIVE_INFINITY)
        assertEquals(Double.POSITIVE_INFINITY, mInf)
        assertEquals(0, eInf)
        val (mNan, _) = BasicMath.frexp(Double.NaN)
        assertTrue(mNan.isNaN())
    }

    @Test
    fun ldexpBasics() {
        assertEquals(8.0, BasicMath.ldexp(1.0, 3))
        assertEquals(0.125, BasicMath.ldexp(1.0, -3))
        assertEquals(0.0, BasicMath.ldexp(0.0, 100))
        assertEquals(1.0, BasicMath.ldexp(1.0, 0))
        // overflow
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.ldexp(1.0, 2000))
        // underflow to zero
        assertEquals(0.0, BasicMath.ldexp(1.0, -2000))
    }

    @Test
    fun modfBasics() {
        val (i1, f1) = BasicMath.modf(3.75)
        assertEquals(3.0, i1)
        assertEquals(0.75, f1)  // exact

        val (i2, f2) = BasicMath.modf(-3.75)
        assertEquals(-3.0, i2)
        assertEquals(-0.75, f2)

        val (i3, f3) = BasicMath.modf(0.0)
        assertEquals(0.0, i3)
        assertEquals(0.0, f3)

        val (i4, f4) = BasicMath.modf(Double.POSITIVE_INFINITY)
        assertEquals(Double.POSITIVE_INFINITY, i4)
        assertEquals(0.0, f4)
    }

    @Test
    fun ldexpPowerCheck() {
        // Cross-check with 2.0.pow for small exponents
        for (e in -50..50) {
            val expected = 2.0.pow(e)
            assertEquals(expected, BasicMath.ldexp(1.0, e))
        }
    }
}

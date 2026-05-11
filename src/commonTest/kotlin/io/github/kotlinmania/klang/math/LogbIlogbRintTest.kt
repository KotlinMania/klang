package io.github.kotlinmania.klang.math

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogbIlogbRintTest {

    // ---- logb / ilogb ---------------------------------------------------

    @Test
    fun ilogbNormalPowersOfTwo() {
        for (e in -1021..1023) {
            val x = 2.0.pow(e)
            assertEquals(e, BasicMath.ilogb(x), "ilogb(2^$e)")
            assertEquals(e, BasicMath.ilogb(-x), "ilogb(-2^$e)")
        }
    }

    @Test
    fun logbNormalPowersOfTwo() {
        for (e in -1021..1023) {
            val x = 2.0.pow(e)
            assertEquals(e.toDouble(), BasicMath.logb(x), "logb(2^$e)")
            assertEquals(e.toDouble(), BasicMath.logb(-x), "logb(-2^$e)")
        }
    }

    @Test
    fun ilogbNonPowerOfTwo() {
        // logb(x) gives floor(log2(|x|)) for finite normals
        assertEquals(0, BasicMath.ilogb(1.5))           // 2^0 <= 1.5 < 2^1
        assertEquals(1, BasicMath.ilogb(3.0))           // 2^1 <= 3.0 < 2^2
        assertEquals(2, BasicMath.ilogb(7.999))         // 2^2 <= 7.999 < 2^3
        assertEquals(-1, BasicMath.ilogb(0.6))          // 2^-1 <= 0.6 < 2^0
        assertEquals(-2, BasicMath.ilogb(0.3))          // 2^-2 <= 0.3 < 2^-1
    }

    @Test
    fun ilogbZeroInfNan() {
        assertEquals(Float64Bits.FP_ILOGB0, BasicMath.ilogb(0.0))
        assertEquals(Float64Bits.FP_ILOGB0, BasicMath.ilogb(-0.0))
        assertEquals(Int.MAX_VALUE, BasicMath.ilogb(Double.POSITIVE_INFINITY))
        assertEquals(Int.MAX_VALUE, BasicMath.ilogb(Double.NEGATIVE_INFINITY))
        assertEquals(Float64Bits.FP_ILOGBNAN, BasicMath.ilogb(Double.NaN))
    }

    @Test
    fun logbZeroInfNan() {
        assertEquals(Double.NEGATIVE_INFINITY, BasicMath.logb(0.0))
        assertEquals(Double.NEGATIVE_INFINITY, BasicMath.logb(-0.0))
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.logb(Double.POSITIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.logb(Double.NEGATIVE_INFINITY))
        assertTrue(BasicMath.logb(Double.NaN).isNaN())
    }

    @Test
    fun ilogbSubnormals() {
        // Largest subnormal: 0x000F_FFFF_FFFF_FFFF * 2^-1074 ≈ 2^-1022 - 2^-1074.
        // Its normalized exponent is -1023.
        val largestSubnormal = Double.fromBits(0x000F_FFFF_FFFF_FFFFL)
        assertEquals(-1023, BasicMath.ilogb(largestSubnormal))

        // Smallest positive subnormal: 0x0000_0000_0000_0001 = 2^-1074.
        val smallestSubnormal = Double.fromBits(0x0000_0000_0000_0001L)
        assertEquals(-1074, BasicMath.ilogb(smallestSubnormal))

        // Mid-subnormal: 2^-1030 ≈ 0x0000_8000_0000_0000.
        // The leading 1 of the mantissa is at bit position (1074 - 1030) = 44.
        val midSubnormal = Double.fromBits(1L shl 44)
        assertEquals(-1030, BasicMath.ilogb(midSubnormal))

        // Negative subnormal mirrors the magnitude.
        assertEquals(-1074, BasicMath.ilogb(-smallestSubnormal))
    }

    @Test
    fun logbSubnormals() {
        val largestSubnormal = Double.fromBits(0x000F_FFFF_FFFF_FFFFL)
        assertEquals(-1023.0, BasicMath.logb(largestSubnormal))
        val smallestSubnormal = Double.fromBits(0x0000_0000_0000_0001L)
        assertEquals(-1074.0, BasicMath.logb(smallestSubnormal))
    }

    @Test
    fun ilogbBoundary() {
        // Smallest normal: 0x0010_0000_0000_0000 = 2^-1022.
        assertEquals(-1022, BasicMath.ilogb(Double.fromBits(0x0010_0000_0000_0000L)))
        // Largest finite: 0x7FEF_FFFF_FFFF_FFFF ≈ (2 - 2^-52) * 2^1023.
        assertEquals(1023, BasicMath.ilogb(Double.MAX_VALUE))
    }

    // ---- rint / nearbyint ----------------------------------------------

    @Test
    fun rintHalfToEven() {
        // Ties round toward even; the sign of the result follows the input when
        // the rounded magnitude is zero (IEEE 754).
        assertEquals(0.0.toRawBits(), BasicMath.rint(0.5).toRawBits())
        assertEquals(2.0, BasicMath.rint(1.5))
        assertEquals(2.0, BasicMath.rint(2.5))
        assertEquals(4.0, BasicMath.rint(3.5))
        assertEquals((-0.0).toRawBits(), BasicMath.rint(-0.5).toRawBits())
        assertEquals(-2.0, BasicMath.rint(-1.5))
        assertEquals(-2.0, BasicMath.rint(-2.5))
    }

    @Test
    fun rintNonTies() {
        assertEquals(2.0, BasicMath.rint(2.3))
        assertEquals(3.0, BasicMath.rint(2.7))
        assertEquals(-2.0, BasicMath.rint(-2.3))
        assertEquals(-3.0, BasicMath.rint(-2.7))
    }

    @Test
    fun rintSpecialValues() {
        assertTrue(BasicMath.rint(Double.NaN).isNaN())
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.rint(Double.POSITIVE_INFINITY))
        assertEquals(Double.NEGATIVE_INFINITY, BasicMath.rint(Double.NEGATIVE_INFINITY))
        assertEquals(0.0, BasicMath.rint(0.0))
        // -0.0 round-trips its sign on most platforms; check by bits.
        assertEquals((-0.0).toRawBits(), BasicMath.rint(-0.0).toRawBits())
    }

    @Test
    fun nearbyintMatchesRint() {
        val values = doubleArrayOf(
            0.0, -0.0, 0.5, 1.5, 2.5, 3.5, -0.5, -1.5,
            2.3, 2.7, -2.3, -2.7,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.MAX_VALUE, Double.MIN_VALUE, 1e100, 1e-100,
        )
        for (x in values) {
            assertEquals(
                BasicMath.rint(x).toRawBits(),
                BasicMath.nearbyint(x).toRawBits(),
                "nearbyint should match rint for $x",
            )
        }
        // NaN handled separately because NaN != NaN.
        assertTrue(BasicMath.nearbyint(Double.NaN).isNaN())
    }

    // ---- scalbn / scalbln ----------------------------------------------

    @Test
    fun scalbnMatchesLdexp() {
        val mantissas = doubleArrayOf(1.0, 1.5, -2.25, 3.14159, 0.5, 1e-10, 1e10)
        for (m in mantissas) {
            for (n in intArrayOf(-100, -10, -1, 0, 1, 10, 100)) {
                assertEquals(
                    BasicMath.ldexp(m, n),
                    BasicMath.scalbn(m, n),
                    "scalbn($m, $n) should match ldexp",
                )
            }
        }
    }

    @Test
    fun scalbnPowerCheck() {
        for (n in -50..50) {
            assertEquals(2.0.pow(n), BasicMath.scalbn(1.0, n))
        }
    }

    @Test
    fun scalblnSaturatesAtIntBounds() {
        // scalbln(1.0, huge) should overflow to +Inf and never crash.
        assertEquals(Double.POSITIVE_INFINITY, BasicMath.scalbln(1.0, Long.MAX_VALUE))
        assertEquals(0.0, BasicMath.scalbln(1.0, Long.MIN_VALUE))
    }

    @Test
    fun scalblnMatchesScalbnInRange() {
        for (n in intArrayOf(-1000, -100, -1, 0, 1, 100, 1000)) {
            assertEquals(
                BasicMath.scalbn(1.5, n),
                BasicMath.scalbln(1.5, n.toLong()),
            )
        }
    }
}

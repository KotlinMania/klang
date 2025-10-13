package ai.solace.klang.fp

/**
 * CLongDouble: An intent-driven representation of C's `long double` with explicit flavor.
 *
 * Why: C `long double` differs across platforms (double64, x87 extended80, IEEE-754 binary128).
 * We expose a stable API that selects a flavor explicitly or via AUTO (profiled later).
 *
 * Notes:
 * - DOUBLE64 maps to C `double` semantics (common on Windows/MSVC, macOS arm64/musl).
 * - EXTENDED80 approximates x87 80-bit using a double-double core and (later) rounds to 64-bit
 *   significand + 15-bit exponent model. For now this routes to CFloat128 and leaves rounding TODO.
 * - IEEE128 uses CFloat128 (double-double) with rounding hooks to 113-bit mantissa semantics.
 *
 * This class is intentionally small; arithmetic delegates to underlying representations.
 */
class CLongDouble private constructor(
    val flavor: Flavor,
    private val d: CDouble?,
    private val q: CFloat128?,
) {
    enum class Flavor { AUTO, DOUBLE64, EXTENDED80, IEEE128 }

    fun toDouble(): Double = when (flavorResolved()) {
        Flavor.DOUBLE64 -> d!!.toDouble()
        Flavor.EXTENDED80, Flavor.IEEE128 -> q!!.toDouble()
        else -> error("unreachable")
    }

    fun toCFloat128(): CFloat128 = when (flavorResolved()) {
        Flavor.DOUBLE64 -> CFloat128.fromDouble(d!!.toDouble())
        Flavor.EXTENDED80, Flavor.IEEE128 -> q!!
        else -> error("unreachable")
    }

    operator fun plus(other: CLongDouble): CLongDouble = operate(other) { a, b ->
        when (flavorResolved()) {
            Flavor.DOUBLE64 -> ofDouble(a.d!!.toDouble() + b.d!!.toDouble(), Flavor.DOUBLE64)
            Flavor.EXTENDED80 -> ofCFloat128(a.q!! + b.q!!, Flavor.EXTENDED80)
            Flavor.IEEE128 -> ofCFloat128(roundToIeee128(a.q!! + b.q!!), Flavor.IEEE128)
            else -> error("unreachable")
        }
    }

    operator fun minus(other: CLongDouble): CLongDouble = operate(other) { a, b ->
        when (flavorResolved()) {
            Flavor.DOUBLE64 -> ofDouble(a.d!!.toDouble() - b.d!!.toDouble(), Flavor.DOUBLE64)
            Flavor.EXTENDED80 -> ofCFloat128(a.q!! - b.q!!, Flavor.EXTENDED80)
            Flavor.IEEE128 -> ofCFloat128(roundToIeee128(a.q!! - b.q!!), Flavor.IEEE128)
            else -> error("unreachable")
        }
    }

    operator fun times(other: CLongDouble): CLongDouble = operate(other) { a, b ->
        when (flavorResolved()) {
            Flavor.DOUBLE64 -> ofDouble(a.d!!.toDouble() * b.d!!.toDouble(), Flavor.DOUBLE64)
            Flavor.EXTENDED80 -> ofCFloat128(a.q!! * b.q!!, Flavor.EXTENDED80)
            Flavor.IEEE128 -> ofCFloat128(roundToIeee128(a.q!! * b.q!!), Flavor.IEEE128)
            else -> error("unreachable")
        }
    }

    operator fun div(other: CLongDouble): CLongDouble = operate(other) { a, b ->
        when (flavorResolved()) {
            Flavor.DOUBLE64 -> ofDouble(a.d!!.toDouble() / b.d!!.toDouble(), Flavor.DOUBLE64)
            Flavor.EXTENDED80 -> ofCFloat128(a.q!! / b.q!!, Flavor.EXTENDED80)
            Flavor.IEEE128 -> ofCFloat128(roundToIeee128(a.q!! / b.q!!), Flavor.IEEE128)
            else -> error("unreachable")
        }
    }

    private fun operate(other: CLongDouble, f: (CLongDouble, CLongDouble) -> CLongDouble): CLongDouble {
        val a = coerceFlavor(this, flavorResolved())
        val b = coerceFlavor(other, flavorResolved())
        return f(a, b)
    }

    private fun flavorResolved(): Flavor = when (flavor) {
        Flavor.AUTO -> DefaultFlavorProvider.default
        else -> flavor
    }

    companion object {
        fun ofDouble(value: Double, flavor: Flavor = Flavor.AUTO): CLongDouble {
            val resolvedFlavor = if (flavor == Flavor.AUTO) DefaultFlavorProvider.default else flavor
            return when (resolvedFlavor) {
                Flavor.DOUBLE64 -> CLongDouble(resolvedFlavor, CDouble.fromDouble(value), null)
                Flavor.EXTENDED80, Flavor.IEEE128 -> CLongDouble(resolvedFlavor, null, CFloat128.fromDouble(value))
                Flavor.AUTO -> error("AUTO must be resolved")
            }
        }

        fun ofCFloat128(value: CFloat128, flavor: Flavor): CLongDouble =
            CLongDouble(flavor, null, value)

        fun fromCDouble(value: CDouble, flavor: Flavor = Flavor.AUTO): CLongDouble =
            CLongDouble(flavor, value, null)

        fun fromCFloat128(value: CFloat128, flavor: Flavor): CLongDouble =
            CLongDouble(flavor, null, value)

        // Future: host ABI probing sets default at build/runtime
        object DefaultFlavorProvider {
            var default: Flavor = Flavor.DOUBLE64
        }

        // Placeholder IEEE-128 rounding hook (round-to-nearest-even at 113-bit mantissa)
        private fun roundToIeee128(v: CFloat128): CFloat128 {
            // TODO: Implement mantissa truncation to 113 bits. For now pass-through.
            return v
        }

        private fun coerceFlavor(v: CLongDouble, flavor: Flavor): CLongDouble = when (flavor) {
            Flavor.DOUBLE64 -> if (v.d != null) v else ofDouble(v.toDouble(), Flavor.DOUBLE64)
            Flavor.EXTENDED80 -> if (v.q != null) v else ofCFloat128(CFloat128.fromDouble(v.toDouble()), Flavor.EXTENDED80)
            Flavor.IEEE128 -> if (v.q != null) v else ofCFloat128(CFloat128.fromDouble(v.toDouble()), Flavor.IEEE128)
            Flavor.AUTO -> error("AUTO must be resolved")
        }
    }
}

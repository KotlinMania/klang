package ai.solace.klang.mem

import ai.solace.klang.fp.CBF16
import ai.solace.klang.fp.CFloat128
import ai.solace.klang.fp.CFloat16
import ai.solace.klang.fp.CFloat32
import ai.solace.klang.fp.CFloat64
import ai.solace.klang.fp.CLongDouble

/**
 * Heap-backed floating-point arithmetic operators.
 *
 * This file gives every C-style heap-resident FP scalar (`CFloatVar`,
 * `CFloat32Var`, `CFloat64Var`, `CFloat128Var`, `CLongDoubleVar`,
 * `CFloat16Var`, `CBF16Var`) a complete arithmetic surface so that
 * client code never needs to drop down to the wrapper types
 * (`CFloat32`, `CFloat64`, `CFloat128`, …) or to native Kotlin
 * scalars to do math.
 *
 * Two operator families are provided per type:
 *
 * - **Compound assignment** (`+=`, `-=`, `*=`, `/=`) — mutates `this`
 *   in place: load → compute → store back to the same heap cell.
 *   Mirrors C's `+=`/`-=` semantics. No allocation.
 *
 * - **Binary arithmetic** (`+`, `-`, `*`, `/`) — returns a *new*
 *   heap-resident `*Var` whose backing storage is freshly allocated
 *   on the current `KStack` frame via `CAutos.<type>()`.
 *   The caller must therefore be inside a `KStack.withFrame { … }`.
 *
 * Both families operate heap-to-heap: every operand is a heap address
 * (no native-scalar leakage at the API boundary).
 *
 * @since 0.1.0
 */

// ============================================================================
// CFloat64Var (IEEE-754 binary64, native Double storage)
// ============================================================================

operator fun CFloat64Var.plusAssign(other: CFloat64Var) {
    cfloat64 = cfloat64 + other.cfloat64
}
operator fun CFloat64Var.minusAssign(other: CFloat64Var) {
    cfloat64 = cfloat64 - other.cfloat64
}
operator fun CFloat64Var.timesAssign(other: CFloat64Var) {
    cfloat64 = cfloat64 * other.cfloat64
}
operator fun CFloat64Var.divAssign(other: CFloat64Var) {
    cfloat64 = cfloat64 / other.cfloat64
}

operator fun CFloat64Var.plus(other: CFloat64Var): CFloat64Var {
    val r = CAutos.double()
    r.cfloat64 = this.cfloat64 + other.cfloat64
    return r
}
operator fun CFloat64Var.minus(other: CFloat64Var): CFloat64Var {
    val r = CAutos.double()
    r.cfloat64 = this.cfloat64 - other.cfloat64
    return r
}
operator fun CFloat64Var.times(other: CFloat64Var): CFloat64Var {
    val r = CAutos.double()
    r.cfloat64 = this.cfloat64 * other.cfloat64
    return r
}
operator fun CFloat64Var.div(other: CFloat64Var): CFloat64Var {
    val r = CAutos.double()
    r.cfloat64 = this.cfloat64 / other.cfloat64
    return r
}

operator fun CFloat64Var.unaryMinus(): CFloat64Var {
    val r = CAutos.double()
    r.cfloat64 = -this.cfloat64
    return r
}

// ============================================================================
// CFloat128Var (double-double, ~106 bits of mantissa)
// ============================================================================

operator fun CFloat128Var.plusAssign(other: CFloat128Var) {
    value = value + other.value
}
operator fun CFloat128Var.minusAssign(other: CFloat128Var) {
    value = value - other.value
}
operator fun CFloat128Var.timesAssign(other: CFloat128Var) {
    value = value * other.value
}
operator fun CFloat128Var.divAssign(other: CFloat128Var) {
    value = value / other.value
}

operator fun CFloat128Var.plus(other: CFloat128Var): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value + other.value
    return r
}
operator fun CFloat128Var.minus(other: CFloat128Var): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value - other.value
    return r
}
operator fun CFloat128Var.times(other: CFloat128Var): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value * other.value
    return r
}
operator fun CFloat128Var.div(other: CFloat128Var): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value / other.value
    return r
}

operator fun CFloat128Var.unaryMinus(): CFloat128Var {
    val r = CAutos.float128()
    r.value = -this.value
    return r
}

// ============================================================================
// CFloat32Var (deterministic 32-bit IEEE-754, bit-exact)
// ============================================================================

operator fun CFloat32Var.plusAssign(other: CFloat32Var) {
    value = value + other.value
}
operator fun CFloat32Var.minusAssign(other: CFloat32Var) {
    value = value - other.value
}
operator fun CFloat32Var.timesAssign(other: CFloat32Var) {
    value = value * other.value
}
operator fun CFloat32Var.divAssign(other: CFloat32Var) {
    value = value / other.value
}

operator fun CFloat32Var.plus(other: CFloat32Var): CFloat32Var {
    val r = CAutos.float32()
    r.value = this.value + other.value
    return r
}
operator fun CFloat32Var.minus(other: CFloat32Var): CFloat32Var {
    val r = CAutos.float32()
    r.value = this.value - other.value
    return r
}
operator fun CFloat32Var.times(other: CFloat32Var): CFloat32Var {
    val r = CAutos.float32()
    r.value = this.value * other.value
    return r
}
operator fun CFloat32Var.div(other: CFloat32Var): CFloat32Var {
    val r = CAutos.float32()
    r.value = this.value / other.value
    return r
}

// ============================================================================
// CFloat16Var (IEEE-754 half precision)
// ============================================================================

operator fun CFloat16Var.plusAssign(other: CFloat16Var) {
    value = value + other.value
}
operator fun CFloat16Var.minusAssign(other: CFloat16Var) {
    value = value - other.value
}
operator fun CFloat16Var.timesAssign(other: CFloat16Var) {
    value = value * other.value
}
operator fun CFloat16Var.divAssign(other: CFloat16Var) {
    value = value / other.value
}

operator fun CFloat16Var.plus(other: CFloat16Var): CFloat16Var {
    val r = CAutos.float16()
    r.value = this.value + other.value
    return r
}
operator fun CFloat16Var.minus(other: CFloat16Var): CFloat16Var {
    val r = CAutos.float16()
    r.value = this.value - other.value
    return r
}
operator fun CFloat16Var.times(other: CFloat16Var): CFloat16Var {
    val r = CAutos.float16()
    r.value = this.value * other.value
    return r
}
operator fun CFloat16Var.div(other: CFloat16Var): CFloat16Var {
    val r = CAutos.float16()
    r.value = this.value / other.value
    return r
}

// ============================================================================
// CBF16Var (brain float, 8-bit exponent / 7-bit mantissa)
// ============================================================================

operator fun CBF16Var.plusAssign(other: CBF16Var) {
    value = value + other.value
}
operator fun CBF16Var.minusAssign(other: CBF16Var) {
    value = value - other.value
}
operator fun CBF16Var.timesAssign(other: CBF16Var) {
    value = value * other.value
}
operator fun CBF16Var.divAssign(other: CBF16Var) {
    value = value / other.value
}

operator fun CBF16Var.plus(other: CBF16Var): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value + other.value
    return r
}
operator fun CBF16Var.minus(other: CBF16Var): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value - other.value
    return r
}
operator fun CBF16Var.times(other: CBF16Var): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value * other.value
    return r
}
operator fun CBF16Var.div(other: CBF16Var): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value / other.value
    return r
}

// ============================================================================
// CLongDoubleVar (platform-dependent extended precision)
// ============================================================================

operator fun CLongDoubleVar.plusAssign(other: CLongDoubleVar) {
    value = value + other.value
}
operator fun CLongDoubleVar.minusAssign(other: CLongDoubleVar) {
    value = value - other.value
}
operator fun CLongDoubleVar.timesAssign(other: CLongDoubleVar) {
    value = value * other.value
}
operator fun CLongDoubleVar.divAssign(other: CLongDoubleVar) {
    value = value / other.value
}

operator fun CLongDoubleVar.plus(other: CLongDoubleVar): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value + other.value
    return r
}
operator fun CLongDoubleVar.minus(other: CLongDoubleVar): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value - other.value
    return r
}
operator fun CLongDoubleVar.times(other: CLongDoubleVar): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value * other.value
    return r
}
operator fun CLongDoubleVar.div(other: CLongDoubleVar): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value / other.value
    return r
}

// ============================================================================
// CFloatVar (native Kotlin Float storage — kept for legacy interop;
// new code should prefer CFloat32Var for bit-exact arithmetic)
// ============================================================================

operator fun CFloatVar.plusAssign(other: CFloatVar) {
    value = value + other.value
}
operator fun CFloatVar.minusAssign(other: CFloatVar) {
    value = value - other.value
}
operator fun CFloatVar.timesAssign(other: CFloatVar) {
    value = value * other.value
}
operator fun CFloatVar.divAssign(other: CFloatVar) {
    value = value / other.value
}

operator fun CFloatVar.plus(other: CFloatVar): CFloatVar {
    val r = CAutos.float()
    r.value = this.value + other.value
    return r
}
operator fun CFloatVar.minus(other: CFloatVar): CFloatVar {
    val r = CAutos.float()
    r.value = this.value - other.value
    return r
}
operator fun CFloatVar.times(other: CFloatVar): CFloatVar {
    val r = CAutos.float()
    r.value = this.value * other.value
    return r
}
operator fun CFloatVar.div(other: CFloatVar): CFloatVar {
    val r = CAutos.float()
    r.value = this.value / other.value
    return r
}

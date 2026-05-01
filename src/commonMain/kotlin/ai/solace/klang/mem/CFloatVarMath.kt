package ai.solace.klang.mem

import ai.solace.klang.fp.CBF16
import ai.solace.klang.fp.CFloat128
import ai.solace.klang.fp.CFloat16
import ai.solace.klang.fp.CFloat32
import ai.solace.klang.fp.CFloat64
import ai.solace.klang.fp.CLongDouble
import ai.solace.klang.math.BasicMath
import ai.solace.klang.math.F16Math
import ai.solace.klang.math.F32Math
import ai.solace.klang.math.F64Math

/**
 * Heap-backed floating-point math operators (Slice 3).
 *
 * Each of the 7 heap-resident FP scalars (`CFloatVar`, `CFloat32Var`,
 * `CFloat64Var`, `CFloat128Var`, `CLongDoubleVar`, `CFloat16Var`,
 * `CBF16Var`) gets the full math vocabulary — `sqrt`, `floor`, `ceil`,
 * `trunc`, `round`, `abs`, `copysign`, `frexp`, `ldexp`, `modf` — in
 * two ergonomic forms:
 *
 * - **In-place** (`*Assign`): mutates the receiver's heap slot.
 * - **Allocating**: returns a fresh `*Var` whose backing storage lives
 *   on the current `KStack` frame via `CAutos.<type>()`. The caller
 *   must therefore be inside `KStack.withFrame { … }`.
 *
 * `frexp` returns `Pair<*Var, Int>`; `modf` returns `Pair<*Var, *Var>`.
 * The integer exponent on `ldexp(exp: Int)` and the second component
 * of `frexp` are deliberately kept as `Int` — they are small counts,
 * not heap-storable scalar values.
 *
 * Backend selection:
 *  - `CFloat64Var` → `F64Math` (heap-native, all 10 ops).
 *  - `CFloat32Var` → `F32Math` for sqrt/floor/ceil/trunc/round/abs/
 *    copysign; wrapper-routed for frexp/ldexp/modf.
 *  - `CFloat16Var` → `F16Math` for the same subset; wrapper-routed
 *    otherwise.
 *  - `CBF16Var`, `CFloat128Var`, `CLongDoubleVar` → wrapper-routed
 *    (load → compute via wrapper algorithm → store).
 *  - `CFloatVar` → forwards to `CFloat32Var` semantics.
 *
 * @since 0.1.0
 */

// ============================================================================
// CFloat64Var — backend: F64Math (heap-native, all 10 ops)
// ============================================================================

fun CFloat64Var.sqrtAssign() { F64Math.sqrt(addr, addr) }
fun CFloat64Var.sqrt(): CFloat64Var {
    val r = CAutos.double()
    F64Math.sqrt(r.addr, addr)
    return r
}

fun CFloat64Var.floorAssign() { F64Math.floor(addr, addr) }
fun CFloat64Var.floor(): CFloat64Var {
    val r = CAutos.double()
    F64Math.floor(r.addr, addr)
    return r
}

fun CFloat64Var.ceilAssign() { F64Math.ceil(addr, addr) }
fun CFloat64Var.ceil(): CFloat64Var {
    val r = CAutos.double()
    F64Math.ceil(r.addr, addr)
    return r
}

fun CFloat64Var.truncAssign() { F64Math.trunc(addr, addr) }
fun CFloat64Var.trunc(): CFloat64Var {
    val r = CAutos.double()
    F64Math.trunc(r.addr, addr)
    return r
}

fun CFloat64Var.roundAssign() { F64Math.round(addr, addr) }
fun CFloat64Var.round(): CFloat64Var {
    val r = CAutos.double()
    F64Math.round(r.addr, addr)
    return r
}

fun CFloat64Var.absAssign() { F64Math.abs(addr, addr) }
fun CFloat64Var.abs(): CFloat64Var {
    val r = CAutos.double()
    F64Math.abs(r.addr, addr)
    return r
}

fun CFloat64Var.copysignAssign(sign: CFloat64Var) { F64Math.copysign(addr, addr, sign.addr) }
fun CFloat64Var.copysign(sign: CFloat64Var): CFloat64Var {
    val r = CAutos.double()
    F64Math.copysign(r.addr, addr, sign.addr)
    return r
}

/**
 * Decompose into (mantissa in [0.5, 1.0), exponent).
 * Returns a freshly allocated mantissa cell on the current `KStack`
 * frame and the integer exponent.
 */
fun CFloat64Var.frexp(): Pair<CFloat64Var, Int> {
    val r = CAutos.double()
    val e = F64Math.frexp(r.addr, addr)
    return r to e
}

fun CFloat64Var.ldexpAssign(exp: Int) { F64Math.ldexp(addr, addr, exp) }
fun CFloat64Var.ldexp(exp: Int): CFloat64Var {
    val r = CAutos.double()
    F64Math.ldexp(r.addr, addr, exp)
    return r
}

/** Decompose into (integer-part, fractional-part), both freshly allocated. */
fun CFloat64Var.modf(): Pair<CFloat64Var, CFloat64Var> {
    val i = CAutos.double()
    val f = CAutos.double()
    F64Math.modf(i.addr, f.addr, addr)
    return i to f
}

// ============================================================================
// CFloat128Var — wrapper-routed (load .value → compute → store .value)
// ============================================================================

fun CFloat128Var.sqrtAssign() { value = value.sqrt() }
fun CFloat128Var.sqrt(): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value.sqrt()
    return r
}

fun CFloat128Var.floorAssign() { value = value.floor() }
fun CFloat128Var.floor(): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value.floor()
    return r
}

fun CFloat128Var.ceilAssign() { value = value.ceil() }
fun CFloat128Var.ceil(): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value.ceil()
    return r
}

fun CFloat128Var.truncAssign() { value = value.trunc() }
fun CFloat128Var.trunc(): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value.trunc()
    return r
}

fun CFloat128Var.roundAssign() { value = value.round() }
fun CFloat128Var.round(): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value.round()
    return r
}

fun CFloat128Var.absAssign() {
    val v = value
    value = if (v.hi < 0.0) CFloat128(-v.hi, -v.lo) else v
}
fun CFloat128Var.abs(): CFloat128Var {
    val r = CAutos.float128()
    val v = this.value
    r.value = if (v.hi < 0.0) CFloat128(-v.hi, -v.lo) else v
    return r
}

fun CFloat128Var.copysignAssign(sign: CFloat128Var) {
    val v = value
    val s = sign.value
    val mag = if (v.hi < 0.0) CFloat128(-v.hi, -v.lo) else v
    value = if (s.hi < 0.0) CFloat128(-mag.hi, -mag.lo) else mag
}
fun CFloat128Var.copysign(sign: CFloat128Var): CFloat128Var {
    val r = CAutos.float128()
    val v = this.value
    val s = sign.value
    val mag = if (v.hi < 0.0) CFloat128(-v.hi, -v.lo) else v
    r.value = if (s.hi < 0.0) CFloat128(-mag.hi, -mag.lo) else mag
    return r
}

fun CFloat128Var.frexp(): Pair<CFloat128Var, Int> {
    val r = CAutos.float128()
    val (m, e) = this.value.frexp()
    r.value = m
    return r to e
}

fun CFloat128Var.ldexpAssign(exp: Int) { value = value.ldexp(exp) }
fun CFloat128Var.ldexp(exp: Int): CFloat128Var {
    val r = CAutos.float128()
    r.value = this.value.ldexp(exp)
    return r
}

fun CFloat128Var.modf(): Pair<CFloat128Var, CFloat128Var> {
    val i = CAutos.float128()
    val f = CAutos.float128()
    val (ip, fp) = this.value.modf()
    i.value = ip
    f.value = fp
    return i to f
}

// ============================================================================
// CFloat32Var — backend: F32Math for sqrt/round modes/abs/copysign;
// wrapper-routed for frexp/ldexp/modf.
// ============================================================================

fun CFloat32Var.sqrtAssign() { F32Math.sqrt(addr, addr) }
fun CFloat32Var.sqrt(): CFloat32Var {
    val r = CAutos.float32()
    F32Math.sqrt(r.addr, addr)
    return r
}

fun CFloat32Var.floorAssign() { F32Math.floor(addr, addr) }
fun CFloat32Var.floor(): CFloat32Var {
    val r = CAutos.float32()
    F32Math.floor(r.addr, addr)
    return r
}

fun CFloat32Var.ceilAssign() { F32Math.ceil(addr, addr) }
fun CFloat32Var.ceil(): CFloat32Var {
    val r = CAutos.float32()
    F32Math.ceil(r.addr, addr)
    return r
}

fun CFloat32Var.truncAssign() { F32Math.trunc(addr, addr) }
fun CFloat32Var.trunc(): CFloat32Var {
    val r = CAutos.float32()
    F32Math.trunc(r.addr, addr)
    return r
}

fun CFloat32Var.roundAssign() { F32Math.round(addr, addr) }
fun CFloat32Var.round(): CFloat32Var {
    val r = CAutos.float32()
    F32Math.round(r.addr, addr)
    return r
}

fun CFloat32Var.absAssign() { F32Math.abs(addr, addr) }
fun CFloat32Var.abs(): CFloat32Var {
    val r = CAutos.float32()
    F32Math.abs(r.addr, addr)
    return r
}

fun CFloat32Var.copysignAssign(sign: CFloat32Var) { F32Math.copysign(addr, addr, sign.addr) }
fun CFloat32Var.copysign(sign: CFloat32Var): CFloat32Var {
    val r = CAutos.float32()
    F32Math.copysign(r.addr, addr, sign.addr)
    return r
}

fun CFloat32Var.frexp(): Pair<CFloat32Var, Int> {
    val r = CAutos.float32()
    val (m, e) = this.value.frexp()
    r.value = m
    return r to e
}

fun CFloat32Var.ldexpAssign(exp: Int) { value = value.ldexp(exp) }
fun CFloat32Var.ldexp(exp: Int): CFloat32Var {
    val r = CAutos.float32()
    r.value = this.value.ldexp(exp)
    return r
}

fun CFloat32Var.modf(): Pair<CFloat32Var, CFloat32Var> {
    val i = CAutos.float32()
    val f = CAutos.float32()
    val (ip, fp) = this.value.modf()
    i.value = ip
    f.value = fp
    return i to f
}

// ============================================================================
// CFloat16Var — backend: F16Math (subset); wrapper-routed otherwise.
// ============================================================================

fun CFloat16Var.sqrtAssign() { F16Math.sqrt(addr, addr) }
fun CFloat16Var.sqrt(): CFloat16Var {
    val r = CAutos.float16()
    F16Math.sqrt(r.addr, addr)
    return r
}

fun CFloat16Var.floorAssign() { F16Math.floor(addr, addr) }
fun CFloat16Var.floor(): CFloat16Var {
    val r = CAutos.float16()
    F16Math.floor(r.addr, addr)
    return r
}

fun CFloat16Var.ceilAssign() { F16Math.ceil(addr, addr) }
fun CFloat16Var.ceil(): CFloat16Var {
    val r = CAutos.float16()
    F16Math.ceil(r.addr, addr)
    return r
}

fun CFloat16Var.truncAssign() { F16Math.trunc(addr, addr) }
fun CFloat16Var.trunc(): CFloat16Var {
    val r = CAutos.float16()
    F16Math.trunc(r.addr, addr)
    return r
}

fun CFloat16Var.roundAssign() { F16Math.round(addr, addr) }
fun CFloat16Var.round(): CFloat16Var {
    val r = CAutos.float16()
    F16Math.round(r.addr, addr)
    return r
}

fun CFloat16Var.absAssign() { F16Math.abs(addr, addr) }
fun CFloat16Var.abs(): CFloat16Var {
    val r = CAutos.float16()
    F16Math.abs(r.addr, addr)
    return r
}

fun CFloat16Var.copysignAssign(sign: CFloat16Var) { F16Math.copysign(addr, addr, sign.addr) }
fun CFloat16Var.copysign(sign: CFloat16Var): CFloat16Var {
    val r = CAutos.float16()
    F16Math.copysign(r.addr, addr, sign.addr)
    return r
}

fun CFloat16Var.frexp(): Pair<CFloat16Var, Int> {
    val r = CAutos.float16()
    val (m, e) = this.value.frexp()
    r.value = m
    return r to e
}

fun CFloat16Var.ldexpAssign(exp: Int) { value = value.ldexp(exp) }
fun CFloat16Var.ldexp(exp: Int): CFloat16Var {
    val r = CAutos.float16()
    r.value = this.value.ldexp(exp)
    return r
}

fun CFloat16Var.modf(): Pair<CFloat16Var, CFloat16Var> {
    val i = CAutos.float16()
    val f = CAutos.float16()
    val (ip, fp) = this.value.modf()
    i.value = ip
    f.value = fp
    return i to f
}

// ============================================================================
// CBF16Var — wrapper-routed (no heap-native math object today).
// ============================================================================

fun CBF16Var.sqrtAssign() { value = value.sqrt() }
fun CBF16Var.sqrt(): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value.sqrt()
    return r
}

fun CBF16Var.floorAssign() { value = value.floor() }
fun CBF16Var.floor(): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value.floor()
    return r
}

fun CBF16Var.ceilAssign() { value = value.ceil() }
fun CBF16Var.ceil(): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value.ceil()
    return r
}

fun CBF16Var.truncAssign() { value = value.trunc() }
fun CBF16Var.trunc(): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value.trunc()
    return r
}

fun CBF16Var.roundAssign() { value = value.round() }
fun CBF16Var.round(): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value.round()
    return r
}

fun CBF16Var.absAssign() {
    val f = value.toFloat()
    value = CBF16.fromFloat(if (f < 0f) -f else f)
}
fun CBF16Var.abs(): CBF16Var {
    val r = CAutos.bfloat16()
    val f = this.value.toFloat()
    r.value = CBF16.fromFloat(if (f < 0f) -f else f)
    return r
}

fun CBF16Var.copysignAssign(sign: CBF16Var) {
    val mag = kotlin.math.abs(value.toFloat())
    val signed = if (sign.value.toFloat() < 0f) -mag else mag
    value = CBF16.fromFloat(signed)
}
fun CBF16Var.copysign(sign: CBF16Var): CBF16Var {
    val r = CAutos.bfloat16()
    val mag = kotlin.math.abs(this.value.toFloat())
    val signed = if (sign.value.toFloat() < 0f) -mag else mag
    r.value = CBF16.fromFloat(signed)
    return r
}

fun CBF16Var.frexp(): Pair<CBF16Var, Int> {
    val r = CAutos.bfloat16()
    val (m, e) = this.value.frexp()
    r.value = m
    return r to e
}

fun CBF16Var.ldexpAssign(exp: Int) { value = value.ldexp(exp) }
fun CBF16Var.ldexp(exp: Int): CBF16Var {
    val r = CAutos.bfloat16()
    r.value = this.value.ldexp(exp)
    return r
}

fun CBF16Var.modf(): Pair<CBF16Var, CBF16Var> {
    val i = CAutos.bfloat16()
    val f = CAutos.bfloat16()
    val (ip, fp) = this.value.modf()
    i.value = ip
    f.value = fp
    return i to f
}

// ============================================================================
// CLongDoubleVar — flavor-aware; wrapper-routed via CLongDouble methods.
// Slice 2 fixed a latent NPE in CLongDoubleVar.value getter; these ops
// rely on the same EXTENDED80 convention.
// ============================================================================

fun CLongDoubleVar.sqrtAssign() { value = value.sqrt() }
fun CLongDoubleVar.sqrt(): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value.sqrt()
    return r
}

fun CLongDoubleVar.floorAssign() { value = value.floor() }
fun CLongDoubleVar.floor(): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value.floor()
    return r
}

fun CLongDoubleVar.ceilAssign() { value = value.ceil() }
fun CLongDoubleVar.ceil(): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value.ceil()
    return r
}

fun CLongDoubleVar.truncAssign() { value = value.trunc() }
fun CLongDoubleVar.trunc(): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value.trunc()
    return r
}

fun CLongDoubleVar.roundAssign() { value = value.round() }
fun CLongDoubleVar.round(): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value.round()
    return r
}

fun CLongDoubleVar.absAssign() {
    val v = value
    value = if (v.toDouble() < 0.0) CLongDouble.ofDouble(-v.toDouble()) else v
}
fun CLongDoubleVar.abs(): CLongDoubleVar {
    val r = CAutos.longdouble()
    val v = this.value
    r.value = if (v.toDouble() < 0.0) CLongDouble.ofDouble(-v.toDouble()) else v
    return r
}

fun CLongDoubleVar.copysignAssign(sign: CLongDoubleVar) {
    val mag = kotlin.math.abs(value.toDouble())
    val signed = if (sign.value.toDouble() < 0.0) -mag else mag
    value = CLongDouble.ofDouble(signed)
}
fun CLongDoubleVar.copysign(sign: CLongDoubleVar): CLongDoubleVar {
    val r = CAutos.longdouble()
    val mag = kotlin.math.abs(this.value.toDouble())
    val signed = if (sign.value.toDouble() < 0.0) -mag else mag
    r.value = CLongDouble.ofDouble(signed)
    return r
}

fun CLongDoubleVar.frexp(): Pair<CLongDoubleVar, Int> {
    val r = CAutos.longdouble()
    val (m, e) = this.value.frexp()
    r.value = m
    return r to e
}

fun CLongDoubleVar.ldexpAssign(exp: Int) { value = value.ldexp(exp) }
fun CLongDoubleVar.ldexp(exp: Int): CLongDoubleVar {
    val r = CAutos.longdouble()
    r.value = this.value.ldexp(exp)
    return r
}

fun CLongDoubleVar.modf(): Pair<CLongDoubleVar, CLongDoubleVar> {
    val i = CAutos.longdouble()
    val f = CAutos.longdouble()
    val (ip, fp) = this.value.modf()
    i.value = ip
    f.value = fp
    return i to f
}

// ============================================================================
// CFloatVar — native Float storage. Routed through the binary32 bit kernels in
// Float32Math (no kotlin.math.* shortcuts).
// ============================================================================

private fun applyF32(v: Float, op: (Int) -> Int): Float =
    Float.fromBits(op(v.toRawBits()))

fun CFloatVar.sqrtAssign() { value = applyF32(value, ai.solace.klang.fp.Float32Math::sqrtBits) }
fun CFloatVar.sqrt(): CFloatVar {
    val r = CAutos.float()
    r.value = applyF32(this.value, ai.solace.klang.fp.Float32Math::sqrtBits)
    return r
}

fun CFloatVar.floorAssign() { value = applyF32(value, ai.solace.klang.fp.Float32Math::floorBits) }
fun CFloatVar.floor(): CFloatVar {
    val r = CAutos.float()
    r.value = applyF32(this.value, ai.solace.klang.fp.Float32Math::floorBits)
    return r
}

fun CFloatVar.ceilAssign() { value = applyF32(value, ai.solace.klang.fp.Float32Math::ceilBits) }
fun CFloatVar.ceil(): CFloatVar {
    val r = CAutos.float()
    r.value = applyF32(this.value, ai.solace.klang.fp.Float32Math::ceilBits)
    return r
}

fun CFloatVar.truncAssign() { value = applyF32(value, ai.solace.klang.fp.Float32Math::truncBits) }
fun CFloatVar.trunc(): CFloatVar {
    val r = CAutos.float()
    r.value = applyF32(this.value, ai.solace.klang.fp.Float32Math::truncBits)
    return r
}

fun CFloatVar.roundAssign() { value = applyF32(value, ai.solace.klang.fp.Float32Math::roundBits) }
fun CFloatVar.round(): CFloatVar {
    val r = CAutos.float()
    r.value = applyF32(this.value, ai.solace.klang.fp.Float32Math::roundBits)
    return r
}

// abs / copysign are pure single-bit manipulations: |x| clears bit 31; copysign
// transplants bit 31. No native math library involved.
fun CFloatVar.absAssign() {
    val bits = value.toRawBits() and 0x7FFFFFFF
    value = Float.fromBits(bits)
}
fun CFloatVar.abs(): CFloatVar {
    val r = CAutos.float()
    r.value = Float.fromBits(this.value.toRawBits() and 0x7FFFFFFF)
    return r
}

fun CFloatVar.copysignAssign(sign: CFloatVar) {
    val mag = value.toRawBits() and 0x7FFFFFFF
    val s = sign.value.toRawBits() and 0x80000000.toInt()
    value = Float.fromBits(mag or s)
}
fun CFloatVar.copysign(sign: CFloatVar): CFloatVar {
    val r = CAutos.float()
    val mag = this.value.toRawBits() and 0x7FFFFFFF
    val s = sign.value.toRawBits() and 0x80000000.toInt()
    r.value = Float.fromBits(mag or s)
    return r
}

fun CFloatVar.frexp(): Pair<CFloatVar, Int> {
    val r = CAutos.float()
    val (mBits, e) = ai.solace.klang.fp.Float32Math.frexpBits(this.value.toRawBits())
    r.value = Float.fromBits(mBits)
    return r to e
}

fun CFloatVar.ldexpAssign(exp: Int) {
    value = Float.fromBits(ai.solace.klang.fp.Float32Math.ldexpBits(value.toRawBits(), exp))
}
fun CFloatVar.ldexp(exp: Int): CFloatVar {
    val r = CAutos.float()
    r.value = Float.fromBits(ai.solace.klang.fp.Float32Math.ldexpBits(this.value.toRawBits(), exp))
    return r
}

fun CFloatVar.modf(): Pair<CFloatVar, CFloatVar> {
    val i = CAutos.float()
    val f = CAutos.float()
    val (iBits, fBits) = ai.solace.klang.fp.Float32Math.modfBits(this.value.toRawBits())
    i.value = Float.fromBits(iBits)
    f.value = Float.fromBits(fBits)
    return i to f
}

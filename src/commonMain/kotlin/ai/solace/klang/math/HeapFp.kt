package ai.solace.klang.math

import ai.solace.klang.mem.CBF16Var
import ai.solace.klang.mem.CFloat128Var
import ai.solace.klang.mem.CFloat16Var
import ai.solace.klang.mem.CFloat32Var
import ai.solace.klang.mem.CFloat64Var
import ai.solace.klang.mem.CFloatVar
import ai.solace.klang.mem.CLongDoubleVar
import ai.solace.klang.mem.abs as memAbs
import ai.solace.klang.mem.ceil as memCeil
import ai.solace.klang.mem.copysign as memCopysign
import ai.solace.klang.mem.floor as memFloor
import ai.solace.klang.mem.frexp as memFrexp
import ai.solace.klang.mem.ldexp as memLdexp
import ai.solace.klang.mem.modf as memModf
import ai.solace.klang.mem.round as memRound
import ai.solace.klang.mem.sqrt as memSqrt
import ai.solace.klang.mem.trunc as memTrunc

/**
 * Free-function math layer over heap-resident FP scalars (Slice 3).
 *
 * Lets call sites read like math expressions: `sqrt(v)`, `floor(x)`,
 * `frexp(y)` rather than `v.sqrt()`. Each function is one-line
 * forwarding to the corresponding extension method in
 * `mem/CFloatVarMath.kt`.
 *
 * **Note on shadowing**: these functions live in `ai.solace.klang.math`,
 * a different package from `kotlin.math`. They take heap-resident
 * `*Var` parameters and never accept native scalars, so a name clash
 * with `kotlin.math.sqrt(Double)` cannot occur at a single call site —
 * overload resolution picks the right one based on argument type.
 */

// ---------------- sqrt ----------------
fun sqrt(v: CFloat64Var): CFloat64Var = v.memSqrt()
fun sqrt(v: CFloat128Var): CFloat128Var = v.memSqrt()
fun sqrt(v: CFloat32Var): CFloat32Var = v.memSqrt()
fun sqrt(v: CFloat16Var): CFloat16Var = v.memSqrt()
fun sqrt(v: CBF16Var): CBF16Var = v.memSqrt()
fun sqrt(v: CLongDoubleVar): CLongDoubleVar = v.memSqrt()
fun sqrt(v: CFloatVar): CFloatVar = v.memSqrt()

// ---------------- floor ----------------
fun floor(v: CFloat64Var): CFloat64Var = v.memFloor()
fun floor(v: CFloat128Var): CFloat128Var = v.memFloor()
fun floor(v: CFloat32Var): CFloat32Var = v.memFloor()
fun floor(v: CFloat16Var): CFloat16Var = v.memFloor()
fun floor(v: CBF16Var): CBF16Var = v.memFloor()
fun floor(v: CLongDoubleVar): CLongDoubleVar = v.memFloor()
fun floor(v: CFloatVar): CFloatVar = v.memFloor()

// ---------------- ceil ----------------
fun ceil(v: CFloat64Var): CFloat64Var = v.memCeil()
fun ceil(v: CFloat128Var): CFloat128Var = v.memCeil()
fun ceil(v: CFloat32Var): CFloat32Var = v.memCeil()
fun ceil(v: CFloat16Var): CFloat16Var = v.memCeil()
fun ceil(v: CBF16Var): CBF16Var = v.memCeil()
fun ceil(v: CLongDoubleVar): CLongDoubleVar = v.memCeil()
fun ceil(v: CFloatVar): CFloatVar = v.memCeil()

// ---------------- trunc ----------------
fun trunc(v: CFloat64Var): CFloat64Var = v.memTrunc()
fun trunc(v: CFloat128Var): CFloat128Var = v.memTrunc()
fun trunc(v: CFloat32Var): CFloat32Var = v.memTrunc()
fun trunc(v: CFloat16Var): CFloat16Var = v.memTrunc()
fun trunc(v: CBF16Var): CBF16Var = v.memTrunc()
fun trunc(v: CLongDoubleVar): CLongDoubleVar = v.memTrunc()
fun trunc(v: CFloatVar): CFloatVar = v.memTrunc()

// ---------------- round ----------------
fun round(v: CFloat64Var): CFloat64Var = v.memRound()
fun round(v: CFloat128Var): CFloat128Var = v.memRound()
fun round(v: CFloat32Var): CFloat32Var = v.memRound()
fun round(v: CFloat16Var): CFloat16Var = v.memRound()
fun round(v: CBF16Var): CBF16Var = v.memRound()
fun round(v: CLongDoubleVar): CLongDoubleVar = v.memRound()
fun round(v: CFloatVar): CFloatVar = v.memRound()

// ---------------- abs ----------------
fun abs(v: CFloat64Var): CFloat64Var = v.memAbs()
fun abs(v: CFloat128Var): CFloat128Var = v.memAbs()
fun abs(v: CFloat32Var): CFloat32Var = v.memAbs()
fun abs(v: CFloat16Var): CFloat16Var = v.memAbs()
fun abs(v: CBF16Var): CBF16Var = v.memAbs()
fun abs(v: CLongDoubleVar): CLongDoubleVar = v.memAbs()
fun abs(v: CFloatVar): CFloatVar = v.memAbs()

// ---------------- copysign ----------------
fun copysign(v: CFloat64Var, sign: CFloat64Var): CFloat64Var = v.memCopysign(sign)
fun copysign(v: CFloat128Var, sign: CFloat128Var): CFloat128Var = v.memCopysign(sign)
fun copysign(v: CFloat32Var, sign: CFloat32Var): CFloat32Var = v.memCopysign(sign)
fun copysign(v: CFloat16Var, sign: CFloat16Var): CFloat16Var = v.memCopysign(sign)
fun copysign(v: CBF16Var, sign: CBF16Var): CBF16Var = v.memCopysign(sign)
fun copysign(v: CLongDoubleVar, sign: CLongDoubleVar): CLongDoubleVar = v.memCopysign(sign)
fun copysign(v: CFloatVar, sign: CFloatVar): CFloatVar = v.memCopysign(sign)

// ---------------- frexp ----------------
fun frexp(v: CFloat64Var): Pair<CFloat64Var, Int> = v.memFrexp()
fun frexp(v: CFloat128Var): Pair<CFloat128Var, Int> = v.memFrexp()
fun frexp(v: CFloat32Var): Pair<CFloat32Var, Int> = v.memFrexp()
fun frexp(v: CFloat16Var): Pair<CFloat16Var, Int> = v.memFrexp()
fun frexp(v: CBF16Var): Pair<CBF16Var, Int> = v.memFrexp()
fun frexp(v: CLongDoubleVar): Pair<CLongDoubleVar, Int> = v.memFrexp()
fun frexp(v: CFloatVar): Pair<CFloatVar, Int> = v.memFrexp()

// ---------------- ldexp ----------------
fun ldexp(v: CFloat64Var, exp: Int): CFloat64Var = v.memLdexp(exp)
fun ldexp(v: CFloat128Var, exp: Int): CFloat128Var = v.memLdexp(exp)
fun ldexp(v: CFloat32Var, exp: Int): CFloat32Var = v.memLdexp(exp)
fun ldexp(v: CFloat16Var, exp: Int): CFloat16Var = v.memLdexp(exp)
fun ldexp(v: CBF16Var, exp: Int): CBF16Var = v.memLdexp(exp)
fun ldexp(v: CLongDoubleVar, exp: Int): CLongDoubleVar = v.memLdexp(exp)
fun ldexp(v: CFloatVar, exp: Int): CFloatVar = v.memLdexp(exp)

// ---------------- modf ----------------
fun modf(v: CFloat64Var): Pair<CFloat64Var, CFloat64Var> = v.memModf()
fun modf(v: CFloat128Var): Pair<CFloat128Var, CFloat128Var> = v.memModf()
fun modf(v: CFloat32Var): Pair<CFloat32Var, CFloat32Var> = v.memModf()
fun modf(v: CFloat16Var): Pair<CFloat16Var, CFloat16Var> = v.memModf()
fun modf(v: CBF16Var): Pair<CBF16Var, CBF16Var> = v.memModf()
fun modf(v: CLongDoubleVar): Pair<CLongDoubleVar, CLongDoubleVar> = v.memModf()
fun modf(v: CFloatVar): Pair<CFloatVar, CFloatVar> = v.memModf()

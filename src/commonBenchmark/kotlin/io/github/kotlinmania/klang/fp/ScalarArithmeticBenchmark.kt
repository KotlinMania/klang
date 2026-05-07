package io.github.kotlinmania.klang.fp

import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import io.github.kotlinmania.klang.bitwise.BitShiftMode
import io.github.kotlinmania.klang.mem.GlobalHeap
import io.github.kotlinmania.klang.mem.KMalloc
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/**
 * Scalar arithmetic benchmarks comparing
 *   - native Kotlin Float operations
 *   - klang's bit-exact software IEEE-754 operations ([Float32Math])
 *   - the [CFloat32] wrapper around the bit kernel
 *   - native bit shifts vs [BitShiftEngine] (NATIVE mode)
 *
 * Each `@Benchmark` method does one arithmetic operation and returns the
 * result so the JIT can't dead-code-eliminate the work. The framework
 * handles iteration, warmup, and ops/sec aggregation.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
class ScalarArithmeticBenchmark {

    // ---- Float32 sample inputs ----
    private val a: Float = 1.5f
    private val b: Float = 2.7f
    private val aBits: Int = a.toRawBits()
    private val bBits: Int = b.toRawBits()

    // ---- CFloat32 wrapper inputs ----
    private val ca: CFloat32 = CFloat32.fromFloat(1.5f)
    private val cb: CFloat32 = CFloat32.fromFloat(2.7f)

    // ---- Bit-shift inputs ----
    private val word: Long = 0x1234_5678_9ABC_DEF0L
    private val shiftAmount: Int = 17
    private val nativeEngine: BitShiftEngine = BitShiftEngine(BitShiftMode.NATIVE, 64)
    private val arithEngine: BitShiftEngine = BitShiftEngine(BitShiftMode.ARITHMETIC, 64)

    @Setup
    fun setup() {
        GlobalHeap.init(1 shl 20)   // 1 MB
        KMalloc.init(1 shl 18)      // 256 KB
    }

    // ===== Float32 multiply =====

    @Benchmark
    fun nativeMultiplyFloat(): Float = a * b

    @Benchmark
    fun softwareMultiplyFloat32MathBits(): Int = Float32Math.mulBits(aBits, bBits)

    @Benchmark
    fun cfloat32Multiply(): CFloat32 = ca * cb

    // ===== Float32 add =====

    @Benchmark
    fun nativeAddFloat(): Float = a + b

    @Benchmark
    fun softwareAddFloat32MathBits(): Int = Float32Math.addBits(aBits, bBits)

    @Benchmark
    fun cfloat32Add(): CFloat32 = ca + cb

    // ===== Float32 divide =====

    @Benchmark
    fun nativeDivFloat(): Float = a / b

    @Benchmark
    fun softwareDivFloat32MathBits(): Int = Float32Math.divBits(aBits, bBits)

    // ===== Bit shifts (NATIVE engine vs raw operator) =====

    @Benchmark
    fun nativeLeftShift(): Long = word shl shiftAmount

    @Benchmark
    fun engineLeftShiftNative(): Long = nativeEngine.leftShift(word, shiftAmount).value

    @Benchmark
    fun engineLeftShiftArithmetic(): Long = arithEngine.leftShift(word, shiftAmount).value

    @Benchmark
    fun nativeUnsignedRightShift(): Long = word ushr shiftAmount

    @Benchmark
    fun engineUnsignedRightShiftNative(): Long = nativeEngine.unsignedRightShift(word, shiftAmount).value

    @Benchmark
    fun engineUnsignedRightShiftArithmetic(): Long = arithEngine.unsignedRightShift(word, shiftAmount).value

    // ===== Chained operations: a*b + c*d =====
    // Common shape for matrix-style inner loops; keeps three operations
    // visible to the runner per iteration.

    @Benchmark
    fun nativeChainedMulAdd(): Float = a * b + 1.1f * 2.2f

    @Benchmark
    fun softwareChainedMulAdd(): Int {
        val mul1 = Float32Math.mulBits(aBits, bBits)
        val mul2 = Float32Math.mulBits(1.1f.toRawBits(), 2.2f.toRawBits())
        return Float32Math.addBits(mul1, mul2)
    }
}

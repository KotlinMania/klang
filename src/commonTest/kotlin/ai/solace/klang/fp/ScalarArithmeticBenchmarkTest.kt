package ai.solace.klang.fp

import ai.solace.klang.bitwise.BitShiftEngine
import ai.solace.klang.bitwise.BitShiftMode
import ai.solace.klang.mem.GlobalHeap
import ai.solace.klang.mem.KMalloc
import kotlin.test.Test
import kotlin.time.measureTime

/**
 * Benchmark comparing scalar arithmetic operations:
 * - Native Kotlin Float/Double operations
 * - Float32Math software IEEE-754 operations (uses BitShiftEngine)
 * - CFloat32 wrapper operations
 *
 * This benchmark helps identify performance bottlenecks in the
 * software floating-point implementation.
 */
class ScalarArithmeticBenchmarkTest {

    companion object {
        const val WARMUP_ITERATIONS = 100
        const val BENCHMARK_ITERATIONS = 10000
    }

    private fun setup() {
        GlobalHeap.init(1 shl 20)  // 1MB
        KMalloc.init(1 shl 18)      // 256KB
    }

    @Test
    fun benchmarkNativeVsSoftwareMultiply() {
        setup()
        println("\n" + "=".repeat(70))
        println("MULTIPLICATION BENCHMARK: Native vs Software IEEE-754")
        println("=".repeat(70))

        val a = 1.5f
        val b = 2.7f
        val aBits = a.toRawBits()
        val bBits = b.toRawBits()

        // Warm up
        repeat(WARMUP_ITERATIONS) {
            @Suppress("UNUSED_VARIABLE") val r1 = a * b
            @Suppress("UNUSED_VARIABLE") val r2 = Float32Math.mulBits(aBits, bBits)
        }

        // Native multiply
        var nativeResult = 0f
        val nativeTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                nativeResult = a * b
            }
        }

        // Software multiply (Float32Math.mulBits)
        var softwareResult = 0
        val softwareTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                softwareResult = Float32Math.mulBits(aBits, bBits)
            }
        }

        val nativeMs = nativeTime.inWholeNanoseconds / 1_000_000.0
        val softwareMs = softwareTime.inWholeNanoseconds / 1_000_000.0

        println("Native multiply:   ${nativeMs}ms (${nativeMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software multiply: ${softwareMs}ms (${softwareMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software is ${softwareMs / nativeMs}x slower")
        println("Results match: ${nativeResult.toRawBits() == softwareResult}")
    }

    @Test
    fun benchmarkNativeVsSoftwareAdd() {
        setup()
        println("\n" + "=".repeat(70))
        println("ADDITION BENCHMARK: Native vs Software IEEE-754")
        println("=".repeat(70))

        val a = 1.5f
        val b = 2.7f
        val aBits = a.toRawBits()
        val bBits = b.toRawBits()

        // Warm up
        repeat(WARMUP_ITERATIONS) {
            @Suppress("UNUSED_VARIABLE") val r1 = a + b
            @Suppress("UNUSED_VARIABLE") val r2 = Float32Math.addBits(aBits, bBits)
        }

        // Native add
        var nativeResult = 0f
        val nativeTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                nativeResult = a + b
            }
        }

        // Software add (Float32Math.addBits - uses BitShiftEngine)
        var softwareResult = 0
        val softwareTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                softwareResult = Float32Math.addBits(aBits, bBits)
            }
        }

        val nativeMs = nativeTime.inWholeNanoseconds / 1_000_000.0
        val softwareMs = softwareTime.inWholeNanoseconds / 1_000_000.0

        println("Native add:   ${nativeMs}ms (${nativeMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software add: ${softwareMs}ms (${softwareMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software is ${softwareMs / nativeMs}x slower")
        println("Results match: ${nativeResult.toRawBits() == softwareResult}")
    }

    @Test
    fun benchmarkNativeVsSoftwareDivide() {
        setup()
        println("\n" + "=".repeat(70))
        println("DIVISION BENCHMARK: Native vs Software IEEE-754")
        println("=".repeat(70))

        val a = 10.0f
        val b = 3.0f
        val aBits = a.toRawBits()
        val bBits = b.toRawBits()

        // Warm up
        repeat(WARMUP_ITERATIONS) {
            @Suppress("UNUSED_VARIABLE") val r1 = a / b
            @Suppress("UNUSED_VARIABLE") val r2 = Float32Math.divBits(aBits, bBits)
        }

        // Native divide
        var nativeResult = 0f
        val nativeTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                nativeResult = a / b
            }
        }

        // Software divide (Float32Math.divBits - uses BitShiftEngine)
        var softwareResult = 0
        val softwareTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                softwareResult = Float32Math.divBits(aBits, bBits)
            }
        }

        val nativeMs = nativeTime.inWholeNanoseconds / 1_000_000.0
        val softwareMs = softwareTime.inWholeNanoseconds / 1_000_000.0

        println("Native divide:   ${nativeMs}ms (${nativeMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software divide: ${softwareMs}ms (${softwareMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software is ${softwareMs / nativeMs}x slower")
        println("Results match: ${nativeResult.toRawBits() == softwareResult}")
    }

    @Test
    fun benchmarkBitShiftEngineVsNativeShifts() {
        println("\n" + "=".repeat(70))
        println("BIT SHIFT BENCHMARK: BitShiftEngine vs Native shl/shr")
        println("=".repeat(70))

        val engine = BitShiftEngine(BitShiftMode.NATIVE, 64)
        val value = 0x123456789ABCDEF0L
        val shiftAmount = 17

        // Warm up
        repeat(WARMUP_ITERATIONS) {
            @Suppress("UNUSED_VARIABLE") val r1 = value shl shiftAmount
            @Suppress("UNUSED_VARIABLE") val r2 = engine.leftShift(value, shiftAmount)
        }

        // Native left shift
        var nativeResult = 0L
        val nativeTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                nativeResult = value shl shiftAmount
            }
        }

        // BitShiftEngine left shift
        var engineResult = 0L
        val engineTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                engineResult = engine.leftShift(value, shiftAmount).value
            }
        }

        val nativeMs = nativeTime.inWholeNanoseconds / 1_000_000.0
        val engineMs = engineTime.inWholeNanoseconds / 1_000_000.0

        println("Native shl:         ${nativeMs}ms (${nativeMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("BitShiftEngine shl: ${engineMs}ms (${engineMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Engine is ${engineMs / nativeMs}x slower")
        println("Results match: ${nativeResult == engineResult}")

        // Now test right shift
        println()

        val nativeRightTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                nativeResult = value ushr shiftAmount
            }
        }

        val engineRightTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                engineResult = engine.rightShift(value, shiftAmount).value
            }
        }

        val nativeRightMs = nativeRightTime.inWholeNanoseconds / 1_000_000.0
        val engineRightMs = engineRightTime.inWholeNanoseconds / 1_000_000.0

        println("Native ushr:         ${nativeRightMs}ms (${nativeRightMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("BitShiftEngine ushr: ${engineRightMs}ms (${engineRightMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Engine is ${engineRightMs / nativeRightMs}x slower")
    }

    @Test
    fun benchmarkCFloat32Operations() {
        setup()
        println("\n" + "=".repeat(70))
        println("CFloat32 WRAPPER BENCHMARK")
        println("=".repeat(70))

        val a = CFloat32.fromFloat(1.5f)
        val b = CFloat32.fromFloat(2.7f)

        // Warm up
        repeat(WARMUP_ITERATIONS) {
            @Suppress("UNUSED_VARIABLE") val r1 = a + b
            @Suppress("UNUSED_VARIABLE") val r2 = a * b
        }

        // CFloat32 add
        var addResult = CFloat32.ZERO
        val addTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                addResult = a + b
            }
        }

        // CFloat32 multiply
        var mulResult = CFloat32.ZERO
        val mulTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                mulResult = a * b
            }
        }

        val addMs = addTime.inWholeNanoseconds / 1_000_000.0
        val mulMs = mulTime.inWholeNanoseconds / 1_000_000.0

        println("CFloat32 add:      ${addMs}ms (${addMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("CFloat32 multiply: ${mulMs}ms (${mulMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Add result: ${addResult.value}")
        println("Mul result: ${mulResult.value}")
    }

    @Test
    fun benchmarkChainedOperations() {
        setup()
        println("\n" + "=".repeat(70))
        println("CHAINED OPERATIONS BENCHMARK: (a*b + c*d) / e")
        println("=".repeat(70))

        val a = 1.5f
        val b = 2.0f
        val c = 3.0f
        val d = 4.0f
        val e = 2.5f

        val aBits = a.toRawBits()
        val bBits = b.toRawBits()
        val cBits = c.toRawBits()
        val dBits = d.toRawBits()
        val eBits = e.toRawBits()

        // Warm up
        repeat(WARMUP_ITERATIONS) {
            @Suppress("UNUSED_VARIABLE") val r1 = (a * b + c * d) / e
            val ab = Float32Math.mulBits(aBits, bBits)
            val cd = Float32Math.mulBits(cBits, dBits)
            val sum = Float32Math.addBits(ab, cd)
            @Suppress("UNUSED_VARIABLE") val r2 = Float32Math.divBits(sum, eBits)
        }

        // Native chained
        var nativeResult = 0f
        val nativeTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                nativeResult = (a * b + c * d) / e
            }
        }

        // Software chained
        var softwareResult = 0
        val softwareTime = measureTime {
            repeat(BENCHMARK_ITERATIONS) {
                val ab = Float32Math.mulBits(aBits, bBits)
                val cd = Float32Math.mulBits(cBits, dBits)
                val sum = Float32Math.addBits(ab, cd)
                softwareResult = Float32Math.divBits(sum, eBits)
            }
        }

        val nativeMs = nativeTime.inWholeNanoseconds / 1_000_000.0
        val softwareMs = softwareTime.inWholeNanoseconds / 1_000_000.0

        println("Native chained:   ${nativeMs}ms (${nativeMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software chained: ${softwareMs}ms (${softwareMs / BENCHMARK_ITERATIONS * 1_000_000}ns/op)")
        println("Software is ${softwareMs / nativeMs}x slower")
        println("Native result:   ${nativeResult}")
        println("Software result: ${Float.fromBits(softwareResult)}")
    }

    @Test
    fun benchmarkComparison() {
        setup()
        println("\n" + "=".repeat(70))
        println("OVERALL SCALAR ARITHMETIC BENCHMARK SUMMARY")
        println("=".repeat(70))

        val iterations = 100000
        val a = 1.5f
        val b = 2.7f
        val aBits = a.toRawBits()
        val bBits = b.toRawBits()

        // Warm up
        repeat(1000) {
            @Suppress("UNUSED_VARIABLE") val r1 = a + b
            @Suppress("UNUSED_VARIABLE") val r2 = a * b
            @Suppress("UNUSED_VARIABLE") val r3 = a / b
            @Suppress("UNUSED_VARIABLE") val r4 = Float32Math.addBits(aBits, bBits)
            @Suppress("UNUSED_VARIABLE") val r5 = Float32Math.mulBits(aBits, bBits)
            @Suppress("UNUSED_VARIABLE") val r6 = Float32Math.divBits(aBits, bBits)
        }

        // Native operations
        val nativeAddTime = measureTime { repeat(iterations) { a + b } }
        val nativeMulTime = measureTime { repeat(iterations) { a * b } }
        val nativeDivTime = measureTime { repeat(iterations) { a / b } }

        // Software operations
        val softAddTime = measureTime { repeat(iterations) { Float32Math.addBits(aBits, bBits) } }
        val softMulTime = measureTime { repeat(iterations) { Float32Math.mulBits(aBits, bBits) } }
        val softDivTime = measureTime { repeat(iterations) { Float32Math.divBits(aBits, bBits) } }

        fun toNsPerOp(ms: Double, iters: Int): Double = ms / iters * 1_000_000

        val nativeAddMs = nativeAddTime.inWholeNanoseconds / 1_000_000.0
        val nativeMulMs = nativeMulTime.inWholeNanoseconds / 1_000_000.0
        val nativeDivMs = nativeDivTime.inWholeNanoseconds / 1_000_000.0
        val softAddMs = softAddTime.inWholeNanoseconds / 1_000_000.0
        val softMulMs = softMulTime.inWholeNanoseconds / 1_000_000.0
        val softDivMs = softDivTime.inWholeNanoseconds / 1_000_000.0

        println("Operation   | Native (ns/op) | Software (ns/op) | Slowdown")
        println("-".repeat(60))
        println("ADD         | ${toNsPerOp(nativeAddMs, iterations)}       | ${toNsPerOp(softAddMs, iterations)}         | ${(softAddMs / nativeAddMs * 10).toInt() / 10.0}x")
        println("MUL         | ${toNsPerOp(nativeMulMs, iterations)}       | ${toNsPerOp(softMulMs, iterations)}         | ${(softMulMs / nativeMulMs * 10).toInt() / 10.0}x")
        println("DIV         | ${toNsPerOp(nativeDivMs, iterations)}       | ${toNsPerOp(softDivMs, iterations)}         | ${(softDivMs / nativeDivMs * 10).toInt() / 10.0}x")
        println("=".repeat(70))
    }
}

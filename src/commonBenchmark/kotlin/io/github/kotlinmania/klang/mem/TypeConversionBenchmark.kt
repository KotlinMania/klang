package io.github.kotlinmania.klang.mem

import io.github.kotlinmania.klang.bitwise.BitShiftConfig
import io.github.kotlinmania.klang.bitwise.BitShiftEngine
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/**
 * Benchmarks for the type-conversion audit
 * (`docs/general/type-conversion-audit.md`).
 *
 * Three groups, all sweeping the same 1024-element sample arrays so loop
 * overhead is constant across variants and only the measured conversion
 * pattern varies:
 *
 *   1. **Tier 1 verification** — the pairs that were changed in the audit.
 *      `Long.toShort()` vs `Long.toInt().toShort()`,
 *      `Short.toLong()` vs `Short.toInt().toLong()`, plus the
 *      analogous `Long.toByte()` and `Byte.toLong()` chains for
 *      completeness. Each pair should match within the noise floor or
 *      the simpler form should win — confirming the audit changes
 *      didn't regress, and quantifying the saving where there is one.
 *
 *   2. **Tier 2-A: setShort API shape** — `PackedBuffer.setShort` currently
 *      takes `Short`, so callers do `Int → Short → Int` via the type
 *      system on every 16-bit store. A local helper `setShortViaInt`
 *      (same body but skipping the type round-trip) is benchmarked
 *      against the existing `setShort` to put a number on the proposal.
 *
 *   3. **Tier 2-B: 32-bit shifts via the Long-shaped engine** — the cost
 *      of `engine.leftShift(value.toLong(), n).value.toInt()` versus the
 *      same operation with a plain `(value shl n)`. Quantifies what an
 *      `Int`-overload set on `BitShiftEngine` would save its callers
 *      (`CBF16`, `HexShift`, `BitwiseOps`, `BitPrimitives`, `Float32Math`).
 *
 * Each `@Benchmark` does one unit of work over the sample array, returns
 * the accumulator to defeat dead-code elimination, and otherwise carries
 * identical structure to its sibling so any difference between the
 * measurements is the conversion pattern under test.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
class TypeConversionBenchmark {

    companion object {
        const val N = 1024
        const val BUFFER_BYTES = N * 2 // enough for 1024 16-bit slots
    }

    private val longSamples: LongArray = LongArray(N) { (it.toLong() * 0xDEAD_BEEFL).inv() }
    private val intSamples: IntArray = IntArray(N) { (it * 0xDEADBEEF.toInt()).inv() }
    private val shortSamples: ShortArray = ShortArray(N) { ((it * 7919) and 0xFFFF).toShort() }
    private val byteSamples: ByteArray = ByteArray(N) { ((it * 7) and 0xFF).toByte() }

    private var packed: PackedBuffer = PackedBuffer(BUFFER_BYTES)
    private val engine32 = BitShiftEngine(BitShiftConfig.defaultMode, 32)

    @Setup
    fun setup() {
        packed = PackedBuffer(BUFFER_BYTES)
        // Pre-fill so the benchmarks aren't measuring zero-filled memory.
        for (i in 0 until N) packed.setShort(i * 2, ((i * 7919) and 0xFFFF).toShort())
    }

    // =================================================================
    // Tier 1 verification: the audit's actual changes
    // =================================================================

    /** New shape: direct `Long.toShort()`. */
    @Benchmark
    fun longToShortDirect(): Int {
        var acc = 0
        for (v in longSamples) acc = acc xor v.toShort().toInt()
        return acc
    }

    /** Old shape: `Long.toInt().toShort()` round-trip through Int. */
    @Benchmark
    fun longToShortViaInt(): Int {
        var acc = 0
        for (v in longSamples) acc = acc xor v.toInt().toShort().toInt()
        return acc
    }

    /** Direct `Long.toByte()`. */
    @Benchmark
    fun longToByteDirect(): Int {
        var acc = 0
        for (v in longSamples) acc = acc xor v.toByte().toInt()
        return acc
    }

    /** `Long.toInt().toByte()` round-trip. */
    @Benchmark
    fun longToByteViaInt(): Int {
        var acc = 0
        for (v in longSamples) acc = acc xor v.toInt().toByte().toInt()
        return acc
    }

    /** New shape: direct `Short.toLong()`. */
    @Benchmark
    fun shortToLongDirect(): Long {
        var acc = 0L
        for (v in shortSamples) acc = acc xor v.toLong()
        return acc
    }

    /** Old shape: `Short.toInt().toLong()`. */
    @Benchmark
    fun shortToLongViaInt(): Long {
        var acc = 0L
        for (v in shortSamples) acc = acc xor v.toInt().toLong()
        return acc
    }

    /** Direct `Byte.toLong()`. */
    @Benchmark
    fun byteToLongDirect(): Long {
        var acc = 0L
        for (v in byteSamples) acc = acc xor v.toLong()
        return acc
    }

    /** `Byte.toInt().toLong()` round-trip. */
    @Benchmark
    fun byteToLongViaInt(): Long {
        var acc = 0L
        for (v in byteSamples) acc = acc xor v.toInt().toLong()
        return acc
    }

    // =================================================================
    // Tier 2-A: PackedBuffer.setShort signature comparison
    // =================================================================

    /**
     * Current shape: caller has an Int and narrows to Short before calling
     * `setShort`. `setShort` then widens back internally with sign-extend
     * and mask. Net: `Int → Short → Int` round-trip per store.
     */
    @Benchmark
    fun setShortViaShortParameter(): Int {
        for (i in 0 until N) {
            val intValue = intSamples[i]
            packed.setShort(i * 2, intValue.toShort())
        }
        return packed.getShort(0).toInt()
    }

    /**
     * Proposed shape: caller passes the Int directly; the function masks
     * to 16 bits internally without the Short round-trip. Inlined here
     * so the benchmark exercises the proposed path without modifying the
     * production API.
     */
    @Benchmark
    fun setShortViaIntParameter(): Int {
        for (i in 0 until N) {
            val intValue = intSamples[i]
            setShortInt(i * 2, intValue)
        }
        return packed.getShort(0).toInt()
    }

    /**
     * Mirrors what `PackedBuffer.setShort` does internally, but accepts
     * an `Int` directly so the caller doesn't have to narrow to `Short`
     * (only to be widened-and-masked again on entry). Single mask op.
     */
    private fun setShortInt(addr: Int, value: Int) {
        val idx = addr ushr 3
        val shift = (addr and 7) shl 3
        val v = (value and 0xFFFF).toLong()
        val data = packed.data
        val mask = (0xFFFFL shl shift).inv()
        if (shift <= 48) {
            data[idx] = (data[idx] and mask) or (v shl shift)
        } else {
            // straddle (rare in practice with 2-byte alignment; included for parity)
            val bitsInFirst = 64 - shift
            val maskFirst = (0xFFFFL shl shift).inv()
            data[idx] = (data[idx] and maskFirst) or (v shl shift)
            val maskSecond = (0xFFFFL ushr bitsInFirst).inv()
            data[idx + 1] = (data[idx + 1] and maskSecond) or (v ushr bitsInFirst)
        }
    }

    // =================================================================
    // Tier 2-B: 32-bit shifts through the Long-shaped engine
    // =================================================================

    /**
     * Current call-site shape (used in CBF16, HexShift, BitwiseOps, etc.):
     * widen Int → Long, call the engine, narrow Long → Int. Two boundary
     * conversions per shift.
     */
    @Benchmark
    fun engineShiftViaLongBoundary(): Int {
        var acc = 0
        for (v in intSamples) {
            val shifted = engine32.leftShift(v.toLong(), 7).value.toInt() and 0xFF
            acc = acc xor shifted
        }
        return acc
    }

    /**
     * What the proposed `Int`-overload `engine.leftShift32(Int, Int): Int`
     * would compile to in NATIVE mode: a plain `Int shl n`, no widen/narrow.
     * The engine's NATIVE mode delegates to the same hardware shift
     * instruction either way; the difference measured here is the
     * boundary tax on the existing API shape.
     */
    @Benchmark
    fun directIntShiftNoBoundary(): Int {
        var acc = 0
        for (v in intSamples) {
            val shifted = (v shl 7) and 0xFF
            acc = acc xor shifted
        }
        return acc
    }

    /**
     * Same comparison but for `unsignedRightShift` — the more common
     * shape in the IEEE-754 kernels, where masking after a `ushr` is
     * the dominant pattern.
     */
    @Benchmark
    fun engineUshrViaLongBoundary(): Int {
        var acc = 0
        for (v in intSamples) {
            val shifted = engine32.unsignedRightShift(v.toLong(), 7).value.toInt() and 0x1FF_FFFF
            acc = acc xor shifted
        }
        return acc
    }

    @Benchmark
    fun directIntUshrNoBoundary(): Int {
        var acc = 0
        for (v in intSamples) {
            val shifted = (v ushr 7) and 0x1FF_FFFF
            acc = acc xor shifted
        }
        return acc
    }
}

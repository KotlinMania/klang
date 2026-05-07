package io.github.kotlinmania.klang.mem

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/**
 * Benchmarks comparing `ByteArray` (signed) against `UByteArray` (unsigned)
 * for buffer-style load / store / memset workloads. The masking pattern
 * (`b.toInt() and 0xFF`) signed `ByteArray` requires is a known overhead
 * that disappears on `UByteArray`; these benchmarks quantify it across the
 * platforms klang targets.
 *
 * The previous "sign-extension demo" `@Test` that printed sample values
 * with no assertions was educational rather than measurement, so it does
 * not survive the migration.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class UByteArrayBenchmark {

    companion object {
        const val BUFFER_SIZE = 15360  // matches the 80×24×8 TUI buffer
    }

    // UByteArray is an inline class so it can't be `lateinit`. Initialise
    // both arrays at field declaration; @Setup re-initialises them so each
    // benchmark iteration starts from a fresh deterministic state.
    private var signedArr: ByteArray = ByteArray(BUFFER_SIZE)
    private var unsignedArr: UByteArray = UByteArray(BUFFER_SIZE)

    @Setup
    fun setup() {
        signedArr = ByteArray(BUFFER_SIZE) { (it and 0xFF).toByte() }
        unsignedArr = UByteArray(BUFFER_SIZE) { (it and 0xFF).toUByte() }
    }

    // ===== Load word =====

    @Benchmark
    fun loadWordSweepSigned(): Int {
        var acc = 0
        var addr = 0
        while (addr <= BUFFER_SIZE - 4) {
            acc = acc xor loadWordSigned(signedArr, addr)
            addr += 4
        }
        return acc
    }

    @Benchmark
    fun loadWordSweepUnsigned(): Int {
        var acc = 0
        var addr = 0
        while (addr <= BUFFER_SIZE - 4) {
            acc = acc xor loadWordUnsigned(unsignedArr, addr)
            addr += 4
        }
        return acc
    }

    // ===== Store word =====

    @Benchmark
    fun storeWordSweepSigned(): Int {
        var addr = 0
        val value = 0xDEAD_BEEF.toInt()
        while (addr <= BUFFER_SIZE - 4) {
            storeWordSigned(signedArr, addr, value)
            addr += 4
        }
        return signedArr[0].toInt()
    }

    @Benchmark
    fun storeWordSweepUnsigned(): Int {
        var addr = 0
        val value = 0xDEAD_BEEF.toInt()
        while (addr <= BUFFER_SIZE - 4) {
            storeWordUnsigned(unsignedArr, addr, value)
            addr += 4
        }
        return unsignedArr[0].toInt()
    }

    // ===== Memset =====

    @Benchmark
    fun memsetSigned(): Int {
        signedArr.fill(0x20)
        return signedArr[0].toInt()
    }

    @Benchmark
    fun memsetUnsigned(): Int {
        unsignedArr.fill(0x20u)
        return unsignedArr[0].toInt()
    }

    // ---- helpers ----

    /** Signed `ByteArray` requires explicit `and 0xFF` masking on every read. */
    private fun loadWordSigned(arr: ByteArray, addr: Int): Int =
        (arr[addr].toInt() and 0xFF) or
            ((arr[addr + 1].toInt() and 0xFF) shl 8) or
            ((arr[addr + 2].toInt() and 0xFF) shl 16) or
            ((arr[addr + 3].toInt() and 0xFF) shl 24)

    /** `UByteArray` reads zero-extend by definition; no mask needed. */
    private fun loadWordUnsigned(arr: UByteArray, addr: Int): Int =
        arr[addr].toInt() or
            (arr[addr + 1].toInt() shl 8) or
            (arr[addr + 2].toInt() shl 16) or
            (arr[addr + 3].toInt() shl 24)

    private fun storeWordSigned(arr: ByteArray, addr: Int, value: Int) {
        arr[addr] = value.toByte()
        arr[addr + 1] = (value shr 8).toByte()
        arr[addr + 2] = (value shr 16).toByte()
        arr[addr + 3] = (value shr 24).toByte()
    }

    private fun storeWordUnsigned(arr: UByteArray, addr: Int, value: Int) {
        arr[addr] = value.toUByte()
        arr[addr + 1] = (value shr 8).toUByte()
        arr[addr + 2] = (value shr 16).toUByte()
        arr[addr + 3] = (value shr 24).toUByte()
    }
}

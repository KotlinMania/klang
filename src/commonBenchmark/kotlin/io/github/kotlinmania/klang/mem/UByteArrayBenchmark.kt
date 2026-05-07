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
 * Benchmarks comparing three storage shapes for buffer-style load / store /
 * memset workloads:
 *
 *   1. `ByteArray` + explicit `and 0xFF` masking — naive port of C's
 *      `unsigned char[]` to a signed Kotlin `ByteArray`. Forced because
 *      `Byte.toInt()` sign-extends.
 *   2. `UByteArray` — stdlib's value-class wrapper around `ByteArray`.
 *      Looks "free" on paper (no explicit mask) but `UByteArray.get(i)`
 *      is *not* `inline` and `UByte.toInt()` is literally
 *      `data.toInt() and 0xFF` under the hood — same work plus a
 *      function-call boundary on every access.
 *   3. `LongArray` + word-aligned shift — the storage shape klang's
 *      [PackedBuffer] / [GlobalHeap] actually use. `Long.ushr` returns
 *      `Long` (no type promotion, no sign-extension on the operand),
 *      and a single `Long` carries 8 bytes' worth of data, so a
 *      4-byte little-endian word load is a couple of shifts and a mask
 *      against an existing `Long`. No `Byte` ever appears.
 *
 * Storage is the only thing varying. The question the benchmark answers
 * is: which shape produces the tightest machine code on Kotlin/Native
 * for "load 4 LE bytes as an Int, swept across a 15 KB buffer"?
 *
 * Klang's design choice is (3); these numbers are the empirical backing.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class UByteArrayBenchmark {

    companion object {
        const val BUFFER_SIZE = 15360                    // 80 × 24 × 8 cells
        const val LONG_COUNT  = (BUFFER_SIZE + 7) ushr 3 // 8 bytes per Long slot
    }

    // UByteArray is an inline class so it can't be `lateinit`. Initialise
    // each backing array at field declaration; @Setup re-initialises so
    // every benchmark iteration starts from a fresh deterministic state.
    private var signedArr: ByteArray   = ByteArray(BUFFER_SIZE)
    private var unsignedArr: UByteArray = UByteArray(BUFFER_SIZE)
    private var longArr: LongArray     = LongArray(LONG_COUNT)

    @Setup
    fun setup() {
        // Same logical bytes (i and 0xFF) at every address across all three
        // storage shapes so the loads are comparing identical work.
        signedArr   = ByteArray(BUFFER_SIZE)   { (it and 0xFF).toByte() }
        unsignedArr = UByteArray(BUFFER_SIZE)  { (it and 0xFF).toUByte() }
        longArr     = LongArray(LONG_COUNT) { lIdx ->
            // Pack 8 consecutive bytes (b0..b7) as little-endian Long.
            var packed = 0L
            for (i in 0..7) {
                val byteAddr = (lIdx shl 3) + i
                if (byteAddr < BUFFER_SIZE) {
                    packed = packed or ((byteAddr and 0xFF).toLong() shl (i shl 3))
                }
            }
            packed
        }
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

    @Benchmark
    fun loadWordSweepLongArray(): Int {
        var acc = 0
        var addr = 0
        while (addr <= BUFFER_SIZE - 4) {
            acc = acc xor loadWordLongArray(longArr, addr)
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

    /**
     * Word-aligned read directly from a `LongArray`. No `Byte` is ever
     * involved — `Long ushr n` returns `Long`, and we mask the four bytes
     * of interest out of one or two `Long` slots and reassemble. Same
     * semantics, no sign-extension exposure, no value-class boundary.
     *
     * The address may straddle two `Long` slots, so the helper handles
     * both the aligned-within-one-slot and split-across-two-slots cases.
     */
    private fun loadWordLongArray(arr: LongArray, addr: Int): Int {
        val idx = addr ushr 3
        val byteOffsetInLong = (addr and 7) shl 3
        val bytesInFirstSlot = 8 - (addr and 7)
        return if (bytesInFirstSlot >= 4) {
            // All four bytes live in `arr[idx]`.
            ((arr[idx] ushr byteOffsetInLong) and 0xFFFF_FFFFL).toInt()
        } else {
            // Word straddles the boundary: low bytes from arr[idx], high from arr[idx+1].
            val low  = (arr[idx]     ushr byteOffsetInLong) and ((1L shl (bytesInFirstSlot shl 3)) - 1L)
            val high = arr[idx + 1] and ((1L shl ((4 - bytesInFirstSlot) shl 3)) - 1L)
            ((low or (high shl (bytesInFirstSlot shl 3))) and 0xFFFF_FFFFL).toInt()
        }
    }

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

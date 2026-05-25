package io.github.kotlinmania.klang.int

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
 * Throughput benchmarks for the heap-backed fixed-width integer types
 * (C_Int8/16/32/64 + C_UInt8/16/32/64) against their native-primitive
 * counterparts.
 *
 * Each `@Benchmark` returns a value so the JIT can't dead-code-eliminate the
 * work. The kotlinx-benchmark runner aggregates throughput in ops/ns; the
 * ratio between paired Klang and native benchmarks shows the BitShiftEngine
 * + heap-load overhead per op.
 *
 * Coverage per width:
 *   - addition (wrap on overflow)
 *   - bitwise AND
 *   - left shift by 1
 *   - right shift (arithmetic for signed, logical for unsigned)
 *
 * For arithmetic right shift the signed-vs-unsigned cost difference shows
 * up because the signed path also has to build a sign-fill mask via the
 * engine.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
class FixedWidthIntBenchmark {

    // -- C_UInt8 -------------------------------------------------------------
    private lateinit var cU8a: C_UInt8
    private lateinit var cU8b: C_UInt8
    private val nativeU8a: UByte = 0xC3u
    private val nativeU8b: UByte = 0x42u

    // -- C_Int8 --------------------------------------------------------------
    private lateinit var cI8a: C_Int8
    private lateinit var cI8b: C_Int8
    private val nativeI8a: Byte = -42
    private val nativeI8b: Byte = 17

    // -- C_UInt16 ------------------------------------------------------------
    private lateinit var cU16a: C_UInt16
    private lateinit var cU16b: C_UInt16
    private val nativeU16a: UShort = 0xCAFEu
    private val nativeU16b: UShort = 0x1234u

    // -- C_Int16 -------------------------------------------------------------
    private lateinit var cI16a: C_Int16
    private lateinit var cI16b: C_Int16
    private val nativeI16a: Short = -12345
    private val nativeI16b: Short = 6789

    // -- C_UInt32 ------------------------------------------------------------
    private lateinit var cU32a: C_UInt32
    private lateinit var cU32b: C_UInt32
    private val nativeU32a: UInt = 0xCAFE_BABEu
    private val nativeU32b: UInt = 0x1234_5678u

    // -- C_Int32 -------------------------------------------------------------
    private lateinit var cI32a: C_Int32
    private lateinit var cI32b: C_Int32
    private val nativeI32a: Int = -1_234_567
    private val nativeI32b: Int = 987_654

    // -- C_UInt64 ------------------------------------------------------------
    private lateinit var cU64a: C_UInt64
    private lateinit var cU64b: C_UInt64
    private val nativeU64a: ULong = 0xCAFE_BABE_DEAD_BEEFuL
    private val nativeU64b: ULong = 0x1234_5678_9ABC_DEF0uL

    // -- C_Int64 -------------------------------------------------------------
    private lateinit var cI64a: C_Int64
    private lateinit var cI64b: C_Int64
    private val nativeI64a: Long = -1_234_567_890_123L
    private val nativeI64b: Long = 987_654_321_098L

    @Setup
    fun setup() {
        KMalloc.init(1 shl 16) // 64 KB (also initializes GlobalHeap)

        cU8a = C_UInt8.fromUByte(nativeU8a)
        cU8b = C_UInt8.fromUByte(nativeU8b)
        cI8a = C_Int8.fromByte(nativeI8a)
        cI8b = C_Int8.fromByte(nativeI8b)

        cU16a = C_UInt16.fromUShort(nativeU16a)
        cU16b = C_UInt16.fromUShort(nativeU16b)
        cI16a = C_Int16.fromShort(nativeI16a)
        cI16b = C_Int16.fromShort(nativeI16b)

        cU32a = C_UInt32.fromUInt(nativeU32a)
        cU32b = C_UInt32.fromUInt(nativeU32b)
        cI32a = C_Int32.fromInt(nativeI32a)
        cI32b = C_Int32.fromInt(nativeI32b)

        cU64a = C_UInt64.fromULong(nativeU64a)
        cU64b = C_UInt64.fromULong(nativeU64b)
        cI64a = C_Int64.fromLong(nativeI64a)
        cI64b = C_Int64.fromLong(nativeI64b)
    }

    // ====================================================================
    // 8-bit
    // ====================================================================
    @Benchmark fun nativeU8Add(): UByte = (nativeU8a + nativeU8b).toUByte()
    @Benchmark fun cU8Add(): C_UInt8 = cU8a + cU8b
    @Benchmark fun nativeU8And(): UByte = nativeU8a and nativeU8b
    @Benchmark fun cU8And(): C_UInt8 = cU8a and cU8b
    @Benchmark fun nativeU8ShiftLeft(): UInt = nativeU8a.toUInt() shl 1
    @Benchmark fun cU8ShiftLeft(): C_UInt8 = cU8a.shiftLeft(1)
    @Benchmark fun nativeU8ShiftRight(): UInt = nativeU8a.toUInt() shr 1
    @Benchmark fun cU8ShiftRight(): C_UInt8 = cU8a.shiftRight(1)

    @Benchmark fun nativeI8Add(): Byte = (nativeI8a + nativeI8b).toByte()
    @Benchmark fun cI8Add(): C_Int8 = cI8a + cI8b
    @Benchmark fun nativeI8ShiftRightArithmetic(): Int = nativeI8a.toInt() shr 1
    @Benchmark fun cI8ShiftRightArithmetic(): C_Int8 = cI8a.shiftRight(1)

    // ====================================================================
    // 16-bit
    // ====================================================================
    @Benchmark fun nativeU16Add(): UShort = (nativeU16a + nativeU16b).toUShort()
    @Benchmark fun cU16Add(): C_UInt16 = cU16a + cU16b
    @Benchmark fun nativeU16And(): UShort = nativeU16a and nativeU16b
    @Benchmark fun cU16And(): C_UInt16 = cU16a and cU16b
    @Benchmark fun nativeU16ShiftLeft(): UInt = nativeU16a.toUInt() shl 1
    @Benchmark fun cU16ShiftLeft(): C_UInt16 = cU16a.shiftLeft(1)

    @Benchmark fun nativeI16Add(): Short = (nativeI16a + nativeI16b).toShort()
    @Benchmark fun cI16Add(): C_Int16 = cI16a + cI16b
    @Benchmark fun nativeI16ShiftRightArithmetic(): Int = nativeI16a.toInt() shr 1
    @Benchmark fun cI16ShiftRightArithmetic(): C_Int16 = cI16a.shiftRight(1)

    // ====================================================================
    // 32-bit
    // ====================================================================
    @Benchmark fun nativeU32Add(): UInt = nativeU32a + nativeU32b
    @Benchmark fun cU32Add(): C_UInt32 = cU32a + cU32b
    @Benchmark fun nativeU32And(): UInt = nativeU32a and nativeU32b
    @Benchmark fun cU32And(): C_UInt32 = cU32a and cU32b
    @Benchmark fun nativeU32ShiftLeft(): UInt = nativeU32a shl 1
    @Benchmark fun cU32ShiftLeft(): C_UInt32 = cU32a.shiftLeft(1)
    @Benchmark fun nativeU32ShiftRight(): UInt = nativeU32a shr 1
    @Benchmark fun cU32ShiftRight(): C_UInt32 = cU32a.shiftRight(1)

    @Benchmark fun nativeI32Add(): Int = nativeI32a + nativeI32b
    @Benchmark fun cI32Add(): C_Int32 = cI32a + cI32b
    @Benchmark fun nativeI32ShiftRightArithmetic(): Int = nativeI32a shr 1
    @Benchmark fun cI32ShiftRightArithmetic(): C_Int32 = cI32a.shiftRight(1)
    @Benchmark fun nativeI32Mul(): Int = nativeI32a * nativeI32b
    @Benchmark fun cI32Mul(): C_Int32 = cI32a * cI32b

    // ====================================================================
    // 64-bit
    // ====================================================================
    @Benchmark fun nativeU64Add(): ULong = nativeU64a + nativeU64b
    @Benchmark fun cU64Add(): C_UInt64 = cU64a + cU64b
    @Benchmark fun nativeU64And(): ULong = nativeU64a and nativeU64b
    @Benchmark fun cU64And(): C_UInt64 = cU64a and cU64b
    @Benchmark fun nativeU64ShiftLeft(): ULong = nativeU64a shl 1
    @Benchmark fun cU64ShiftLeft(): C_UInt64 = cU64a.shiftLeft(1)

    @Benchmark fun nativeI64Add(): Long = nativeI64a + nativeI64b
    @Benchmark fun cI64Add(): C_Int64 = cI64a + cI64b
    @Benchmark fun nativeI64ShiftRightArithmetic(): Long = nativeI64a shr 1
    @Benchmark fun cI64ShiftRightArithmetic(): C_Int64 = cI64a.shiftRight(1)
    @Benchmark fun nativeI64Mul(): Long = nativeI64a * nativeI64b
    @Benchmark fun cI64Mul(): C_Int64 = cI64a * cI64b
}

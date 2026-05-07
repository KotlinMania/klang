package io.github.kotlinmania.klang.mem

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/**
 * Benchmarks comparing klang's heap-backed buffer model against an
 * equivalent Kotlin-object buffer for TUI (terminal UI) workloads.
 *
 * Buffer shape: 80 × 24 grid of 8-byte cells (codepoint:4, fg:1, bg:1, modifier:2).
 *
 * Each `@Benchmark` method does one unit of work (fill, write text, diff,
 * clear, equality probe). The runner takes care of iteration, warmup, and
 * ops/sec aggregation — the previous in-method `repeat(...)` loops with
 * `measureTime` and `println` blocks are gone.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(kotlinx.benchmark.BenchmarkTimeUnit.MICROSECONDS)
class TuiBufferBenchmark {

    companion object {
        const val CELL_SIZE = 8
        const val CODEPOINT_OFFSET = 0
        const val FG_OFFSET = 4
        const val BG_OFFSET = 5
        const val MODIFIER_OFFSET = 6

        const val WIDTH = 80
        const val HEIGHT = 24
        const val TOTAL_CELLS = WIDTH * HEIGHT       // 1920
        const val BUFFER_BYTES = TOTAL_CELLS * CELL_SIZE  // 15360
    }

    /** Object-based cell, equivalent to the current ratatui-kotlin model. */
    data class ObjectCell(
        var symbol: String = " ",
        var fg: Int = 0,
        var bg: Int = 0,
        var modifier: Int = 0,
    )

    // ---- Heap-backed state ----
    private var heapAddr1: Int = 0
    private var heapAddr2: Int = 0

    // ---- Object-backed state ----
    private lateinit var objectCells1: MutableList<ObjectCell>
    private lateinit var objectCells2: MutableList<ObjectCell>

    @Setup
    fun setup() {
        GlobalHeap.init(1024 * 1024)
        GlobalHeap.reset()
        heapAddr1 = GlobalHeap.malloc(BUFFER_BYTES)
        heapAddr2 = GlobalHeap.malloc(BUFFER_BYTES)
        // Initial fill so equality benchmarks compare equal buffers.
        GlobalHeap.memset(heapAddr1, 0x20, BUFFER_BYTES)
        GlobalHeap.memset(heapAddr2, 0x20, BUFFER_BYTES)

        objectCells1 = MutableList(TOTAL_CELLS) { ObjectCell(symbol = " ") }
        objectCells2 = MutableList(TOTAL_CELLS) { ObjectCell(symbol = " ") }
    }

    // ===== Fill-and-write benchmarks (per-cell writes) =====

    @Benchmark
    fun heapFillAndDiff(): Long {
        // Fill buffer
        for (i in 0 until TOTAL_CELLS) {
            val cellAddr = heapAddr1 + i * CELL_SIZE
            GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, 0x20)
            GlobalHeap.sb(cellAddr + FG_OFFSET, 7)
            GlobalHeap.sb(cellAddr + BG_OFFSET, 0)
            GlobalHeap.sh(cellAddr + MODIFIER_OFFSET, 0)
        }
        // "Hello World" at row 5
        val text = "Hello World"
        for ((col, char) in text.withIndex()) {
            val cellAddr = heapAddr1 + (5 * WIDTH + col) * CELL_SIZE
            GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, char.code)
            GlobalHeap.sb(cellAddr + FG_OFFSET, 2)
        }
        // Read back (simulates a diff pass).
        var checksum = 0L
        for (i in 0 until TOTAL_CELLS) {
            checksum += GlobalHeap.lw(heapAddr1 + i * CELL_SIZE + CODEPOINT_OFFSET)
        }
        return checksum
    }

    @Benchmark
    fun objectFillAndDiff(): Long {
        for (cell in objectCells1) {
            cell.symbol = " "
            cell.fg = 7
            cell.bg = 0
            cell.modifier = 0
        }
        val text = "Hello World"
        for ((col, char) in text.withIndex()) {
            val idx = 5 * WIDTH + col
            objectCells1[idx].symbol = char.toString()
            objectCells1[idx].fg = 2
        }
        var checksum = 0L
        for (cell in objectCells1) {
            checksum += cell.symbol.firstOrNull()?.code ?: 0
        }
        return checksum
    }

    // ===== Bulk-clear benchmarks =====

    @Benchmark
    fun heapBulkClearMemset(): Int {
        GlobalHeap.memset(heapAddr1, 0x20, BUFFER_BYTES)
        return GlobalHeap.lw(heapAddr1)  // pin against DCE
    }

    @Benchmark
    fun objectBulkClear(): Int {
        for (cell in objectCells1) {
            cell.symbol = " "
            cell.fg = 0
            cell.bg = 0
            cell.modifier = 0
        }
        return objectCells1[0].fg
    }

    // ===== Equality / diff benchmarks =====

    @Benchmark
    fun heapBufferEqualityWordwise(): Boolean {
        var equal = true
        var i = 0
        while (i < BUFFER_BYTES) {
            if (GlobalHeap.lw(heapAddr1 + i) != GlobalHeap.lw(heapAddr2 + i)) {
                equal = false
                break
            }
            i += 4
        }
        return equal
    }

    @Benchmark
    fun objectBufferEqualityListEquals(): Boolean = objectCells1 == objectCells2

    // ===== Direct-byte vs typed-store comparison =====

    @Benchmark
    fun heapWriteCellsDirectBytes(): Int {
        for (i in 0 until TOTAL_CELLS) {
            val cellAddr = heapAddr1 + i * CELL_SIZE
            GlobalHeap.sb(cellAddr,     0x20.toByte())
            GlobalHeap.sb(cellAddr + 1, 0.toByte())
            GlobalHeap.sb(cellAddr + 2, 0.toByte())
            GlobalHeap.sb(cellAddr + 3, 0.toByte())
            GlobalHeap.sb(cellAddr + 4, 7.toByte())
            GlobalHeap.sb(cellAddr + 5, 0.toByte())
            GlobalHeap.sb(cellAddr + 6, 0.toByte())
            GlobalHeap.sb(cellAddr + 7, 0.toByte())
        }
        return GlobalHeap.lw(heapAddr1)
    }

    @Benchmark
    fun heapWriteCellsTypedStores(): Int {
        for (i in 0 until TOTAL_CELLS) {
            val cellAddr = heapAddr1 + i * CELL_SIZE
            GlobalHeap.sw(cellAddr, 0x20)
            GlobalHeap.sb(cellAddr + 4, 7.toByte())
            GlobalHeap.sb(cellAddr + 5, 0.toByte())
            GlobalHeap.sh(cellAddr + 6, 0)
        }
        return GlobalHeap.lw(heapAddr1)
    }

    // ===== Full render-cycle benchmark =====

    @Benchmark
    fun heapFullRenderCycle(): Long {
        GlobalHeap.memset(heapAddr1, 0, BUFFER_BYTES)
        var checksum = 0L
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val cellAddr = heapAddr1 + (y * WIDTH + x) * CELL_SIZE
                GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, 0x20 + (x % 26))
                GlobalHeap.sb(cellAddr + FG_OFFSET, ((x + y) % 8).toByte())
                checksum += GlobalHeap.lw(cellAddr + CODEPOINT_OFFSET)
            }
        }
        return checksum
    }

    @Benchmark
    fun objectFullRenderCycle(): Long {
        for (cell in objectCells1) {
            cell.symbol = " "
            cell.fg = 0
            cell.bg = 0
            cell.modifier = 0
        }
        var checksum = 0L
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val idx = y * WIDTH + x
                objectCells1[idx].symbol = (0x20 + (x % 26)).toChar().toString()
                objectCells1[idx].fg = (x + y) % 8
                checksum += objectCells1[idx].symbol.firstOrNull()?.code ?: 0
            }
        }
        return checksum
    }
}

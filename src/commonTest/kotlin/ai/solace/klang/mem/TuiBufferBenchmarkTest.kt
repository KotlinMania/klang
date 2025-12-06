package ai.solace.klang.mem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime
import kotlin.time.Duration

/**
 * Benchmark comparing heap-backed buffer vs object-based buffer
 * for TUI (Terminal User Interface) rendering workloads.
 *
 * Simulates a typical TUI buffer: 80x24 grid of cells where each cell has:
 * - symbol (4 bytes - unicode codepoint)
 * - fg color (1 byte)
 * - bg color (1 byte)
 * - modifier (2 bytes)
 * Total: 8 bytes per cell
 */
class TuiBufferBenchmarkTest {

    // Cell layout in heap: [codepoint:4][fg:1][bg:1][modifier:2] = 8 bytes
    companion object {
        const val CELL_SIZE = 8
        const val CODEPOINT_OFFSET = 0
        const val FG_OFFSET = 4
        const val BG_OFFSET = 5
        const val MODIFIER_OFFSET = 6

        const val WIDTH = 80
        const val HEIGHT = 24
        const val TOTAL_CELLS = WIDTH * HEIGHT  // 1920 cells
        const val BUFFER_BYTES = TOTAL_CELLS * CELL_SIZE  // 15360 bytes
    }

    // Object-based cell (like current ratatui-kotlin)
    data class ObjectCell(
        var symbol: String = " ",
        var fg: Int = 0,
        var bg: Int = 0,
        var modifier: Int = 0
    )

    @Test
    fun benchmarkHeapBufferOperations() {
        GlobalHeap.init(1024 * 1024)  // 1MB heap

        val warmupIterations = 100
        val benchmarkIterations = 1000

        // Warm up
        repeat(warmupIterations) {
            val addr = GlobalHeap.malloc(BUFFER_BYTES)
            // Fill buffer with space character (0x20)
            for (i in 0 until TOTAL_CELLS) {
                val cellAddr = addr + i * CELL_SIZE
                GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, 0x20)  // space
                GlobalHeap.sb(cellAddr + FG_OFFSET, 7)            // white
                GlobalHeap.sb(cellAddr + BG_OFFSET, 0)            // black
                GlobalHeap.sh(cellAddr + MODIFIER_OFFSET, 0)      // no modifier
            }
            GlobalHeap.reset()
        }

        // Benchmark heap-backed buffer
        GlobalHeap.reset()
        val heapTime = measureTime {
            repeat(benchmarkIterations) {
                GlobalHeap.reset()
                val addr = GlobalHeap.malloc(BUFFER_BYTES)

                // Fill buffer
                for (i in 0 until TOTAL_CELLS) {
                    val cellAddr = addr + i * CELL_SIZE
                    GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, 0x20)
                    GlobalHeap.sb(cellAddr + FG_OFFSET, 7)
                    GlobalHeap.sb(cellAddr + BG_OFFSET, 0)
                    GlobalHeap.sh(cellAddr + MODIFIER_OFFSET, 0)
                }

                // Simulate writing text "Hello World" at row 5
                val text = "Hello World"
                for ((col, char) in text.withIndex()) {
                    val cellIndex = 5 * WIDTH + col
                    val cellAddr = addr + cellIndex * CELL_SIZE
                    GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, char.code)
                    GlobalHeap.sb(cellAddr + FG_OFFSET, 2)  // green
                }

                // Read back the buffer (simulate diff)
                var checksum = 0L
                for (i in 0 until TOTAL_CELLS) {
                    val cellAddr = addr + i * CELL_SIZE
                    checksum += GlobalHeap.lw(cellAddr + CODEPOINT_OFFSET)
                }
            }
        }

        val heapTimeMs = heapTime.inWholeMilliseconds.toDouble()
        println("Heap-backed buffer: ${benchmarkIterations} iterations in ${heapTimeMs}ms")
        println("  Per iteration: ${heapTimeMs / benchmarkIterations}ms")
        println("  Per cell write: ${heapTimeMs / benchmarkIterations / TOTAL_CELLS * 1000}us")
    }

    @Test
    fun benchmarkObjectBufferOperations() {
        val warmupIterations = 100
        val benchmarkIterations = 1000

        // Warm up
        repeat(warmupIterations) {
            val cells = MutableList(TOTAL_CELLS) { ObjectCell() }
            for (cell in cells) {
                cell.symbol = " "
                cell.fg = 7
                cell.bg = 0
                cell.modifier = 0
            }
        }

        // Benchmark object-based buffer
        val objectTime = measureTime {
            repeat(benchmarkIterations) {
                val cells = MutableList(TOTAL_CELLS) { ObjectCell() }

                // Fill buffer
                for (cell in cells) {
                    cell.symbol = " "
                    cell.fg = 7
                    cell.bg = 0
                    cell.modifier = 0
                }

                // Simulate writing text "Hello World" at row 5
                val text = "Hello World"
                for ((col, char) in text.withIndex()) {
                    val cellIndex = 5 * WIDTH + col
                    cells[cellIndex].symbol = char.toString()
                    cells[cellIndex].fg = 2  // green
                }

                // Read back the buffer (simulate diff)
                var checksum = 0L
                for (cell in cells) {
                    checksum += cell.symbol.firstOrNull()?.code ?: 0
                }
            }
        }

        val objectTimeMs = objectTime.inWholeMilliseconds.toDouble()
        println("Object-based buffer: ${benchmarkIterations} iterations in ${objectTimeMs}ms")
        println("  Per iteration: ${objectTimeMs / benchmarkIterations}ms")
        println("  Per cell write: ${objectTimeMs / benchmarkIterations / TOTAL_CELLS * 1000}us")
    }

    @Test
    fun benchmarkBulkClearWithFastMem() {
        GlobalHeap.init(1024 * 1024)

        val iterations = 10000

        // Heap bulk clear using memset
        GlobalHeap.reset()
        val addr = GlobalHeap.malloc(BUFFER_BYTES)

        val memsetTime = measureTime {
            repeat(iterations) {
                GlobalHeap.memset(addr, 0x20, BUFFER_BYTES)  // Fill with spaces
            }
        }
        val memsetTimeMs = memsetTime.inWholeMilliseconds.toDouble()

        println("FastMem.memset clear: ${iterations} iterations in ${memsetTimeMs}ms")
        println("  Per clear: ${memsetTimeMs / iterations * 1000}us")
        println("  Throughput: ${(iterations.toDouble() * BUFFER_BYTES) / memsetTimeMs / 1000}MB/s")

        // Object-based clear
        val cells = MutableList(TOTAL_CELLS) { ObjectCell() }

        val objectClearTime = measureTime {
            repeat(iterations) {
                for (cell in cells) {
                    cell.symbol = " "
                    cell.fg = 0
                    cell.bg = 0
                    cell.modifier = 0
                }
            }
        }
        val objectClearTimeMs = objectClearTime.inWholeMilliseconds.toDouble()

        println("Object-based clear: ${iterations} iterations in ${objectClearTimeMs}ms")
        println("  Per clear: ${objectClearTimeMs / iterations * 1000}us")

        val speedup = if (memsetTimeMs > 0) objectClearTimeMs / memsetTimeMs else 0.0
        println("\nSpeedup: ${speedup}x")
    }

    @Test
    fun benchmarkBufferEquality() {
        GlobalHeap.init(1024 * 1024)

        val iterations = 10000

        // Heap-based equality using memcmp-style comparison
        GlobalHeap.reset()
        val addr1 = GlobalHeap.malloc(BUFFER_BYTES)
        val addr2 = GlobalHeap.malloc(BUFFER_BYTES)

        // Fill both with same content
        GlobalHeap.memset(addr1, 0x20, BUFFER_BYTES)
        GlobalHeap.memset(addr2, 0x20, BUFFER_BYTES)

        var heapEqual = true
        val heapEqTime = measureTime {
            repeat(iterations) {
                heapEqual = true
                // Compare word by word (4 bytes at a time)
                for (i in 0 until BUFFER_BYTES / 4) {
                    if (GlobalHeap.lw(addr1 + i * 4) != GlobalHeap.lw(addr2 + i * 4)) {
                        heapEqual = false
                        break
                    }
                }
            }
        }
        val heapEqTimeMs = heapEqTime.inWholeMilliseconds.toDouble()

        println("Heap buffer equality: ${iterations} iterations in ${heapEqTimeMs}ms")
        println("  Per comparison: ${heapEqTimeMs / iterations * 1000}us")

        // Object-based equality
        val cells1 = MutableList(TOTAL_CELLS) { ObjectCell(symbol = " ") }
        val cells2 = MutableList(TOTAL_CELLS) { ObjectCell(symbol = " ") }

        var objEqual = true
        val objEqTime = measureTime {
            repeat(iterations) {
                objEqual = cells1 == cells2
            }
        }
        val objEqTimeMs = objEqTime.inWholeMilliseconds.toDouble()

        println("Object buffer equality: ${iterations} iterations in ${objEqTimeMs}ms")
        println("  Per comparison: ${objEqTimeMs / iterations * 1000}us")

        val speedup = if (heapEqTimeMs > 0) objEqTimeMs / heapEqTimeMs else 0.0
        println("\nSpeedup: ${speedup}x")

        assertEquals(heapEqual, objEqual, "Both should be equal")
    }

    @Test
    fun benchmarkDirectByteAccess() {
        println("\n" + "=".repeat(70))
        println("DIRECT BYTE ACCESS vs TYPED OPERATIONS")
        println("=".repeat(70))

        // Check what mode BitShiftConfig resolved to
        println("BitShiftConfig.defaultMode = ${ai.solace.klang.bitwise.BitShiftConfig.defaultMode}")
        println("Resolved mode for 8-bit: ${ai.solace.klang.bitwise.BitShiftConfig.resolveMode(8)}")
        println("Resolved mode for 16-bit: ${ai.solace.klang.bitwise.BitShiftConfig.resolveMode(16)}")
        println("Resolved mode for 32-bit: ${ai.solace.klang.bitwise.BitShiftConfig.resolveMode(32)}")

        // Force NATIVE mode for fair comparison
        ai.solace.klang.bitwise.BitShiftConfig.defaultMode = ai.solace.klang.bitwise.BitShiftMode.NATIVE
        println("FORCED to NATIVE mode for benchmark")

        GlobalHeap.init(1024 * 1024)
        val iterations = 1000

        // Using lb/sb (direct ByteArray access)
        GlobalHeap.reset()
        val addr = GlobalHeap.malloc(BUFFER_BYTES)

        val directTime = measureTime {
            repeat(iterations) {
                // Direct byte write for each cell (just symbol as single byte for simplicity)
                for (i in 0 until TOTAL_CELLS) {
                    val cellAddr = addr + i * CELL_SIZE
                    GlobalHeap.sb(cellAddr, 0x20.toByte())     // symbol low byte
                    GlobalHeap.sb(cellAddr + 1, 0.toByte())    // symbol high bytes
                    GlobalHeap.sb(cellAddr + 2, 0.toByte())
                    GlobalHeap.sb(cellAddr + 3, 0.toByte())
                    GlobalHeap.sb(cellAddr + 4, 7.toByte())    // fg
                    GlobalHeap.sb(cellAddr + 5, 0.toByte())    // bg
                    GlobalHeap.sb(cellAddr + 6, 0.toByte())    // modifier low
                    GlobalHeap.sb(cellAddr + 7, 0.toByte())    // modifier high
                }
            }
        }

        // Using lw/sw (through BitShiftEngine)
        val typedTime = measureTime {
            repeat(iterations) {
                for (i in 0 until TOTAL_CELLS) {
                    val cellAddr = addr + i * CELL_SIZE
                    GlobalHeap.sw(cellAddr, 0x20)              // symbol (4 bytes through shifter)
                    GlobalHeap.sb(cellAddr + 4, 7.toByte())    // fg
                    GlobalHeap.sb(cellAddr + 5, 0.toByte())    // bg
                    GlobalHeap.sh(cellAddr + 6, 0)             // modifier (2 bytes through shifter)
                }
            }
        }

        val directMs = directTime.inWholeMilliseconds.toDouble()
        val typedMs = typedTime.inWholeMilliseconds.toDouble()

        println("Direct sb() calls:   ${directMs}ms (${directMs/iterations}ms/iter)")
        println("Typed sw()/sh():     ${typedMs}ms (${typedMs/iterations}ms/iter)")
        println("Direct is ${typedMs / directMs}x faster")
        println("=".repeat(70))
    }

    @Test
    fun benchmarkComparison() {
        println("\n" + "=".repeat(70))
        println("TUI BUFFER BENCHMARK: Heap-backed vs Object-based")
        println("=".repeat(70))
        println("Buffer size: ${WIDTH}x${HEIGHT} = ${TOTAL_CELLS} cells (${BUFFER_BYTES} bytes)")
        println()

        GlobalHeap.init(1024 * 1024)

        val iterations = 1000

        // Heap-backed full render cycle
        GlobalHeap.reset()
        val heapTime = measureTime {
            repeat(iterations) {
                GlobalHeap.reset()
                val addr = GlobalHeap.malloc(BUFFER_BYTES)

                // Clear with memset (fast!)
                GlobalHeap.memset(addr, 0, BUFFER_BYTES)

                // Write some content
                for (y in 0 until HEIGHT) {
                    for (x in 0 until WIDTH) {
                        val cellAddr = addr + (y * WIDTH + x) * CELL_SIZE
                        GlobalHeap.sw(cellAddr + CODEPOINT_OFFSET, 0x20 + (x % 26))
                        GlobalHeap.sb(cellAddr + FG_OFFSET, ((x + y) % 8).toByte())
                    }
                }
            }
        }

        // Object-based full render cycle
        val objectTime = measureTime {
            repeat(iterations) {
                val cells = MutableList(TOTAL_CELLS) { ObjectCell() }

                // Clear
                for (cell in cells) {
                    cell.symbol = " "
                    cell.fg = 0
                    cell.bg = 0
                    cell.modifier = 0
                }

                // Write some content
                for (y in 0 until HEIGHT) {
                    for (x in 0 until WIDTH) {
                        val idx = y * WIDTH + x
                        cells[idx].symbol = (0x20 + (x % 26)).toChar().toString()
                        cells[idx].fg = (x + y) % 8
                    }
                }
            }
        }

        val heapMs = heapTime.inWholeMilliseconds.toDouble()
        val objectMs = objectTime.inWholeMilliseconds.toDouble()
        val speedup = if (heapMs > 0) objectMs / heapMs else 0.0

        println("Full render cycle (${iterations} iterations):")
        println("  Heap-backed:   ${heapMs}ms (${heapMs/iterations}ms/iter)")
        println("  Object-based:  ${objectMs}ms (${objectMs/iterations}ms/iter)")
        println("  Speedup:       ${((speedup * 100).toInt() / 100.0)}x")
        println()
        println("=".repeat(70))
    }
}

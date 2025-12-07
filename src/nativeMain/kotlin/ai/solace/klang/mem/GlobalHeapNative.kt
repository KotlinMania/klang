@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ai.solace.klang.mem

import kotlinx.cinterop.*
import platform.posix.memcpy as posixMemcpy
import platform.posix.memmove as posixMemmove
import platform.posix.memset as posixMemset

/**
 * Native implementation of GlobalHeap using pinned memory with typed pointer access.
 *
 * On native platforms (macOS, Linux, Windows), this uses kotlinx.cinterop to:
 * - Pin the ByteArray in memory to get a stable native pointer
 * - Use typed pointer reinterpretation for direct load/store (single CPU instruction)
 * - Leverage platform memcpy/memmove/memset for bulk operations
 *
 * This is dramatically faster than byte-by-byte assembly because:
 * - lw() becomes a single LDR instruction on ARM64 (vs 4 loads, 4 masks, 3 shifts, 3 ORs)
 * - memset/memcpy use SIMD-optimized platform implementations
 */
actual object GlobalHeap {
    private var mem: ByteArray = ByteArray(0)
    private var hp: Int = 0

    actual val size: Int get() = mem.size
    actual val used: Int get() = hp

    actual fun init(bytes: Int) {
        require(bytes >= 0) { "Heap size must be non-negative" }
        mem = ByteArray(bytes)
        hp = 0
    }

    actual fun reset() { hp = 0 }

    actual fun dispose() {
        mem = ByteArray(0)
        hp = 0
    }

    actual fun malloc(bytes: Int): Int {
        require(bytes >= 0) { "Cannot allocate negative bytes" }
        val base = hp
        val end = base + bytes
        ensure(end)
        hp = end
        return base
    }

    actual fun calloc(count: Int, elemSize: Int): Int {
        require(count >= 0 && elemSize >= 0) { "Count and elemSize must be non-negative" }
        val total = count * elemSize
        val p = malloc(total)
        memset(p, 0, total)
        return p
    }

    actual fun free(ptr: Int) { /* no-op bump allocator */ }

    private fun ensure(minSize: Int) {
        if (minSize <= mem.size) return
        var newSize = mem.size.coerceAtLeast(1024)
        while (newSize < minSize) {
            newSize = newSize + (newSize ushr 1)  // 1.5x growth
        }
        val next = ByteArray(newSize)
        mem.copyInto(next, 0, 0, mem.size)
        mem = next
    }

    actual fun ensureCapacity(minSize: Int) = ensure(minSize)

    // ========== Typed Load/Store using Native Pointers ==========

    actual fun lb(addr: Int): Byte = mem[addr]

    actual fun sb(addr: Int, value: Byte) { mem[addr] = value }

    actual fun lbu(addr: Int): Int = mem[addr].toInt() and 0xFF

    actual fun lh(addr: Int): Short {
        mem.usePinned { pinned ->
            return pinned.addressOf(addr).reinterpret<ShortVar>().pointed.value
        }
    }

    actual fun sh(addr: Int, value: Short) {
        mem.usePinned { pinned ->
            pinned.addressOf(addr).reinterpret<ShortVar>().pointed.value = value
        }
    }

    actual fun lw(addr: Int): Int {
        mem.usePinned { pinned ->
            return pinned.addressOf(addr).reinterpret<IntVar>().pointed.value
        }
    }

    actual fun sw(addr: Int, value: Int) {
        mem.usePinned { pinned ->
            pinned.addressOf(addr).reinterpret<IntVar>().pointed.value = value
        }
    }

    actual fun ld(addr: Int): Long {
        mem.usePinned { pinned ->
            return pinned.addressOf(addr).reinterpret<LongVar>().pointed.value
        }
    }

    actual fun sd(addr: Int, value: Long) {
        mem.usePinned { pinned ->
            pinned.addressOf(addr).reinterpret<LongVar>().pointed.value = value
        }
    }

    actual fun lwf(addr: Int): Float {
        mem.usePinned { pinned ->
            return pinned.addressOf(addr).reinterpret<FloatVar>().pointed.value
        }
    }

    actual fun swf(addr: Int, value: Float) {
        mem.usePinned { pinned ->
            pinned.addressOf(addr).reinterpret<FloatVar>().pointed.value = value
        }
    }

    actual fun ldf(addr: Int): Double {
        mem.usePinned { pinned ->
            return pinned.addressOf(addr).reinterpret<DoubleVar>().pointed.value
        }
    }

    actual fun sdf(addr: Int, value: Double) {
        mem.usePinned { pinned ->
            pinned.addressOf(addr).reinterpret<DoubleVar>().pointed.value = value
        }
    }

    // ========== Bulk Memory Operations using Platform SIMD ==========

    actual fun memcpy(dst: Int, src: Int, bytes: Int) {
        if (bytes <= 0) return
        mem.usePinned { pinned ->
            val base = pinned.addressOf(0)
            posixMemcpy(
                (base + dst)!!,
                (base + src)!!,
                bytes.toULong()
            )
        }
    }

    actual fun memmove(dst: Int, src: Int, bytes: Int) {
        if (bytes <= 0) return
        mem.usePinned { pinned ->
            val base = pinned.addressOf(0)
            posixMemmove(
                (base + dst)!!,
                (base + src)!!,
                bytes.toULong()
            )
        }
    }

    actual fun memset(addr: Int, value: Int, bytes: Int) {
        if (bytes <= 0) return
        mem.usePinned { pinned ->
            posixMemset(
                pinned.addressOf(addr),
                value,
                bytes.toULong()
            )
        }
    }
}

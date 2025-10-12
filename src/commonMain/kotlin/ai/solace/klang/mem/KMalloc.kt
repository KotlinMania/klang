package ai.solace.klang.mem

/**
 * KMalloc: a tiny, pure-Kotlin heap allocator on top of GlobalHeap.
 *
 * Features (phase 1):
 * - 16-byte alignment, size normalization
 * - Segregated free lists for small sizes (<= 1024)
 * - Top-chunk bump allocation for others
 * - calloc/realloc provided (realloc copies)
 * - No coalescing yet (phase 2)
 *
 * Pointers are byte offsets (Int) into GlobalHeap.
 */
object KMalloc {
    private const val ALIGN = 16
    private const val HEADER_SIZE = 4 // size+flags packed in Int (LSB = inUse)
    private const val FOOTER_SIZE = 4
    private const val OVERHEAD = HEADER_SIZE + FOOTER_SIZE
    private const val MIN_CHUNK = 16 // payload min (>=4 so we can store next ptr)
    private const val SMALL_LIMIT = 1024
    private const val BIN_SHIFT = 4 // 16-byte classes
    private const val BIN_COUNT = (SMALL_LIMIT shr BIN_SHIFT)

    // Free-list heads: store chunk address (start of header) or -1 if empty
    private val bins = IntArray(BIN_COUNT) { -1 }
    private var largeFreeHead: Int = -1

    // Top pointer for bump allocation (address of next free byte)
    private var brk: Int = 0

    fun init(bytes: Int) {
        GlobalHeap.init(bytes)
        bins.fill(-1)
        brk = 0
    }

    fun reset() {
        bins.fill(-1)
        largeFreeHead = -1
        brk = 0
        GlobalHeap.reset()
    }

    fun dispose() {
        bins.fill(-1)
        brk = 0
        GlobalHeap.dispose()
    }

    fun malloc(bytes: Int): Int {
        val size = normalize(bytes)
        // Try bins first (first-fit with splitting)
        val fromBin = findAndPrepareChunk(size)
        if (fromBin != 0) return fromBin + HEADER_SIZE
        // allocate from top
        val total = OVERHEAD + size
        val chunk = brk
        ensureCapacity(chunk + total)
        writeHeaderFooter(chunk, size, inUse = true)
        brk += total
        return chunk + HEADER_SIZE
    }

    fun calloc(count: Int, elemSize: Int): Int {
        val bytes = count * elemSize
        val p = malloc(bytes)
        GlobalHeap.memset(p, 0, bytes)
        return p
    }

    fun free(ptr: Int) {
        if (ptr <= 0) return
        var chunk = ptr - HEADER_SIZE
        var size = readSize(chunk)
        // mark free
        writeHeaderFooter(chunk, size, inUse = false)
        // coalesce with next
        val next = nextChunk(chunk)
        if (next in 0 until brk && !isInUse(next)) {
            removeFromFreeList(next)
            val nextSize = readSize(next)
            size = size + OVERHEAD + nextSize
            writeHeaderFooter(chunk, size, inUse = false)
        }
        // coalesce with prev
        val prev = prevChunk(chunk)
        if (prev >= 0 && !isInUse(prev)) {
            removeFromFreeList(prev)
            val prevSize = readSize(prev)
            chunk = prev
            size = prevSize + OVERHEAD + size
            writeHeaderFooter(chunk, size, inUse = false)
        }
        pushFree(chunk)
    }

    fun realloc(ptr: Int, newSize: Int): Int {
        if (ptr == 0) return malloc(newSize)
        val chunk = ptr - HEADER_SIZE
        val oldSize = readSize(chunk)
        val size = normalize(newSize)
        if (size <= oldSize) {
            // Optional split if there is enough room for a new free chunk
            maybeSplit(chunk, oldSize, size)
            return ptr
        }
        val np = malloc(size)
        GlobalHeap.memcpy(np, ptr, oldSize)
        free(ptr)
        return np
    }

    private fun ensureCapacity(minSize: Int) {
        if (minSize <= GlobalHeap.size) return
        GlobalHeap.ensureCapacity(minSize)
    }

    private fun binIndexOrMinus1(size: Int): Int = if (size <= SMALL_LIMIT) ((size ushr BIN_SHIFT) - 1) else -1

    private fun normalize(n: Int): Int {
        val v = if (n <= 0) MIN_CHUNK else n
        val a = ((v + (ALIGN - 1)) / ALIGN) * ALIGN
        return a.coerceAtLeast(MIN_CHUNK)
    }

    private fun writeHeaderFooter(chunk: Int, size: Int, inUse: Boolean) {
        val tag = pack(size, inUse)
        GlobalHeap.sw(chunk, tag)
        GlobalHeap.sw(chunk + HEADER_SIZE + size, tag)
    }
    private fun pack(size: Int, inUse: Boolean): Int = (size shl 1) or (if (inUse) 1 else 0)
    private fun readTag(chunk: Int): Int = GlobalHeap.lw(chunk)
    private fun readSize(chunk: Int): Int = readTag(chunk) ushr 1
    private fun isInUse(chunk: Int): Boolean = (readTag(chunk) and 1) != 0
    private fun nextChunk(chunk: Int): Int = chunk + HEADER_SIZE + readSize(chunk) + FOOTER_SIZE
    private fun prevChunk(chunk: Int): Int {
        if (chunk < FOOTER_SIZE) return -1
        val prevTag = GlobalHeap.lw(chunk - FOOTER_SIZE)
        val prevSize = prevTag ushr 1
        val prevBase = chunk - (HEADER_SIZE + prevSize + FOOTER_SIZE)
        return if (prevBase >= 0) prevBase else -1
    }

    private fun pushFree(chunk: Int) {
        val size = readSize(chunk)
        val binIdx = binIndexOrMinus1(size)
        if (binIdx >= 0) {
            val head = bins[binIdx]
            GlobalHeap.sw(chunk + HEADER_SIZE, head)
            bins[binIdx] = chunk
        } else {
            // large list push front
            GlobalHeap.sw(chunk + HEADER_SIZE, largeFreeHead)
            largeFreeHead = chunk
        }
    }

    private fun removeFromFreeList(chunk: Int) {
        val size = readSize(chunk)
        val binIdx = binIndexOrMinus1(size)
        if (binIdx >= 0) {
            var cur = bins[binIdx]
            var prev = -1
            while (cur != -1) {
                if (cur == chunk) {
                    val next = GlobalHeap.lw(cur + HEADER_SIZE)
                    if (prev == -1) bins[binIdx] = next else GlobalHeap.sw(prev + HEADER_SIZE, next)
                    return
                }
                prev = cur
                cur = GlobalHeap.lw(cur + HEADER_SIZE)
            }
        } else {
            var cur = largeFreeHead
            var prev = -1
            while (cur != -1) {
                if (cur == chunk) {
                    val next = GlobalHeap.lw(cur + HEADER_SIZE)
                    if (prev == -1) largeFreeHead = next else GlobalHeap.sw(prev + HEADER_SIZE, next)
                    return
                }
                prev = cur
                cur = GlobalHeap.lw(cur + HEADER_SIZE)
            }
        }
    }

    private fun maybeSplit(chunk: Int, curSize: Int, wantSize: Int) {
        val remain = curSize - wantSize
        if (remain >= MIN_CHUNK + OVERHEAD) {
            // allocated front keeps wantSize; create a free tail
            writeHeaderFooter(chunk, wantSize, inUse = true)
            val tail = chunk + HEADER_SIZE + wantSize + FOOTER_SIZE
            writeHeaderFooter(tail, remain - OVERHEAD, inUse = false)
            pushFree(tail)
        }
    }

    private fun findAndPrepareChunk(size: Int): Int {
        // Search bins from target bin upward
        var idx = binIndexOrMinus1(size)
        if (idx >= 0) {
            while (idx < BIN_COUNT) {
                var cur = bins[idx]
                var prev = -1
                while (cur != -1) {
                    val curSize = readSize(cur)
                    if (curSize >= size) {
                        // remove from list
                        val next = GlobalHeap.lw(cur + HEADER_SIZE)
                        if (prev == -1) bins[idx] = next else GlobalHeap.sw(prev + HEADER_SIZE, next)
                        // split if needed
                        val remain = curSize - size
                        if (remain >= MIN_CHUNK + OVERHEAD) {
                            val tail = cur + HEADER_SIZE + size + FOOTER_SIZE
                            writeHeaderFooter(tail, remain - OVERHEAD, inUse = false)
                            pushFree(tail)
                            writeHeaderFooter(cur, size, inUse = true)
                        } else {
                            writeHeaderFooter(cur, curSize, inUse = true)
                        }
                        return cur
                    }
                    prev = cur
                    cur = GlobalHeap.lw(prev + HEADER_SIZE)
                }
                idx++
            }
        }
        // Search large list first-fit
        var cur = largeFreeHead
        var prev = -1
        while (cur != -1) {
            val curSize = readSize(cur)
            if (curSize >= size) {
                val next = GlobalHeap.lw(cur + HEADER_SIZE)
                if (prev == -1) largeFreeHead = next else GlobalHeap.sw(prev + HEADER_SIZE, next)
                val remain = curSize - size
                if (remain >= MIN_CHUNK + OVERHEAD) {
                    val tail = cur + HEADER_SIZE + size + FOOTER_SIZE
                    writeHeaderFooter(tail, remain - OVERHEAD, inUse = false)
                    pushFree(tail)
                    writeHeaderFooter(cur, size, inUse = true)
                } else {
                    writeHeaderFooter(cur, curSize, inUse = true)
                }
                return cur
            }
            prev = cur
            cur = GlobalHeap.lw(cur + HEADER_SIZE)
        }
        return 0
    }
}

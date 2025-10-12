package ai.solace.klang.mem

/**
 * KStack: Heap-backed stack frames for C-like automatic storage.
 *
 * Design
 * - A contiguous stack region is allocated from KMalloc on init().
 * - The stack grows downward inside that region. Addresses returned are
 *   stable Int byte offsets into GlobalHeap (no copying).
 * - Frames: pushFrame() returns a marker; popFrame(marker) restores SP.
 * - alloca(size, align) reserves aligned space inside the current frame
 *   and returns its absolute heap address.
 *
 * Notes
 * - This is single-threaded and does not enforce recursion depth; callers
 *   must pair push/pop correctly (like manual RAII).
 * - Alignment rules: default 16-byte; pass stricter alignment if needed.
 */
object KStack {
    private var allocBase: Int = 0
    private var base: Int = 0
    private var size: Int = 0
    private var sp: Int = 0 // offset from base; counts down from size
    private var initialized: Boolean = false

    fun init(bytes: Int = 1 shl 20) { // default 1 MiB stack region
        check(bytes > 0) { "Stack size must be positive" }
        val total = bytes + 15
        allocBase = KMalloc.calloc(total, 1)
        base = alignUp(allocBase, 16)
        size = bytes
        sp = size
        initialized = true
    }

    fun dispose() {
        // Stack region is owned by KMalloc; free it to avoid leaks.
        if (initialized) {
            KMalloc.free(allocBase)
        }
        allocBase = 0; base = 0; size = 0; sp = 0; initialized = false
    }

    fun reset() { ensureInit(); sp = size }

    // Frame management --------------------------------------------------------

    fun pushFrame(align: Int = 16): Int {
        ensureInit(); checkAlign(align)
        sp = alignDown(sp, align)
        return sp // marker
    }

    fun popFrame(marker: Int) {
        ensureInit()
        check(marker in 0..size) { "Invalid frame marker" }
        sp = marker
    }

    // Allocation --------------------------------------------------------------

    fun alloca(bytes: Int, align: Int = 16): Int {
        ensureInit(); check(bytes >= 0) { "bytes must be >= 0" }; checkAlign(align)
        val newSp = alignDown(sp - bytes, align)
        check(newSp >= 0) { "Stack overflow: need=$bytes, sp=$sp, size=$size" }
        sp = newSp
        return base + sp
    }

    // Scoped frame helper -----------------------------------------------------
    inline fun <T> withFrame(align: Int = 16, block: () -> T): T {
        val mark = pushFrame(align)
        return try { block() } finally { popFrame(mark) }
    }

    // Utilities ---------------------------------------------------------------

    fun currentSp(): Int { ensureInit(); return base + sp }
    fun capacityBytes(): Int = size
    fun usedBytes(): Int = size - sp

    private fun ensureInit() { check(initialized) { "KStack not initialized" } }

    private fun checkAlign(align: Int) {
        check(align > 0 && (align and (align - 1)) == 0) { "Alignment must be power of two" }
    }

    private fun alignDown(x: Int, align: Int): Int = x and (align - 1).inv()
    private fun alignUp(x: Int, align: Int): Int = (x + (align - 1)) and (align - 1).inv()
}

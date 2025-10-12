package ai.solace.klang.common

import ai.solace.klang.mem.GlobalHeap

/** C-like struct/union layout helpers (natural alignment, little-endian). */
object StructLayout {
    data class Field(val size: Int, val align: Int)
    data class Layout(val offsets: IntArray, val size: Int, val align: Int)

    /** Computes natural-aligned struct layout. size/align in bytes. */
    fun layoutStruct(fields: List<Field>): Layout {
        require(fields.isNotEmpty())
        val offsets = IntArray(fields.size)
        var offset = 0
        var maxAlign = 1
        for ((i, f) in fields.withIndex()) {
            val a = f.align.coerceAtLeast(1)
            maxAlign = maxOf(maxAlign, a)
            val aligned = ((offset + (a - 1)) / a) * a
            offsets[i] = aligned
            offset = aligned + f.size
        }
        val total = ((offset + (maxAlign - 1)) / maxAlign) * maxAlign
        return Layout(offsets, total, maxAlign)
    }

    /** Computes union layout (max size, max align). */
    fun layoutUnion(fields: List<Field>): Layout {
        require(fields.isNotEmpty())
        var size = 0
        var align = 1
        for (f in fields) { size = maxOf(size, f.size); align = maxOf(align, f.align) }
        val total = ((size + (align - 1)) / align) * align
        return Layout(IntArray(fields.size) { 0 }, total, align)
    }

    /** Allocates a struct of given layout on the heap and returns its base address. */
    fun alloc(layout: Layout): Int = GlobalHeap.malloc(layout.size)
}


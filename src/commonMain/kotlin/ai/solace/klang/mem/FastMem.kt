package ai.solace.klang.mem

/** musl-style, word-at-a-time memset/memcpy/memmove on GlobalHeap. */
internal object FastMem {
    private const val WORD_BYTES = 8
    private const val WORD_MASK = WORD_BYTES - 1

    private fun repeatByte(b: Int): Long {
        val v = (b and 0xFF).toLong()
        return v or (v shl 8) or (v shl 16) or (v shl 24) or (v shl 32) or (v shl 40) or (v shl 48) or (v shl 56)
    }

    private fun storeWord(addr: Int, w: Long) {
        var x = w
        for (i in 0 until WORD_BYTES) {
            GlobalHeap.sb(addr + i, (x and 0xFF).toByte())
            x = x ushr 8
        }
    }

    private fun loadWord(addr: Int): Long {
        var r = 0L
        for (i in (WORD_BYTES - 1) downTo 0) {
            r = (r shl 8) or GlobalHeap.lbu(addr + i).toLong()
        }
        return r
    }

    fun memset(addr: Int, value: Int, bytes: Int) {
        if (bytes <= 0) return
        var p = addr
        var n = bytes
        // Align to word
        while (n > 0 && (p and WORD_MASK) != 0) {
            GlobalHeap.sb(p++, (value and 0xFF).toByte()); n--
        }
        if (n >= WORD_BYTES) {
            val w = repeatByte(value)
            while (n >= WORD_BYTES) {
                storeWord(p, w); p += WORD_BYTES; n -= WORD_BYTES
            }
        }
        while (n-- > 0) GlobalHeap.sb(p++, (value and 0xFF).toByte())
    }

    /** memcpy: undefined behavior for overlap. */
    fun memcpy(dst: Int, src: Int, bytes: Int) {
        if (bytes <= 0 || dst == src) return
        var d = dst
        var s = src
        var n = bytes
        // Align destination; copy bytes
        while (n > 0 && (d and WORD_MASK) != 0) {
            GlobalHeap.sb(d++, GlobalHeap.lb(s++)); n--
        }
        // Bulk words
        while (n >= WORD_BYTES) {
            val w = loadWord(s)
            storeWord(d, w)
            d += WORD_BYTES; s += WORD_BYTES; n -= WORD_BYTES
        }
        // Tail
        while (n-- > 0) GlobalHeap.sb(d++, GlobalHeap.lb(s++))
    }

    /** memmove: overlap-safe; chooses direction. */
    fun memmove(dst: Int, src: Int, bytes: Int) {
        if (bytes <= 0 || dst == src) return
        if (dst > src && dst < src + bytes) {
            // Copy backwards
            var d = dst + bytes
            var s = src + bytes
            var n = bytes
            // Align backward
            while (n > 0 && (d and WORD_MASK) != 0) {
                d--; s--; GlobalHeap.sb(d, GlobalHeap.lb(s)); n--
            }
            while (n >= WORD_BYTES) {
                d -= WORD_BYTES; s -= WORD_BYTES
                val w = loadWord(s)
                storeWord(d, w)
                n -= WORD_BYTES
            }
            while (n-- > 0) { d--; s--; GlobalHeap.sb(d, GlobalHeap.lb(s)) }
        } else {
            memcpy(dst, src, bytes)
        }
    }
}


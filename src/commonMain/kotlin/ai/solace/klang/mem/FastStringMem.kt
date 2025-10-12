package ai.solace.klang.mem

/** musl-style word-at-a-time implementations for core string/memory ops. */
internal object FastStringMem {
    private const val WORD_BYTES = 8
    private const val WORD_MASK: Int = WORD_BYTES - 1
    private const val BYTE_MASK = 0xFF
    private const val INT_MAX = Int.MAX_VALUE

    private const val M1: Long = 0x0101010101010101L
    // 0x8080808080808080 as signed Long literal:
    private const val M2: Long = -0x7F7F7F7F7F7F7F80L

    private inline fun hasZeroByte(x: Long): Boolean = ((x - M1) and x.inv() and M2) != 0L

    private inline fun repeatByte(b: Int): Long {
        val v = (b and BYTE_MASK).toLong()
        return v or (v shl 8) or (v shl 16) or (v shl 24) or (v shl 32) or (v shl 40) or (v shl 48) or (v shl 56)
    }

    private inline fun loadWord(addr: Int): Long {
        // Little-endian assemble 8 bytes
        var r = 0L
        var i = 7
        while (i >= 0) { r = (r shl 8) or GlobalHeap.lbu(addr + i).toLong(); i-- }
        return r
    }

    fun strlen(addr: Int): Int {
        var p = addr
        // Align to word
        while ((p and WORD_MASK) != 0) {
            if (GlobalHeap.lbu(p) == 0) return p - addr
            p++
        }
        // Scan by words
        while (true) {
            val w = loadWord(p)
            if (hasZeroByte(w)) break
            p += WORD_BYTES
        }
        // Finish byte-wise
        while (GlobalHeap.lbu(p) != 0) p++
        return p - addr
    }

    fun memchr(addr: Int, c: Int, n: Int): Int {
        if (n <= 0) return 0
        var p = addr
        var rem = n
        val cword = repeatByte(c)

        // Align
        while (rem > 0 && (p and WORD_MASK) != 0) {
            if (GlobalHeap.lbu(p) == (c and BYTE_MASK)) return p
            p++; rem--
        }
        // Words
        while (rem >= WORD_BYTES) {
            val w = loadWord(p)
            val x = w xor cword
            if (hasZeroByte(x)) {
                // Locate exact byte
                var i = 0
                while (i < WORD_BYTES) {
                    if (GlobalHeap.lbu(p + i) == (c and BYTE_MASK)) return p + i
                    i++
                }
            }
            p += WORD_BYTES; rem -= WORD_BYTES
        }
        // Tail
        while (rem > 0) {
            if (GlobalHeap.lbu(p) == (c and BYTE_MASK)) return p
            p++; rem--
        }
        return 0
    }

    fun memcmp(a: Int, b: Int, n: Int): Int {
        if (n <= 0) return 0
        var pa = a
        var pb = b
        var rem = n
        // Align
        while (rem > 0 && ((pa or pb) and WORD_MASK) != 0) {
            val da = GlobalHeap.lbu(pa)
            val db = GlobalHeap.lbu(pb)
            if (da != db) return da - db
            pa++; pb++; rem--
        }
        // Words
        while (rem >= WORD_BYTES) {
            val wa = loadWord(pa)
            val wb = loadWord(pb)
            if (wa != wb) {
                // Find first differing byte in this word
                var i = 0
                while (i < WORD_BYTES) {
                    val da = GlobalHeap.lbu(pa + i)
                    val db = GlobalHeap.lbu(pb + i)
                    if (da != db) return da - db
                    i++
                }
            }
            pa += WORD_BYTES; pb += WORD_BYTES; rem -= WORD_BYTES
        }
        // Tail
        while (rem > 0) {
            val da = GlobalHeap.lbu(pa)
            val db = GlobalHeap.lbu(pb)
            if (da != db) return da - db
            pa++; pb++; rem--
        }
        return 0
    }

    fun strcmp(a: Int, b: Int): Int {
        var pa = a
        var pb = b
        // Align
        while (((pa or pb) and WORD_MASK) != 0) {
            val da = GlobalHeap.lbu(pa)
            val db = GlobalHeap.lbu(pb)
            if (da != db || da == 0) return da - db
            pa++; pb++
        }
        // Words
        while (true) {
            val wa = loadWord(pa)
            val wb = loadWord(pb)
            if (wa != wb) {
                // differ; locate exact byte
                var i = 0
                while (i < WORD_BYTES) {
                    val da = GlobalHeap.lbu(pa + i)
                    val db = GlobalHeap.lbu(pb + i)
                    if (da != db || da == 0) return da - db
                    i++
                }
            }
            if (hasZeroByte(wa)) return 0 // equal up to NUL in this word
            pa += WORD_BYTES; pb += WORD_BYTES
        }
    }
}

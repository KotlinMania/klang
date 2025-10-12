package ai.solace.klang.mem

/** CString helpers on top of GlobalHeap (addresses are Int byte offsets). */
object CString {
    fun strlenz(addr: Int): Int {
        var i = 0
        while (true) {
            if (GlobalHeap.lb(addr + i).toInt() == 0) return i
            i++
        }
    }

    fun read(addr: Int): String {
        val len = strlenz(addr)
        val out = CharArray(len)
        var i = 0
        while (i < len) {
            val ch = GlobalHeap.lbu(addr + i)
            out[i] = ch.toChar()
            i++
        }
        return out.concatToString()
    }

    /** Writes s plus trailing NUL at `addr`. Returns bytes written excluding NUL. */
    fun write(addr: Int, s: String): Int {
        var i = 0
        while (i < s.length) {
            GlobalHeap.sb(addr + i, (s[i].code and 0xFF).toByte())
            i++
        }
        GlobalHeap.sb(addr + i, 0)
        return s.length
    }

    /** Allocates and duplicates the C-string. Returns address of first char. */
    fun strdup(s: String): Int {
        val addr = KMalloc.malloc(s.length + 1)
        write(addr, s)
        return addr
    }
}

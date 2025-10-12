package ai.solace.klang.mem

/**
 * Minimal C-like string/memory routines backed by GlobalHeap.
 * Addresses are Int byte offsets. Return 0 represents null pointer.
 */
object CLib {
    fun strlen(s: Int): Int = FastStringMem.strlen(s)
    fun strnlen(s: Int, n: Int): Int {
        var i = 0
        while (i < n) {
            if (GlobalHeap.lb(s + i).toInt() == 0) break
            i++
        }
        return i
    }

    fun strcmp(a: Int, b: Int): Int = FastStringMem.strcmp(a, b)

    fun strcpy(dst: Int, src: Int): Int {
        var i = 0
        while (true) {
            val b = GlobalHeap.lbu(src + i)
            GlobalHeap.sb(dst + i, (b and 0xFF).toByte())
            if (b == 0) return dst
            i++
        }
    }

    fun strncpy(dst: Int, src: Int, n: Int): Int {
        var i = 0
        while (i < n) {
            val b = GlobalHeap.lbu(src + i)
            GlobalHeap.sb(dst + i, (b and 0xFF).toByte())
            i++
            if (b == 0) {
                // pad the rest with NULs
                while (i < n) { GlobalHeap.sb(dst + i, 0); i++ }
                break
            }
        }
        return dst
    }

    fun strncmp(a: Int, b: Int, n: Int): Int {
        var i = 0
        if (n <= 0) return 0
        while (i < n) {
            val ca = GlobalHeap.lbu(a + i)
            val cb = GlobalHeap.lbu(b + i)
            if (ca != cb || ca == 0) return ca - cb
            i++
        }
        return 0
    }

    fun memchr(addr: Int, c: Int, n: Int): Int = FastStringMem.memchr(addr, c, n)

    fun strchr(addr: Int, c: Int): Int {
        val needle = c and 0xFF
        var i = 0
        while (true) {
            val b = GlobalHeap.lbu(addr + i)
            if (b == needle) return addr + i
            if (b == 0) return 0
            i++
        }
    }

    fun memcmp(a: Int, b: Int, n: Int): Int = FastStringMem.memcmp(a, b, n)
}

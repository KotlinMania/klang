package ai.solace.klang.mem

/** Typed views over GlobalHeap (little-endian). */

data class U8View(val base: Int, val length: Int) {
    fun get(i: Int): Int { require(i in 0 until length); return GlobalHeap.lbu(base + i) }
    fun set(i: Int, v: Int) { require(i in 0 until length); GlobalHeap.sb(base + i, (v and 0xFF).toByte()) }
    fun fill(v: Int) { GlobalHeap.memset(base, v, length) }
    fun copyFrom(src: U8View) { require(src.length == length); GlobalHeap.memcpy(base, src.base, length) }
    fun slice(offset: Int, len: Int): U8View { require(offset>=0 && len>=0 && offset+len<=length); return U8View(base+offset,len) }
}

data class U16View(val base: Int, val limbCount: Int) {
    fun get(i: Int): Int { require(i in 0 until limbCount); return (GlobalHeap.lbu(base + i*2) or (GlobalHeap.lbu(base + i*2 + 1) shl 8)) and 0xFFFF }
    fun set(i: Int, v: Int) { require(i in 0 until limbCount); val vv=v and 0xFFFF; GlobalHeap.sb(base+i*2,(vv and 0xFF).toByte()); GlobalHeap.sb(base+i*2+1,((vv ushr 8) and 0xFF).toByte()) }
    fun fillZero() { GlobalHeap.memset(base, 0, limbCount*2) }
    fun slice(offset: Int, len: Int): U16View { require(offset>=0 && len>=0 && offset+len<=limbCount); return U16View(base+offset*2, len) }
}

data class U32View(val base: Int, val wordCount: Int) {
    fun get(i: Int): Int { require(i in 0 until wordCount); return GlobalHeap.lw(base + i*4) }
    fun set(i: Int, v: Int) { require(i in 0 until wordCount); GlobalHeap.sw(base + i*4, v) }
    fun fillZero() { GlobalHeap.memset(base, 0, wordCount*4) }
    fun slice(offset: Int, len: Int): U32View { require(offset>=0 && len>=0 && offset+len<=wordCount); return U32View(base+offset*4, len) }
}


// Default package: C-like function symbols mapped to our heap-backed CLib.

fun strlen(s: CPointer<Byte>): Int = ai.solace.klang.mem.CLib.strlen(s.ptr)
fun strnlen(s: CPointer<Byte>, n: Int): Int = ai.solace.klang.mem.CLib.strnlen(s.ptr, n)
fun strcmp(a: CPointer<Byte>, b: CPointer<Byte>): Int = ai.solace.klang.mem.CLib.strcmp(a.ptr, b.ptr)
fun strncmp(a: CPointer<Byte>, b: CPointer<Byte>, n: Int): Int = ai.solace.klang.mem.CLib.strncmp(a.ptr, b.ptr, n)

fun strcpy(dst: CPointer<Byte>, src: CPointer<Byte>): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.strcpy(dst.ptr, src.ptr))
fun strncpy(dst: CPointer<Byte>, src: CPointer<Byte>, n: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.strncpy(dst.ptr, src.ptr, n))

fun memchr(addr: CPointer<Byte>, c: Int, n: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.memchr(addr.ptr, c, n))
fun strchr(addr: CPointer<Byte>, c: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.strchr(addr.ptr, c))
fun memcmp(a: CPointer<Byte>, b: CPointer<Byte>, n: Int): Int = ai.solace.klang.mem.CLib.memcmp(a.ptr, b.ptr, n)

// mem* wrappers (addresses via CPointer)
fun memcpy(dst: CPointer<Byte>, src: CPointer<Byte>, n: Int): CPointer<Byte> {
    ai.solace.klang.mem.GlobalHeap.memcpy(dst.ptr, src.ptr, n)
    return dst
}
fun memmove(dst: CPointer<Byte>, src: CPointer<Byte>, n: Int): CPointer<Byte> {
    ai.solace.klang.mem.GlobalHeap.memmove(dst.ptr, src.ptr, n)
    return dst
}
fun memset(dst: CPointer<Byte>, c: Int, n: Int): CPointer<Byte> {
    ai.solace.klang.mem.GlobalHeap.memset(dst.ptr, c, n)
    return dst
}

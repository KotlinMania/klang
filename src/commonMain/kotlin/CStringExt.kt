// Default package extensions to use CString with CPointer<Byte>

fun CPointer<Byte>.strlenz(): Int = ai.solace.klang.mem.CString.strlenz(this.ptr)
fun CPointer<Byte>.readCString(): String = ai.solace.klang.mem.CString.read(this.ptr)
fun CPointer<Byte>.writeCString(s: String): Int = ai.solace.klang.mem.CString.write(this.ptr, s)

fun strdupCString(s: String): CPointer<Byte> = CPointer(ai.solace.klang.mem.CString.strdup(s))

// CLib wrappers
fun CPointer<Byte>.strcpy(src: CPointer<Byte>): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.strcpy(this.ptr, src.ptr))
fun CPointer<Byte>.strncpy(src: CPointer<Byte>, n: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.strncpy(this.ptr, src.ptr, n))
fun CPointer<Byte>.strcmp(other: CPointer<Byte>): Int = ai.solace.klang.mem.CLib.strcmp(this.ptr, other.ptr)
fun CPointer<Byte>.strchr(c: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.strchr(this.ptr, c))
fun CPointer<Byte>.memchr(c: Int, n: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.CLib.memchr(this.ptr, c, n))
fun CPointer<Byte>.memcmp(other: CPointer<Byte>, n: Int): Int = ai.solace.klang.mem.CLib.memcmp(this.ptr, other.ptr, n)

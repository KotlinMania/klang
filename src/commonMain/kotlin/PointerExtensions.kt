// Default package: extend CPointer<T> utilities in the same namespace.

// Element indexing (byte strides)
inline fun CPointer<Byte>.index(i: Int): CPointer<Byte> = CPointer(this.ptr + i)
inline fun CPointer<Short>.index(i: Int): CPointer<Short> = CPointer(this.ptr + i * 2)
inline fun CPointer<Int>.index(i: Int): CPointer<Int> = CPointer(this.ptr + i * 4)
inline fun CPointer<Long>.index(i: Int): CPointer<Long> = CPointer(this.ptr + i * 8)
inline fun CPointer<Float>.index(i: Int): CPointer<Float> = CPointer(this.ptr + i * 4)
inline fun CPointer<Double>.index(i: Int): CPointer<Double> = CPointer(this.ptr + i * 8)

// Typed load/store via GlobalHeap
fun CPointer<Byte>.load(): Byte = ai.solace.klang.mem.GlobalHeap.lb(ptr)
fun CPointer<Byte>.store(v: Byte) = ai.solace.klang.mem.GlobalHeap.sb(ptr, v)

fun CPointer<Short>.load(): Short = ai.solace.klang.mem.GlobalHeap.lh(ptr)
fun CPointer<Short>.store(v: Short) = ai.solace.klang.mem.GlobalHeap.sh(ptr, v)

fun CPointer<Int>.load(): Int = ai.solace.klang.mem.GlobalHeap.lw(ptr)
fun CPointer<Int>.store(v: Int) = ai.solace.klang.mem.GlobalHeap.sw(ptr, v)

fun CPointer<Long>.load(): Long = ai.solace.klang.mem.GlobalHeap.ld(ptr)
fun CPointer<Long>.store(v: Long) = ai.solace.klang.mem.GlobalHeap.sd(ptr, v)

fun CPointer<Float>.load(): Float = ai.solace.klang.mem.GlobalHeap.lwf(ptr)
fun CPointer<Float>.store(v: Float) = ai.solace.klang.mem.GlobalHeap.swf(ptr, v)

fun CPointer<Double>.load(): Double = ai.solace.klang.mem.GlobalHeap.ldf(ptr)
fun CPointer<Double>.store(v: Double) = ai.solace.klang.mem.GlobalHeap.sdf(ptr, v)

// C-like allocators returning typed pointers
fun mallocBytes(n: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.GlobalHeap.malloc(n))
fun callocBytes(n: Int): CPointer<Byte> = CPointer(ai.solace.klang.mem.GlobalHeap.calloc(n, 1))


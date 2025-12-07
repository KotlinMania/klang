package ai.solace.klang.mem

/**
 * GlobalHeap: A single, deterministic heap for all C-like memory operations.
 *
 * KLang's foundation provides C-like memory semantics across all Kotlin multiplatform targets.
 * All "pointers" are [Int] byte offsets into the heap, enabling:
 * - Porting C programs that expect contiguous memory
 * - Zero-copy operations (modify data in-place)
 * - Deterministic serialization (dump/restore entire heap state)
 * - Predictable performance (no GC pauses)
 *
 * ## Platform Implementations
 *
 * - **Native (macOS/Linux)**: Uses pinned memory with typed pointer access for optimal performance
 * - **JS**: Uses ByteArray with manual byte assembly
 *
 * ## Memory Model
 *
 * ```
 * GlobalHeap Memory Layout:
 * ┌─────────────────────────────────────────────┐
 * │  Used Memory (0 to hp)  │  Free Space       │
 * └─────────────────────────────────────────────┘
 *                           ↑
 *                          hp (heap pointer)
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * GlobalHeap.init(1024 * 1024)  // 1MB heap
 * val addr = GlobalHeap.malloc(128)
 * GlobalHeap.sw(addr, 42)
 * val value = GlobalHeap.lw(addr)  // returns 42
 * ```
 *
 * ## Thread Safety
 *
 * **Not thread-safe**. External synchronization required for concurrent access.
 *
 * @see KMalloc For free-list allocator with malloc/free semantics
 * @see CScalars For type-safe scalar variables on the heap
 * @since 0.1.0
 */
expect object GlobalHeap {
    /** Total heap size in bytes. */
    val size: Int

    /** Number of bytes currently allocated (used). */
    val used: Int

    /** Initialize or reinitialize the heap with specified size. */
    fun init(bytes: Int)

    /** Reset heap pointer to 0, effectively freeing all allocations. */
    fun reset()

    /** Dispose of the heap, releasing the backing memory. */
    fun dispose()

    /** Allocate uninitialized memory (like C's malloc). */
    fun malloc(bytes: Int): Int

    /** Allocate zero-initialized memory (like C's calloc). */
    fun calloc(count: Int, elemSize: Int): Int

    /** Free memory (no-op in bump allocator). */
    fun free(ptr: Int)

    /** Ensure heap capacity, growing if necessary. */
    fun ensureCapacity(minSize: Int)

    // ========== Typed Load/Store Operations (Little-Endian) ==========

    /** Load byte (signed). */
    fun lb(addr: Int): Byte

    /** Store byte. */
    fun sb(addr: Int, value: Byte)

    /** Load byte (unsigned, zero-extended to Int). */
    fun lbu(addr: Int): Int

    /** Load half-word (16-bit signed short, little-endian). */
    fun lh(addr: Int): Short

    /** Store half-word (16-bit short, little-endian). */
    fun sh(addr: Int, value: Short)

    /** Load word (32-bit signed int, little-endian). */
    fun lw(addr: Int): Int

    /** Store word (32-bit int, little-endian). */
    fun sw(addr: Int, value: Int)

    /** Load doubleword (64-bit signed long, little-endian). */
    fun ld(addr: Int): Long

    /** Store doubleword (64-bit long, little-endian). */
    fun sd(addr: Int, value: Long)

    /** Load float (32-bit). */
    fun lwf(addr: Int): Float

    /** Store float (32-bit). */
    fun swf(addr: Int, value: Float)

    /** Load double (64-bit). */
    fun ldf(addr: Int): Double

    /** Store double (64-bit). */
    fun sdf(addr: Int, value: Double)

    /** memcpy: undefined for overlap. */
    fun memcpy(dst: Int, src: Int, bytes: Int)

    /** memmove: overlap-safe. */
    fun memmove(dst: Int, src: Int, bytes: Int)

    /** memset: fill memory with byte value. */
    fun memset(addr: Int, value: Int, bytes: Int)
}

@file:Suppress("unused")
package ai.solace.klang.bitwise

/**
 * ArithmeticBitwiseOps64: Pure arithmetic bitwise operations dedicated to 64-bit values.
 *
 * @native-bitshift-allowed This is a core BitShift implementation file.
 * Native bitwise operations are NOT used here; everything is implemented via
 * Long arithmetic that naturally wraps mod 2^64. The exemption tag merely marks
 * this as a foundation file alongside [ArithmeticBitwiseOps].
 *
 * Companion to [ArithmeticBitwiseOps] (which is hard-capped at 1..32-bit by design).
 * This class is a separately-designed 64-bit implementation that exploits the
 * property that Kotlin's `Long` IS a 64-bit two's-complement word, so:
 *
 * - The bit pattern of a `Long` literally is the unsigned 64-bit value.
 * - Long multiplication wraps mod 2^64 — exactly what unsigned 64-bit `<<` needs.
 * - "Normalize" is the identity function (every Long is a valid 64-bit pattern).
 *
 * What this class avoids:
 *
 * - No `shl`/`shr`/`ushr`/`and`/`or`/`xor`/`inv` operators.
 * - No hardcoded hex masks.
 * - No platform-dependent behavior (Long arithmetic is identical on JVM/Native/JS Long polyfill).
 *
 * What it provides (the operations Runtime.kt and similar memory primitives need):
 *
 * - [leftShift] (unsigned semantics, wraps mod 2^64).
 * - [unsignedRightShift] (logical right shift, treats Long as unsigned).
 * - [or], [and], [xor], [not] (bit-by-bit, arithmetic-only).
 *
 * ### Why a separate class?
 *
 * [ArithmeticBitwiseOps] is documented and asserted to be 1..32-bit. Its internal
 * `pow2(bitLength)` would overflow to 0 for bitLength=64, breaking `normalize`'s
 * `value % (maxValue + 1L)` (mod-by-zero) and `leftShift`'s `% mod`. Retrofitting
 * special cases into that class would muddle a clean abstraction. A purpose-built
 * 64-bit class is simpler and faster.
 *
 * ### Performance
 *
 * - [leftShift]: O(1) — single multiply.
 * - [unsignedRightShift]: O(1) — at most one Long divide plus a constant addend.
 * - [or]/[and]/[xor]: O(64) — bit-by-bit loop with sign-bit peel for negative inputs.
 * - [not]: O(1) — `(-1L) - value`.
 *
 * ### Singleton
 *
 * The class is a stateless `object`; call its members directly
 * (e.g. `ArithmeticBitwiseOps64.or(a, b)`).
 */
object ArithmeticBitwiseOps64 {
    /** All-ones 64-bit mask (== -1L). */
    private const val MASK64: Long = -1L

    /** 2^63 represented as a Long: this is `Long.MIN_VALUE`. */
    private const val SIGN_BIT: Long = Long.MIN_VALUE

    /**
     * Pre-computed powers of two, `POW2[n] = 2^n` for n in 0..62.
     * `2^63` cannot be represented as a positive Long (== Long.MIN_VALUE),
     * so callers must special-case bits == 63.
     */
    private val POW2: LongArray = LongArray(63).also { arr ->
        arr[0] = 1L
        for (i in 1 until 63) arr[i] = arr[i - 1] * 2L
    }

    /**
     * Returns 2^n as a Long for n in 0..63.
     * For n == 63 the result is `Long.MIN_VALUE` (the bit pattern of 2^63).
     */
    private fun pow2(n: Int): Long {
        require(n in 0..63) { "pow2 exponent out of range: $n" }
        return if (n == 63) SIGN_BIT else POW2[n]
    }

    /**
     * Identity normalization: every Long is a valid 64-bit pattern.
     * Provided for API symmetry with [ArithmeticBitwiseOps].
     */
    fun normalize(value: Long): Long = value

    /**
     * Unsigned 64-bit left shift: `value << bits` with wrap mod 2^64.
     *
     * Implemented as Long multiplication, which wraps automatically.
     * For `bits !in 0..63` the result is 0L (matches C `<<` undefined-behavior choice
     * we already use elsewhere: out-of-range shifts return 0).
     */
    fun leftShift(value: Long, bits: Int): Long {
        if (bits !in 0..63) return 0L
        if (bits == 0) return value
        if (bits == 63) {
            // value * 2^63: only the lowest bit of `value` survives, in bit 63.
            return if ((value % 2L) == 0L) 0L else SIGN_BIT
        }
        return value * POW2[bits]
    }

    /**
     * Unsigned 64-bit logical right shift: shifts in zeros at the top.
     *
     * For non-negative `value`, this is plain `value / 2^bits`.
     * For negative `value` (i.e. bit 63 set when interpreted unsigned), we peel
     * off the sign bit, divide the lower 63 bits, and reinject the sign-bit
     * contribution at position `63 - bits`.
     */
    fun unsignedRightShift(value: Long, bits: Int): Long {
        if (bits !in 0..63) return 0L
        if (bits == 0) return value
        if (value >= 0L) return value / pow2(bits)

        // Negative Long → unsigned has bit 63 set.
        // unsigned(value) = value + 2^64. We can't form 2^64; instead split:
        //   value = SIGN_BIT + low,  where low = value - SIGN_BIT  (low in [0, 2^63))
        val low = value - SIGN_BIT
        if (bits == 63) {
            // unsigned >>> 63 keeps only bit 63 → result is 1.
            return 1L
        }
        // unsignedShifted = (low / 2^bits) + (2^63 / 2^bits) = (low / 2^bits) + 2^(63-bits)
        val shiftedLow = low / POW2[bits]
        val signContribution = POW2[63 - bits]
        return shiftedLow + signContribution
    }

    /**
     * Bitwise OR on 64-bit values, computed bit-by-bit using only arithmetic.
     *
     * To avoid Kotlin's truncating `%` and `/` semantics on negative Longs,
     * the sign bit (bit 63) is peeled off first; the remaining 63 bits are
     * iterated on non-negative Longs.
     */
    fun or(a: Long, b: Long): Long = combine(a, b, OP_OR)

    /** Bitwise AND on 64-bit values; see [or] for the algorithm shape. */
    fun and(a: Long, b: Long): Long = combine(a, b, OP_AND)

    /** Bitwise XOR on 64-bit values; see [or] for the algorithm shape. */
    fun xor(a: Long, b: Long): Long = combine(a, b, OP_XOR)

    /** Bitwise NOT on a 64-bit value: `~value` == `-1L - value` in two's complement. */
    fun not(value: Long): Long = MASK64 - value

    // ---- Internal combine helpers ----

    private const val OP_OR: Int = 0
    private const val OP_AND: Int = 1
    private const val OP_XOR: Int = 2

    private fun combine(a: Long, b: Long, op: Int): Long {
        // Peel sign bits.
        val sa: Long = if (a < 0L) 1L else 0L
        val sb: Long = if (b < 0L) 1L else 0L
        val lowA: Long = if (sa == 1L) a - SIGN_BIT else a
        val lowB: Long = if (sb == 1L) b - SIGN_BIT else b

        // Compute the sign-bit (bit 63) of the result purely arithmetically.
        val resSign: Long = when (op) {
            OP_OR -> if (sa + sb >= 1L) 1L else 0L
            OP_AND -> sa * sb
            OP_XOR -> (sa + sb) % 2L
            else -> 0L
        }

        // Iterate the remaining 63 bits on non-negative Longs.
        var result: Long = 0L
        var powerOf2: Long = 1L
        var remA: Long = lowA
        var remB: Long = lowB

        for (i in 0 until 63) {
            if (remA == 0L && remB == 0L) {
                // Early-exit only safe for OR/XOR; AND can keep going but its remaining
                // bits all evaluate to 0 anyway when one operand has run out.
                break
            }
            val bitA: Long = remA % 2L
            val bitB: Long = remB % 2L
            val combined: Long = when (op) {
                OP_OR -> if (bitA == 1L || bitB == 1L) 1L else 0L
                OP_AND -> if (bitA == 1L && bitB == 1L) 1L else 0L
                OP_XOR -> if (bitA != bitB) 1L else 0L
                else -> 0L
            }
            if (combined == 1L) result += powerOf2
            remA /= 2L
            remB /= 2L
            powerOf2 *= 2L
        }

        // Reinject sign bit at position 63.
        return if (resSign == 1L) result + SIGN_BIT else result
    }
}

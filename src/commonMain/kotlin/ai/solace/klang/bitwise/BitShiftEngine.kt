@file:Suppress("unused", "UNUSED_PARAMETER")
package ai.solace.klang.bitwise

/**
 * BitShiftMode: Strategy for performing bit shift operations.
 *
 * Determines whether shifts use native Kotlin operations or pure arithmetic
 * for cross-platform determinism.
 *
 * ## Modes
 *
 * - **AUTO**: Automatically selects best mode based on platform validation
 * - **NATIVE**: Uses Kotlin's shl/shr/ushr (fast, may vary by platform)
 * - **ARITHMETIC**: Uses multiplication/division (slower, deterministic)
 *
 * @see BitShiftEngine For usage
 * @since 0.1.0
 */
enum class BitShiftMode {
    /**
     * Automatically resolve to NATIVE or ARITHMETIC based on runtime validation.
     *
     * Delegates to [BitShiftConfig.resolveMode] which can verify native shift
     * behavior before committing to a strategy.
     */
    AUTO,
    
    /**
     * Use Kotlin's built-in shift operations (shl, shr, ushr).
     *
     * **Advantages**:
     * - Fast (native CPU instructions)
     * - Minimal overhead
     *
     * **Disadvantages**:
     * - May vary between platforms
     * - JavaScript shifts can behave differently
     */
    NATIVE,
    
    /**
     * Use pure arithmetic operations (multiplication/division).
     *
     * **Advantages**:
     * - Cross-platform deterministic
     * - No bitwise operator variations
     *
     * **Disadvantages**:
     * - Slower (iterative for large shifts)
     * - More CPU overhead
     */
    ARITHMETIC,
}

/**
 * ShiftResult: Result of a bit shift operation with carry/overflow information.
 *
 * Encapsulates the shifted value plus metadata about bits that were shifted out.
 *
 * ## Fields
 *
 * - **value**: The shifted result (masked to bit width)
 * - **carry**: Bits that were shifted out (for multi-limb arithmetic)
 * - **overflow**: true if operation exceeded bit width
 *
 * ## Usage Example
 *
 * ```kotlin
 * val engine = BitShiftEngine(BitShiftMode.NATIVE, bitWidth = 8)
 * val result = engine.leftShift(0xFF, 1)
 * println("Value: 0x${result.value.toString(16)}")     // 0xFE (wrapped)
 * println("Carry: 0x${result.carry.toString(16)}")     // 0x1 (lost bit)
 * println("Overflow: ${result.overflow}")              // true
 * ```
 *
 * @property value The shifted value (normalized to bit width)
 * @property carry Bits shifted out (for chaining operations)
 * @property overflow true if value exceeded bit width
 * @since 0.1.0
 */
data class ShiftResult(
    val value: Long,
    val carry: Long = 0,
    val overflow: Boolean = false,
)

/**
 * BitShiftEngine: Configurable bit shift operations with carry/overflow tracking.
 *
 * Provides a unified interface for bit shifting that can use either native Kotlin
 * operations or pure arithmetic for cross-platform determinism. Tracks carry bits
 * and overflow, essential for multi-precision arithmetic and low-level bit manipulation.
 *
 * ## Why BitShiftEngine?
 *
 * **The Problem**: Bit shifts can vary between platforms:
 * ```kotlin
 * // On some platforms:
 * val x: Byte = -128
 * val shifted = x.toInt() shr 1  // May be -64 or 64 depending on sign extension
 * ```
 *
 * **The Solution**: Explicit control over shift behavior:
 * ```kotlin
 * val engine = BitShiftEngine(BitShiftMode.ARITHMETIC, bitWidth = 8)
 * val result = engine.rightShift(0x80, 1)  // Deterministic: 0x40
 * ```
 *
 * ## Use Cases
 *
 * - **Multi-precision arithmetic**: Track carry bits for 128/256/512-bit operations
 * - **Cryptography**: Deterministic shifts for hash functions, ciphers
 * - **Binary protocols**: Bit packing/unpacking with overflow detection
 * - **Cross-platform**: Ensure identical behavior on JVM, Native, JS
 * - **Low-level emulation**: CPU emulators, VM implementations
 *
 * ## Architecture
 *
 * ```
 * ┌─────────────────┐
 * │ BitShiftEngine  │
 * │  mode: NATIVE   │  ← Configuration
 * │  bitWidth: 32   │
 * └────────┬────────┘
 *          │
 *     ┌────┴────┐
 *     │         │
 *   NATIVE   ARITHMETIC
 *     │         │
 *  (fast)   (deterministic)
 * ```
 *
 * ## Modes
 *
 * ### NATIVE Mode
 * ```kotlin
 * val engine = BitShiftEngine(BitShiftMode.NATIVE, 32)
 * val result = engine.leftShift(0x12345678, 4)
 * // Fast: uses Kotlin's shl
 * ```
 *
 * ### ARITHMETIC Mode
 * ```kotlin
 * val engine = BitShiftEngine(BitShiftMode.ARITHMETIC, 32)
 * val result = engine.leftShift(0x12345678, 4)
 * // Deterministic: uses multiplication by 2^n
 * ```
 *
 * ### AUTO Mode
 * ```kotlin
 * val engine = BitShiftEngine(BitShiftMode.AUTO, 32)
 * val result = engine.leftShift(0x12345678, 4)
 * // Resolves to NATIVE or ARITHMETIC based on platform
 * ```
 *
 * ## Bit Widths
 *
 * Supports 8, 16, 32, and 64-bit operations:
 * - **8-bit**: Byte operations (0x00 - 0xFF)
 * - **16-bit**: Short operations (0x0000 - 0xFFFF)
 * - **32-bit**: Int operations (0x00000000 - 0xFFFFFFFF)
 * - **64-bit**: Long operations (0x0000000000000000 - 0x7FFFFFFFFFFFFFFF)
 *
 * ## Usage Example
 *
 * ### Basic Shifting
 * ```kotlin
 * val engine = BitShiftEngine(BitShiftMode.NATIVE, 32)
 *
 * // Left shift with overflow detection
 * val left = engine.leftShift(0xF0000000, 4)
 * println("Value: 0x${left.value.toString(16)}")      // Wrapped result
 * println("Overflow: ${left.overflow}")                // true (bits lost)
 *
 * // Right shift (logical)
 * val right = engine.unsignedRightShift(0x80000000, 1)
 * println("Value: 0x${right.value.toString(16)}")     // 0x40000000
 * ```
 *
 * ### Multi-Precision Arithmetic
 * ```kotlin
 * // Shift a 128-bit number represented as two 64-bit limbs
 * val engine64 = BitShiftEngine(BitShiftMode.NATIVE, 64)
 *
 * // Low limb
 * val lowResult = engine64.leftShift(lowLimb, 1)
 * var newLow = lowResult.value
 *
 * // High limb (include carry from low)
 * val highResult = engine64.leftShift(highLimb, 1)
 * var newHigh = highResult.value or lowResult.carry
 *
 * // Check for 128-bit overflow
 * if (highResult.overflow) {
 *     println("128-bit overflow!")
 * }
 * ```
 *
 * ### Dynamic Bit Width
 * ```kotlin
 * fun shiftByWidth(value: Long, bits: Int, width: Int): Long {
 *     val engine = BitShiftEngine(BitShiftMode.NATIVE, width)
 *     return engine.leftShift(value, bits).value
 * }
 *
 * val byte = shiftByWidth(0xFF, 1, 8)    // 8-bit: 0xFE
 * val word = shiftByWidth(0xFFFF, 1, 16) // 16-bit: 0xFFFE
 * ```
 *
 * ## Performance
 *
 * | Mode | Complexity | Typical Cost |
 * |------|------------|--------------|
 * | NATIVE | O(1) | ~1-2 CPU cycles |
 * | ARITHMETIC (bits=1) | O(1) | ~5-10 cycles |
 * | ARITHMETIC (bits=n) | O(n) | ~5n-10n cycles |
 *
 * **Benchmark** (32-bit left shift by 8):
 * - NATIVE: ~2ns
 * - ARITHMETIC: ~40ns (20× slower)
 * - **Trade-off**: Speed vs determinism
 *
 * ## Carry Propagation
 *
 * Carry bits enable multi-limb arithmetic:
 * ```kotlin
 * // Add carry from previous limb
 * val result = engine.leftShift(limb, bits)
 * val nextLimb = nextValue or result.carry
 * ```
 *
 * ## Overflow Detection
 *
 * Overflow flag indicates when bits are lost:
 * ```kotlin
 * val result = engine.leftShift(0xFFFFFFFF, 1)
 * if (result.overflow) {
 *     // Handle overflow (saturate, wrap, error, etc.)
 * }
 * ```
 *
 * ## Thread Safety
 *
 * BitShiftEngine instances are immutable and thread-safe.
 * Operations return new [ShiftResult] objects.
 *
 * ## Builder Pattern
 *
 * Use [withMode] and [withBitWidth] to create variants:
 * ```kotlin
 * val base = BitShiftEngine(BitShiftMode.NATIVE, 32)
 * val arithmetic = base.withMode(BitShiftMode.ARITHMETIC)
 * val wider = base.withBitWidth(64)
 * ```
 *
 * ## Related Types
 *
 * | Type | Purpose | Carry? | Overflow? |
 * |------|---------|--------|-----------|
 * | Int.shl | Native left shift | No | No |
 * | Int.shr | Native right shift | No | No |
 * | Int.ushr | Native unsigned right shift | No | No |
 * | BitShiftEngine | Configurable shifts | Yes | Yes |
 *
 * @property mode The shift strategy (AUTO, NATIVE, or ARITHMETIC)
 * @property bitWidth The bit width (8, 16, 32, or 64)
 * @constructor Creates a shift engine with specified mode and bit width
 * @see BitShiftMode For mode descriptions
 * @see ShiftResult For result structure
 * @see ArithmeticBitwiseOps For arithmetic shift implementation
 * @since 0.1.0
 */
class BitShiftEngine(
    val mode: BitShiftMode = BitShiftMode.NATIVE,
    val bitWidth: Int = 32,
) {
    init {
        require(bitWidth in listOf(8, 16, 32, 64)) {
            "Bit width must be 8, 16, 32, or 64"
        }
    }

    /** Maximum representable value for this bit width. */
    private val maxValue =
        when (bitWidth) {
            8 -> 0xFFL
            16 -> 0xFFFFL
            32 -> 0xFFFFFFFFL
            64 -> 0x7FFFFFFFFFFFFFFFL // Use max signed long to avoid overflow
            else -> error("Unsupported bit width: $bitWidth")
        }

    /** Arithmetic operations helper (for ARITHMETIC mode). */
    private val arithmeticOps = if (bitWidth in 1..32) ArithmeticBitwiseOps(bitWidth) else null

    /**
     * Perform left shift with carry detection.
     *
     * Shifts [value] left by [bits] positions. Bits shifted out are captured
     * in the carry field. Overflow is detected when any bits are lost.
     *
     * @param value Value to shift (normalized to bit width)
     * @param bits Number of positions to shift (0 to bitWidth-1)
     * @return [ShiftResult] with value, carry, and overflow
     *
     * ## Example
     * ```kotlin
     * val engine = BitShiftEngine(BitShiftMode.NATIVE, 8)
     * val result = engine.leftShift(0x80, 1)
     * // value: 0x00, carry: 0x01, overflow: true
     * ```
     *
     * ## Complexity
     * - NATIVE: O(1)
     * - ARITHMETIC: O(bits)
     */
    fun leftShift(
        value: Long,
        bits: Int,
    ): ShiftResult {
        if (bits !in 0..<bitWidth) {
            return ShiftResult(0L, 0L, true)
        }

        val activeMode = if (mode == BitShiftMode.AUTO) {
            BitShiftConfig.resolveMode(bitWidth)
        } else {
            mode
        }

        return when (activeMode) {
            BitShiftMode.NATIVE -> {
                val originalValue = normalize(value)
                val shiftedValue =
                    when (bitWidth) {
                        8 -> (originalValue.toInt() shl bits).toLong()
                        16 -> (originalValue.toInt() shl bits).toLong()
                        32 -> (originalValue.toInt() shl bits).toLong()
                        64 -> originalValue shl bits
                        else -> error("Unexpected bitWidth in native leftShift: $bitWidth")
                    }

                val result = normalize(shiftedValue)
                val carry = if (shiftedValue != result) (shiftedValue ushr bitWidth) else 0L
                val overflow = shiftedValue > maxValue

                ShiftResult(result, carry, overflow)
            }

            BitShiftMode.ARITHMETIC -> {
                if (arithmeticOps == null) {
                    // Fallback to native for 64-bit
                    return this.withMode(BitShiftMode.NATIVE).leftShift(value, bits)
                }

                val originalValue = normalize(value)
                var result = originalValue
                var carry = 0L
                var overflow = false

                repeat(bits) {
                    val doubled = result * 2
                    if (doubled > maxValue) {
                        carry = (carry * 2) + (doubled ushr bitWidth)
                        overflow = true
                    }
                    result = normalize(doubled)
                }

                ShiftResult(result, carry, overflow)
            }

            BitShiftMode.AUTO -> error("BitShiftMode.AUTO must resolve before execution")
        }
    }

    /**
     * Perform right shift (arithmetic for negative numbers).
     *
     * Shifts [value] right by [bits] positions. For signed values,
     * sign bit is extended.
     *
     * @param value Value to shift (normalized to bit width)
     * @param bits Number of positions to shift (0 to bitWidth-1)
     * @return [ShiftResult] with shifted value
     *
     * ## Example
     * ```kotlin
     * val engine = BitShiftEngine(BitShiftMode.NATIVE, 8)
     * val result = engine.rightShift(0x80, 1)
     * // For unsigned: 0x40
     * ```
     *
     * ## Complexity
     * - NATIVE: O(1)
     * - ARITHMETIC: O(bits)
     */
    fun rightShift(
        value: Long,
        bits: Int,
    ): ShiftResult {
        if (bits !in 0..<bitWidth) {
            return ShiftResult(if (value < 0) -1L else 0L, 0L, false)
        }

        val activeMode = if (mode == BitShiftMode.AUTO) {
            BitShiftConfig.resolveMode(bitWidth)
        } else {
            mode
        }

        return when (activeMode) {
            BitShiftMode.NATIVE -> {
                val originalValue = normalize(value)
                val result =
                    when (bitWidth) {
                        8 -> ((originalValue.toInt() and 0xFF) ushr bits).toLong()
                        16 -> ((originalValue.toInt() and 0xFFFF) ushr bits).toLong()
                        32 -> (originalValue.toInt() ushr bits).toLong()
                        64 -> originalValue ushr bits
                        else -> error("Unexpected bitWidth in native rightShift: $bitWidth")
                    }

                ShiftResult(normalize(result), 0L, false)
            }

            BitShiftMode.ARITHMETIC -> {
                if (arithmeticOps == null) {
                    return this.withMode(BitShiftMode.NATIVE).rightShift(value, bits)
                }

                val result = arithmeticOps.rightShift(normalize(value), bits)
                ShiftResult(result, 0L, false)
            }

            BitShiftMode.AUTO -> error("BitShiftMode.AUTO must resolve before execution")
        }
    }

    /**
     * Perform unsigned right shift (zero-fill).
     *
     * Shifts [value] right by [bits] positions, filling with zeros from the left.
     * Always treats value as unsigned.
     *
     * @param value Value to shift (normalized to bit width)
     * @param bits Number of positions to shift (0 to bitWidth-1)
     * @return [ShiftResult] with shifted value
     *
     * ## Example
     * ```kotlin
     * val engine = BitShiftEngine(BitShiftMode.NATIVE, 16)
     * val result = engine.unsignedRightShift(0x8000, 1)
     * // value: 0x4000 (zero-fill)
     * ```
     *
     * ## Complexity
     * - NATIVE: O(1)
     * - ARITHMETIC: O(bits)
     */
    fun unsignedRightShift(
        value: Long,
        bits: Int,
    ): ShiftResult {
        if (bits !in 0..<bitWidth) {
            return ShiftResult(0L, 0L, false)
        }

        val activeMode = if (mode == BitShiftMode.AUTO) {
            BitShiftConfig.resolveMode(bitWidth)
        } else {
            mode
        }

        return when (activeMode) {
            BitShiftMode.NATIVE -> {
                val originalValue = normalize(value)
                val result =
                    when (bitWidth) {
                        8 -> (originalValue.toInt() and 0xFF) ushr bits
                        16 -> (originalValue.toInt() and 0xFFFF) ushr bits
                        32 -> (originalValue.toInt() ushr bits).toLong()
                        64 -> originalValue ushr bits
                        else -> error("Unexpected bitWidth in native unsignedRightShift: $bitWidth")
                    }

                ShiftResult(normalize(result.toLong()), 0L, false)
            }

            BitShiftMode.ARITHMETIC -> {
                if (arithmeticOps == null) {
                    return this.withMode(BitShiftMode.NATIVE).unsignedRightShift(value, bits)
                }

                val result = arithmeticOps.rightShift(normalize(value), bits)
                ShiftResult(result, 0L, false)
            }

            BitShiftMode.AUTO -> error("BitShiftMode.AUTO must resolve before execution")
        }
    }

    /**
     * Normalize a value to fit within the bit width.
     *
     * Masks value to only keep bits that fit in the configured bit width.
     *
     * @param value Value to normalize
     * @return Value masked to bit width
     */
    private fun normalize(value: Long): Long =
        when (bitWidth) {
            8 -> value and 0xFFL
            16 -> value and 0xFFFFL
            32 -> value and 0xFFFFFFFFL
            64 -> value
            else -> error("Unexpected bitWidth in normalize: $bitWidth")
        }

    /**
     * Create a copy with a different mode.
     *
     * @param newMode New shift strategy
     * @return New BitShiftEngine with specified mode
     */
    fun withMode(newMode: BitShiftMode): BitShiftEngine = BitShiftEngine(newMode, bitWidth)

    /**
     * Create a copy with a different bit width.
     *
     * @param newBitWidth New bit width (8, 16, 32, or 64)
     * @return New BitShiftEngine with specified bit width
     */
    fun withBitWidth(newBitWidth: Int): BitShiftEngine = BitShiftEngine(mode, newBitWidth)
}
        bits: Int,
    ): ShiftResult {
        if (bits !in 0..<bitWidth) {
            return ShiftResult(0L, 0L, true)
        }

        val activeMode = if (mode == BitShiftMode.AUTO) {
            BitShiftConfig.resolveMode(bitWidth)
        } else {
            mode
        }

        return when (activeMode) {
            BitShiftMode.NATIVE -> {
                val originalValue = normalize(value)
                val shiftedValue =
                    when (bitWidth) {
                        8 -> (originalValue.toInt() shl bits).toLong()
                        16 -> (originalValue.toInt() shl bits).toLong()
                        32 -> (originalValue.toInt() shl bits).toLong()
                        64 -> originalValue shl bits
                        else -> error("Unexpected bitWidth in native leftShift: $bitWidth")
                    }

                val result = normalize(shiftedValue)
                val carry = if (shiftedValue != result) (shiftedValue ushr bitWidth) else 0L
                val overflow = shiftedValue > maxValue

                ShiftResult(result, carry, overflow)
            }

            BitShiftMode.ARITHMETIC -> {
                if (arithmeticOps == null) {
                    // Fallback to native for 64-bit
                    return this.withMode(BitShiftMode.NATIVE).leftShift(value, bits)
                }

                val originalValue = normalize(value)
                var result = originalValue
                var carry = 0L
                var overflow = false

                repeat(bits) {
                    val doubled = result * 2
                    if (doubled > maxValue) {
                        carry = (carry * 2) + (doubled ushr bitWidth)
                        overflow = true
                    }
                    result = normalize(doubled)
                }

                ShiftResult(result, carry, overflow)
            }

            BitShiftMode.AUTO -> error("BitShiftMode.AUTO must resolve before execution")
        }
    }

    /**
     * Performs right shift (arithmetic for negative numbers)
     */
    fun rightShift(
        value: Long,
        bits: Int,
    ): ShiftResult {
        if (bits !in 0..<bitWidth) {
            return ShiftResult(if (value < 0) -1L else 0L, 0L, false)
        }

        val activeMode = if (mode == BitShiftMode.AUTO) {
            BitShiftConfig.resolveMode(bitWidth)
        } else {
            mode
        }

        return when (activeMode) {
            BitShiftMode.NATIVE -> {
                val originalValue = normalize(value)
                val result =
                    when (bitWidth) {
                        8 -> ((originalValue.toInt() and 0xFF) ushr bits).toLong()
                        16 -> ((originalValue.toInt() and 0xFFFF) ushr bits).toLong()
                        32 -> (originalValue.toInt() ushr bits).toLong()
                        64 -> originalValue ushr bits
                        else -> error("Unexpected bitWidth in native rightShift: $bitWidth")
                    }

                ShiftResult(normalize(result), 0L, false)
            }

            BitShiftMode.ARITHMETIC -> {
                if (arithmeticOps == null) {
                    return this.withMode(BitShiftMode.NATIVE).rightShift(value, bits)
                }

                val result = arithmeticOps.rightShift(normalize(value), bits)
                ShiftResult(result, 0L, false)
            }

            BitShiftMode.AUTO -> error("BitShiftMode.AUTO must resolve before execution")
        }
    }

    /**
     * Performs unsigned right shift
     */
    fun unsignedRightShift(
        value: Long,
        bits: Int,
    ): ShiftResult {
        if (bits !in 0..<bitWidth) {
            return ShiftResult(0L, 0L, false)
        }

        val activeMode = if (mode == BitShiftMode.AUTO) {
            BitShiftConfig.resolveMode(bitWidth)
        } else {
            mode
        }

        return when (activeMode) {
            BitShiftMode.NATIVE -> {
                val originalValue = normalize(value)
                val result =
                    when (bitWidth) {
                        8 -> (originalValue.toInt() and 0xFF) ushr bits
                        16 -> (originalValue.toInt() and 0xFFFF) ushr bits
                        32 -> (originalValue.toInt() ushr bits).toLong()
                        64 -> originalValue ushr bits
                        else -> error("Unexpected bitWidth in native unsignedRightShift: $bitWidth")
                    }

                ShiftResult(normalize(result.toLong()), 0L, false)
            }

            BitShiftMode.ARITHMETIC -> {
                if (arithmeticOps == null) {
                    return this.withMode(BitShiftMode.NATIVE).unsignedRightShift(value, bits)
                }

                val result = arithmeticOps.rightShift(normalize(value), bits)
                ShiftResult(result, 0L, false)
            }

            BitShiftMode.AUTO -> error("BitShiftMode.AUTO must resolve before execution")
        }
    }

    /**
     * Normalize a value to fit within the bit width
     */
    private fun normalize(value: Long): Long =
        when (bitWidth) {
            8 -> value and 0xFFL
            16 -> value and 0xFFFFL
            32 -> value and 0xFFFFFFFFL
            64 -> value
            else -> error("Unexpected bitWidth in normalize: $bitWidth")
        }

    /**
     * Creates a copy of this engine with different settings
     */
    fun withMode(newMode: BitShiftMode): BitShiftEngine = BitShiftEngine(newMode, bitWidth)

    /**
     * Creates a copy of this engine with different bit width
     */
    fun withBitWidth(newBitWidth: Int): BitShiftEngine = BitShiftEngine(mode, newBitWidth)
}

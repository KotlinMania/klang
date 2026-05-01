package ai.solace.klang.bitwise

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [ArithmeticBitwiseOps64].
 *
 * These tests validate the pure-arithmetic 64-bit bitwise primitives by
 * cross-checking each operation against Kotlin's native operators on the
 * exact same Long inputs. Native operators are used here only as the
 * test oracle — the production code under test does not use them.
 *
 * Coverage focus:
 * - The full unsigned range, including sign-bit (bit-63) inputs.
 * - Boundary shift amounts (0, 1, 31, 32, 63, out-of-range).
 * - All-zero and all-one inputs.
 */
class ArithmeticBitwiseOps64Test {

    private val ops = ArithmeticBitwiseOps64

    // A representative spread of Long values that exercises the sign bit,
    // mid-range, low/high halves, and all-ones / all-zeros.
    private val samples: List<Long> = listOf(
        0L,
        1L,
        2L,
        0xFFL,
        0xFFFFL,
        0xFFFFFFFFL,
        0x123456789ABCDEF0L,
        0x0FEDCBA987654321L,
        Long.MAX_VALUE,           // 0x7FFF_FFFF_FFFF_FFFF
        Long.MIN_VALUE,           // 0x8000_0000_0000_0000 (only sign bit set)
        -1L,                      // 0xFFFF_FFFF_FFFF_FFFF (all ones)
        -2L,
        -0x123456789ABCDEF0L,
    )

    // ----- normalize -----

    @Test
    fun normalizeIsIdentityForAllSamples() {
        for (v in samples) {
            assertEquals(v, ops.normalize(v), "normalize($v)")
        }
    }

    // ----- not -----

    @Test
    fun notMatchesNativeInv() {
        for (v in samples) {
            assertEquals(v.inv(), ops.not(v), "not($v)")
        }
    }

    // ----- leftShift -----

    @Test
    fun leftShiftMatchesNativeAcrossSamples() {
        for (v in samples) {
            for (n in 0..63) {
                assertEquals(
                    v shl n,
                    ops.leftShift(v, n),
                    "leftShift($v, $n)",
                )
            }
        }
    }

    @Test
    fun leftShiftOutOfRangeReturnsZero() {
        assertEquals(0L, ops.leftShift(0x12345L, -1))
        assertEquals(0L, ops.leftShift(0x12345L, 64))
        assertEquals(0L, ops.leftShift(0x12345L, 100))
    }

    @Test
    fun leftShiftSignBitBoundary() {
        // Shifting 1 by 63 should produce Long.MIN_VALUE (== 2^63 bit pattern).
        assertEquals(Long.MIN_VALUE, ops.leftShift(1L, 63))
        // Even values shifted by 63 lose the bottom bit and produce 0.
        assertEquals(0L, ops.leftShift(2L, 63))
        // Odd values shifted by 63 keep the sign bit only.
        assertEquals(Long.MIN_VALUE, ops.leftShift(3L, 63))
    }

    // ----- unsignedRightShift -----

    @Test
    fun unsignedRightShiftMatchesNativeAcrossSamples() {
        for (v in samples) {
            for (n in 0..63) {
                assertEquals(
                    v ushr n,
                    ops.unsignedRightShift(v, n),
                    "unsignedRightShift($v, $n)",
                )
            }
        }
    }

    @Test
    fun unsignedRightShiftOutOfRangeReturnsZero() {
        assertEquals(0L, ops.unsignedRightShift(-1L, -1))
        assertEquals(0L, ops.unsignedRightShift(-1L, 64))
        assertEquals(0L, ops.unsignedRightShift(-1L, 999))
    }

    @Test
    fun unsignedRightShiftOfNegativeOne() {
        // -1L is all-ones; ushr by any 0..63 should yield 2^(64-n) - 1.
        assertEquals(-1L, ops.unsignedRightShift(-1L, 0))
        assertEquals(Long.MAX_VALUE, ops.unsignedRightShift(-1L, 1))
        assertEquals(0xFFFFFFFFL, ops.unsignedRightShift(-1L, 32))
        assertEquals(1L, ops.unsignedRightShift(-1L, 63))
    }

    // ----- or / and / xor: cross-check against native -----

    @Test
    fun orMatchesNativeForSamplePairs() {
        for (a in samples) for (b in samples) {
            assertEquals(a or b, ops.or(a, b), "or($a, $b)")
        }
    }

    @Test
    fun andMatchesNativeForSamplePairs() {
        for (a in samples) for (b in samples) {
            assertEquals(a and b, ops.and(a, b), "and($a, $b)")
        }
    }

    @Test
    fun xorMatchesNativeForSamplePairs() {
        for (a in samples) for (b in samples) {
            assertEquals(a xor b, ops.xor(a, b), "xor($a, $b)")
        }
    }

    // ----- Specific bit-pattern cases the Runtime.kt refactor depends on -----

    @Test
    fun maskZeroExtendByteAndShort() {
        // What `lbu` / `lhu` need: zero-extend a byte/short value into a Long.
        val byteVal: Long = (-1).toByte().toLong()  // -1L (sign-extended)
        // Mask with 0xFF (8-bit zero-extend) ↔ ops.and(byteVal, 0xFFL)
        assertEquals(0xFFL, ops.and(byteVal, 0xFFL))

        val shortVal: Long = (-1).toShort().toLong() // -1L
        assertEquals(0xFFFFL, ops.and(shortVal, 0xFFFFL))
    }

    @Test
    fun maskZeroExtend32BitIntoLong() {
        // `lwu` needs: low 32 bits of a Long, zero-extended.
        val v = -1L
        val mask32: Long = 0xFFFFFFFFL
        assertEquals(0xFFFFFFFFL, ops.and(v, mask32))

        val mixed = 0xDEADBEEF_CAFEBABEUL.toLong()
        assertEquals(0xCAFEBABEL, ops.and(mixed, mask32))
    }

    @Test
    fun assembleLongFromTwoUInt32Halves() {
        // `ld` needs: combine lo (bits 0..31) and hi (bits 32..63) into a Long.
        val lo: Long = 0xCAFEBABEL
        val hi: Long = 0xDEADBEEFL
        val expected: Long = (hi shl 32) or lo
        val got: Long = ops.or(ops.leftShift(hi, 32), lo)
        assertEquals(expected, got)
    }
}

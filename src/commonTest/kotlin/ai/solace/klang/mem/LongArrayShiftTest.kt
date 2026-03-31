package ai.solace.klang.mem

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test whether Long shr/shl preserves unsigned semantics without sign extension.
 *
 * Key question: When we extract a "byte" from a Long using shifts,
 * do we get clean unsigned values or sign-extended garbage?
 */
class LongArrayShiftTest {

    @Test
    fun testLongShiftNoSignExtension() {
        println("\n" + "=".repeat(70))
        println("LONG SHIFT TEST: Does ushr avoid sign extension?")
        println("=".repeat(70))

        // Pack 0x80 (high bit set) into different positions of a Long
        val packed: Long = 0x80_80_80_80_80_80_80_80UL.toLong()

        println("Packed Long: 0x${packed.toULong().toString(16)}")
        println()

        // Extract each "byte" using ushr
        for (bytePos in 0 until 8) {
            val shift = bytePos * 8
            val extracted = ((packed ushr shift) and 0xFF).toInt()

            println("Byte $bytePos (shift $shift): extracted = $extracted (0x${extracted.toString(16)})")
            assertEquals(0x80, extracted, "Byte $bytePos should be 0x80, not sign-extended")
        }

        println()
        println("SUCCESS: Long ushr preserves unsigned byte values!")
    }

    @Test
    fun testLongVsByteComparison() {
        println("\n" + "=".repeat(70))
        println("COMPARISON: Long extraction vs Byte.toInt()")
        println("=".repeat(70))

        // The problematic Byte approach
        val byteVal: Byte = 0x80.toByte()
        val fromByte = byteVal.toInt()
        val fromByteMasked = byteVal.toInt() and 0xFF

        println("Byte approach:")
        println("  0x80.toByte() = $byteVal")
        println("  .toInt() = $fromByte (0x${fromByte.toUInt().toString(16)}) <- SIGN EXTENDED!")
        println("  .toInt() and 0xFF = $fromByteMasked (0x${fromByteMasked.toString(16)}) <- requires mask")
        println()

        // The Long approach
        val longVal: Long = 0x80L
        val fromLong = ((longVal) and 0xFF).toInt()

        println("Long approach:")
        println("  0x80L = $longVal")
        println("  (longVal and 0xFF).toInt() = $fromLong (0x${fromLong.toString(16)}) <- clean!")
        println()

        // Now test extraction from a packed Long
        val packed: Long = 0x00_00_00_00_00_00_00_80L
        val extractedByte0 = ((packed ushr 0) and 0xFF).toInt()

        println("Packed extraction:")
        println("  packed = 0x${packed.toULong().toString(16)}")
        println("  ((packed ushr 0) and 0xFF).toInt() = $extractedByte0 (0x${extractedByte0.toString(16)})")

        assertEquals(0x80, extractedByte0, "Should extract 0x80 cleanly")
    }

    @Test
    fun testPackedBufferOperations() {
        println("\n" + "=".repeat(70))
        println("PACKED BUFFER: LongArray-backed byte operations")
        println("=".repeat(70))

        // Simulate a PackedBuffer
        val data = LongArray(2) // 16 bytes

        // Write bytes at various positions
        fun setByte(addr: Int, value: Int) {
            val idx = addr ushr 3  // addr / 8
            val shift = (addr and 7) shl 3  // (addr % 8) * 8
            val mask = (0xFFL shl shift).inv()
            data[idx] = (data[idx] and mask) or ((value.toLong() and 0xFF) shl shift)
        }

        fun getByte(addr: Int): Int {
            val idx = addr ushr 3
            val shift = (addr and 7) shl 3
            return ((data[idx] ushr shift) and 0xFF).toInt()
        }

        // Write 0x80 to each byte position
        for (i in 0 until 16) {
            setByte(i, 0x80)
        }

        println("After writing 0x80 to all 16 byte positions:")
        println("  data[0] = 0x${data[0].toULong().toString(16)}")
        println("  data[1] = 0x${data[1].toULong().toString(16)}")
        println()

        // Read back and verify
        var allCorrect = true
        for (i in 0 until 16) {
            val readBack = getByte(i)
            if (readBack != 0x80) {
                println("  ERROR at byte $i: got $readBack, expected 0x80")
                allCorrect = false
            }
        }

        if (allCorrect) {
            println("SUCCESS: All 16 bytes read back as 0x80 - no sign extension!")
        }

        // Test word (32-bit) operations
        println()
        println("Testing 32-bit word operations:")

        fun setInt(addr: Int, value: Int) {
            // addr must be 4-byte aligned for simplicity
            val idx = addr ushr 3
            val shift = (addr and 4) shl 3  // 0 or 32
            val mask = (0xFFFFFFFFL shl shift).inv()
            data[idx] = (data[idx] and mask) or ((value.toLong() and 0xFFFFFFFFL) shl shift)
        }

        fun getInt(addr: Int): Int {
            val idx = addr ushr 3
            val shift = (addr and 4) shl 3
            return ((data[idx] ushr shift) and 0xFFFFFFFFL).toInt()
        }

        // Clear and test
        data[0] = 0L
        data[1] = 0L

        setInt(0, 0xDEADBEEF.toInt())
        setInt(4, 0xCAFEBABE.toInt())
        setInt(8, 0x12345678)
        setInt(12, 0x87654321.toInt())

        println("  setInt(0, 0xDEADBEEF) -> getInt(0) = 0x${getInt(0).toUInt().toString(16)}")
        println("  setInt(4, 0xCAFEBABE) -> getInt(4) = 0x${getInt(4).toUInt().toString(16)}")
        println("  setInt(8, 0x12345678) -> getInt(8) = 0x${getInt(8).toUInt().toString(16)}")
        println("  setInt(12, 0x87654321) -> getInt(12) = 0x${getInt(12).toUInt().toString(16)}")

        assertEquals(0xDEADBEEF.toInt(), getInt(0))
        assertEquals(0xCAFEBABE.toInt(), getInt(4))
        assertEquals(0x12345678, getInt(8))
        assertEquals(0x87654321.toInt(), getInt(12))

        println()
        println("SUCCESS: 32-bit word operations work correctly!")
    }

    @Test
    fun testLongOperationsStayAsLong() {
        println("\n" + "=".repeat(70))
        println("TYPE VERIFICATION: Do Long operations stay as Long?")
        println("=".repeat(70))

        val a: Long = 0x8000000000000000UL.toLong()  // High bit set

        println("a = 0x${a.toULong().toString(16)} (Long.MIN_VALUE)")

        val shifted = a ushr 56
        println("a ushr 56 = $shifted (type: Long, value should be 0x80 = 128)")

        val masked = shifted and 0xFF
        println("(a ushr 56) and 0xFF = $masked")

        val asInt = masked.toInt()
        println("masked.toInt() = $asInt")

        assertEquals(0x80, asInt, "Should be 128 (0x80), not negative")

        println()
        println("CONFIRMED: Long ushr returns Long, and 0xFF mask preserves unsigned semantics")
    }
}

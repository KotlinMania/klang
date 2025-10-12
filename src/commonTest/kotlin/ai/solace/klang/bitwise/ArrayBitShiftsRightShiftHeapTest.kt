package ai.solace.klang.bitwise

import ai.solace.klang.mem.GlobalHeap
import ai.solace.klang.mem.KMalloc
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayBitShiftsRightShiftHeapTest {
    @Test
    fun rsh16HeapMatchesIntArrayRandomized() {
        KMalloc.init(1 shl 18)
        val rnd = Random(0xC0FFEE)
        repeat(10) {
            val len = 16 + rnd.nextInt(16)
            val a = IntArray(len) { rnd.nextInt() and 0xFFFF }
            val base = KMalloc.malloc(len * 2)
            for (i in 0 until len) {
                val v = a[i]
                GlobalHeap.sb(base + i * 2, (v and 0xFF).toByte())
                GlobalHeap.sb(base + i * 2 + 1, ((v ushr 8) and 0xFF).toByte())
            }
            val s = 1 + rnd.nextInt(15)
            val r1 = ArrayBitShifts.rsh16LEInPlace(a, 0, len, s)
            val r2 = ArrayBitShifts.rsh16LEInPlace(base, 0, len, s)
            assertEquals(r1.carryOut and 0xFFFF, r2.carryOut and 0xFFFF)
            // Compare arrays
            for (i in 0 until len) {
                val v = GlobalHeap.lbu(base + i * 2) or (GlobalHeap.lbu(base + i * 2 + 1) shl 8)
                assertEquals(a[i] and 0xFFFF, v and 0xFFFF)
            }
        }
    }
}


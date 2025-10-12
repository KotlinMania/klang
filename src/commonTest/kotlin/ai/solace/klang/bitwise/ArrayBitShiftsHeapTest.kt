package ai.solace.klang.bitwise

import ai.solace.klang.mem.GlobalHeap
import ai.solace.klang.mem.KMalloc
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayBitShiftsHeapTest {
    @Test
    fun shl16HeapMatchesIntArray() {
        KMalloc.init(1 shl 18)
        val len = 32
        val arr = IntArray(len) { (it * 17) and 0xFFFF }
        val base = KMalloc.malloc(len * 2 + 2)
        // write arr to heap
        for (i in 0 until len) {
            val v = arr[i]
            GlobalHeap.sb(base + i * 2, (v and 0xFF).toByte())
            GlobalHeap.sb(base + i * 2 + 1, ((v ushr 8) and 0xFF).toByte())
        }

        val s = 9
        val r1 = ArrayBitShifts.shl16LEInPlace(arr, 0, len, s, 0)
        val r2 = ArrayBitShifts.shl16LEInPlace(base, 0, len, s, 0)
        assertEquals(r1.carryOut and 0xFFFF, r2.carryOut and 0xFFFF)
        // read heap back
        val back = IntArray(len)
        for (i in 0 until len) {
            back[i] = GlobalHeap.lbu(base + i * 2) or (GlobalHeap.lbu(base + i * 2 + 1) shl 8)
        }
        for (i in 0 until len) assertEquals(arr[i] and 0xFFFF, back[i] and 0xFFFF)
    }
}


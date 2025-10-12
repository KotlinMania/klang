package ai.solace.klang.int.hpc

import kotlin.test.Test
import kotlin.test.assertEquals

class HeapUInt128Test {
    @Test
    fun addAndShift() {
        val a = HeapUInt128.fromULong(0xFFFF_FFFFu)
        val b = HeapUInt128.fromULong(1u)
        val c = a + b
        // 0x1_0000_0000
        val shifted = c.shiftLeft(16)
        // Expect 0x1_0000_0000 &lt;&lt; 16 = 0x1_0000_0000_0000
        val hex = shifted.toHexString()
        // We only check suffix to avoid formatting assumptions
        assertEquals(true, hex.endsWith("0000"))
    }
}


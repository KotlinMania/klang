package ai.solace.klang.mem

import ai.solace.klang.fp.CDouble

/** C-like scalar variables stored in the heap, in place. */
interface CVar { val addr: Int }

class CByteVar(override val addr: Int) : CVar {
    var value: Byte
        get() = GlobalHeap.lb(addr)
        set(v) = GlobalHeap.sb(addr, v)
}

class CShortVar(override val addr: Int) : CVar {
    var value: Short
        get() = GlobalHeap.lh(addr)
        set(v) = GlobalHeap.sh(addr, v)
}

class CIntVar(override val addr: Int) : CVar {
    var value: Int
        get() = GlobalHeap.lw(addr)
        set(v) = GlobalHeap.sw(addr, v)

    fun addAssign(x: Int) { GlobalHeap.sw(addr, GlobalHeap.lw(addr) + x) }
    fun subAssign(x: Int) { GlobalHeap.sw(addr, GlobalHeap.lw(addr) - x) }
}

class CLongVar(override val addr: Int) : CVar {
    var value: Long
        get() = GlobalHeap.ld(addr)
        set(v) = GlobalHeap.sd(addr, v)
}

class CFloatVar(override val addr: Int) : CVar {
    var value: Float
        get() = GlobalHeap.lwf(addr)
        set(v) = GlobalHeap.swf(addr, v)
}

class CDoubleVar(override val addr: Int) : CVar {
    var value: Double
        get() = GlobalHeap.ldf(addr)
        set(v) = GlobalHeap.sdf(addr, v)

    var cdouble: CDouble
        get() = CDouble.fromBits(GlobalHeap.ld(addr))
        set(v) = GlobalHeap.sd(addr, v.toBits())
}

/** Automatic (stack) storage helpers. */
object CAutos {
    fun byte(init: Byte = 0, align: Int = 1): CByteVar {
        val p = KStack.alloca(1, align)
        GlobalHeap.sb(p, init)
        return CByteVar(p)
    }
    fun short(init: Short = 0, align: Int = 2): CShortVar {
        val p = KStack.alloca(2, align)
        GlobalHeap.sh(p, init)
        return CShortVar(p)
    }
    fun int(init: Int = 0, align: Int = 4): CIntVar {
        val p = KStack.alloca(4, align)
        GlobalHeap.sw(p, init)
        return CIntVar(p)
    }
    fun long(init: Long = 0L, align: Int = 8): CLongVar {
        val p = KStack.alloca(8, align)
        GlobalHeap.sd(p, init)
        return CLongVar(p)
    }
    fun float(init: Float = 0f, align: Int = 4): CFloatVar {
        val p = KStack.alloca(4, align)
        GlobalHeap.swf(p, init)
        return CFloatVar(p)
    }
    fun double(init: Double = 0.0, align: Int = 8): CDoubleVar {
        val p = KStack.alloca(8, align)
        GlobalHeap.sdf(p, init)
        return CDoubleVar(p)
    }
}

/** Global/static storage helpers using GlobalData (DATA/BSS). */
object CGlobals {
    fun int(name: String, init: Int = 0, align: Int = 4): CIntVar = CIntVar(GlobalData.defineI32(name, init, align))
    fun long(name: String, init: Long = 0, align: Int = 8): CLongVar = CLongVar(GlobalData.defineI64(name, init, align))
    fun double(name: String, init: Double = 0.0, align: Int = 8): CDoubleVar = CDoubleVar(GlobalData.defineF64(name, init, align))
}

/** Heap (malloc) storage helpers. */
object CHeapVars {
    fun int(init: Int = 0, align: Int = 4): CIntVar {
        val p = KMalloc.malloc(4)
        // align>=4 guaranteed by KMalloc default alignment; align parameter reserved for future aligned_alloc
        GlobalHeap.sw(p, init); return CIntVar(p)
    }
    fun double(init: Double = 0.0): CDoubleVar {
        val p = KMalloc.malloc(8)
        GlobalHeap.sdf(p, init); return CDoubleVar(p)
    }
    fun free(v: CVar) { KMalloc.free(v.addr) }
}

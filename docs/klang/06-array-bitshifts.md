ArrayBitShifts — 16‑bit Limb Shifts
===================================

Concept
- Operate on IntArray of 16‑bit limbs (little‑endian) or on packed 16‑bit limbs directly on the heap.
- Three‑pass method for large arrays; scalar path for small; parallel coroutines for big inputs.

APIs
- shl16LEInPlace(a, from, len, s, carryIn = 0): Shift IntArray in place and return carryOut + sticky.
- shl16LEInPlace(baseAddr, fromLimb, len, s, carryIn = 0): Same, but operates on heap bytes (no copies across call boundaries).
- rsh16LEInPlace(...): analogous right shift with carry/sticky.

Engine Dependency
- All bit movement uses BitShiftEngine; mode is resolved via BitShiftConfig.

Guidance
- For persistent data structures, use the heap‑address overloads to avoid copying to/from arrays.


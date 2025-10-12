BitShiftEngine — Shifts with Intent
===================================

Purpose
- Provide a unified interface for shifts that can run either with native bitwise ops or arithmetic emulation.

Modes
- NATIVE: uses Kotlin shl/shr/ushr on normalized values.
- ARITHMETIC: shifts via multiply/divide/add (deterministic across targets).
- AUTO: probes native vs arithmetic for the bit width; selects one at startup.

Use
- BitShiftConfig.defaultMode controls the default (we set NATIVE by default for performance on macOS aarch64).
- ArrayBitShifts uses BitShiftEngine internally to shift 16‑bit limb arrays; APIs also provide heap‑address overloads.

Why both?
- Some legacy code (e.g., Mark Adler’s 8/16‑bit routines) assumed arithmetic formulations for determinism. We preserve that via ARITHMETIC.
- General code benefits from NATIVE performance.


KLang Walkthrough – Index
=========================

## Overview & Architecture
- 01-overview.md — goals, principles, layout
- 17-component-architecture.md — **NEW** Component interconnections and data flow

## Core Components

### Memory Management
- 02-global-heap.md — GlobalHeap model and typed IO
- 03-allocator-kmalloc.md — KMalloc design (bins, coalescing, splitting)
- 04-typed-io-and-views.md — loads/stores, U8/U16/U32 views
- 10-pointer-model.md — CPointer, extensions, usage

### Bitwise Operations
- 05-bitshift-engine.md — BitShiftEngine modes (NATIVE/ARITHMETIC/AUTO)
- 06-array-bitshifts.md — 16‑bit limb shifts (ArrayBitShifts)

### Integer Types
- 07-heap-uint128.md — HeapUInt128 representation and ops

### Floating Point
- 08-floating.md — CDouble, CLongDouble flavors, CFloat128

### String Operations
- 09-clib-strings.md — CLib, CString, FastMem/FastStringMem

## Development & Testing
- 16-testing-strategy.md — **NEW** Comprehensive test architecture (198 tests)
- 11-poc-benchmark.md — actor/zero‑copy/native shift benchmark
- 12-build-and-run.md — gradle tasks, targets, entry point

## Migration & Usage
- 15-from-c-to-klang.md — C→KLang mapping, in‑place rules, and status
- 13-porting-guide.md — mapping C constructs onto the heap
- 14-roadmap.md — near‑term work and defaults

## Key Achievements

✅ **198 tests** (100% pass rate on macOS ARM64)
✅ **Zero-copy operations** across all major components
✅ **Cross-platform determinism** (JS, Native)
✅ **Production-ready** memory management
✅ **IEEE-754 compliant** floating-point operations

# KLang Package Structure

## Overview

KLang follows a clear organizational structure that separates the public API surface from internal implementation details. This design enables:

- **API Stability**: Public types have stable interfaces that won't break between versions
- **Internal Flexibility**: Implementation details can evolve without affecting consumers
- **Clear Documentation**: Users know exactly what they should and shouldn't use
- **Maintainability**: Related functionality is logically grouped

## Package Organization

```
ai.solace.klang/
├── api/                    # PUBLIC API - Stable, documented type aliases
│   └── PublicAPI.kt       # Central point for all public types
│
├── bitwise/               # Bit manipulation and shift operations
│   ├── BitShiftEngine.kt  # Arithmetic-mode bit shifting
│   ├── CFloat32.kt        # 32-bit IEEE-754 float
│   ├── SwAR.kt           # SIMD Within A Register operations
│   └── ... (other bitwise utilities)
│
├── fp/                    # Floating point types
│   ├── CDouble.kt        # 64-bit double
│   ├── CFloat16.kt       # 16-bit half precision
│   ├── CBF16.kt          # Brain Float 16
│   ├── CFloat128.kt      # 128-bit quad precision
│   └── CLongDouble.kt    # Platform long double
│
├── int/                   # Extended precision integers
│   ├── C_UInt128.kt      # Unsigned 128-bit integer
│   ├── C_Int128.kt       # Signed 128-bit integer
│   └── hpc/              # High-performance computing variants
│       └── HeapUInt128.kt
│
├── mem/                   # Memory management
│   ├── GlobalArrayHeap.kt # Global heap allocator
│   ├── KMalloc.kt        # malloc/free implementation
│   ├── CLib.kt           # C standard library functions
│   ├── CString.kt        # Null-terminated strings
│   └── CScalars.kt       # Heap-backed scalar variables
│
├── common/               # Shared utilities
│   ├── StatOps.kt
│   └── StructLayout.kt
│
├── stringshift/          # String manipulation
│   └── HexShift.kt
│
├── internal/             # INTERNAL - May change without notice
│   ├── runtime/
│   │   └── Runtime.kt    # AbstractRuntime for transpiled C code
│   ├── symbols/
│   │   └── CLibSymbols.kt # C function symbols
│   ├── CStringExt.kt     # CPointer extensions for strings
│   └── PointerExtensions.kt # CPointer utility methods
│
└── KLangExports.kt       # Main library documentation
```

## Public API Usage

### Importing Types

Users should import from the `api` package for all public types:

```kotlin
import ai.solace.klang.api.CFloat32
import ai.solace.klang.api.C_UInt128
import ai.solace.klang.api.GlobalHeap
import ai.solace.klang.api.BitShiftEngine

// Use the types
val x = CFloat32.fromFloat(3.14f)
val y = C_UInt128.fromLongs(0, 1)
val ptr = GlobalHeap.mallocBytes(100)
```

### What's Public?

The `ai.solace.klang.api.PublicAPI` file defines all stable, public types:

**Floating Point**
- `CFloat32`, `CDouble`, `CFloat16`, `CBF16`, `CFloat128`, `CLongDouble`

**Integers**
- `C_UInt128`, `C_Int128`

**Memory Management**
- `CPointer<T>`, `GlobalHeap`, `KMalloc`, `CLib`, `CString`
- `CByteVar`, `CShortVar`, `CIntVar`, `CLongVar`, `CFloatVar`, `CDoubleVar`

**Bit Manipulation**
- `BitShiftEngine`, `BitShiftConfig`, `BitwiseOps`, `BitPrimitives`
- `Float32Math`, `Float64Math`

## Internal Implementation

### When to Use Internal Packages

Internal packages are for:
- Library implementation details
- Runtime support for transpiled C code
- Internal utilities and helpers
- Experimental or unstable features

### Accessing Internal APIs

If you must use internal APIs (not recommended):

```kotlin
import ai.solace.klang.internal.runtime.AbstractRuntime
import ai.solace.klang.internal.symbols.strdupCString
```

**Warning**: Internal APIs may change at any time without notice.

## Design Principles

### 1. Public API Stability

Types in `ai.solace.klang.api` follow semantic versioning:
- **Major version**: Breaking changes to public API
- **Minor version**: New features, backward compatible
- **Patch version**: Bug fixes only

### 2. Zero-Copy Operations

All operations work directly on the heap:
- No intermediate allocations
- Pointers are Int offsets into ByteArray
- Results written in-place when possible

### 3. C Compatibility

Types match C semantics exactly:
- Same bit patterns as C types
- Same arithmetic behavior (including overflow)
- Same memory layout for structs

### 4. Multiplatform Support

All code is pure Kotlin/Common:
- No platform-specific implementations
- Same behavior on JVM, JS, Native
- No native interop required

## Migration Guide

If you're using old code that imported directly from implementation packages:

### Before
```kotlin
import ai.solace.klang.bitwise.CFloat32
import ai.solace.klang.mem.GlobalArrayHeap
```

### After
```kotlin
import ai.solace.klang.api.CFloat32
import ai.solace.klang.api.GlobalHeap
```

## Adding New Public Types

When adding a new type to the public API:

1. Implement the type in the appropriate component package (`fp/`, `int/`, etc.)
2. Add a typealias in `api/PublicAPI.kt`
3. Document the type thoroughly with KDoc
4. Add usage examples
5. Update this document

## Performance Considerations

The package structure has zero runtime overhead:
- Type aliases are compile-time only
- No wrapper objects or delegation
- Direct function calls after inlining

## Questions?

See `docs/architecture/` for detailed component documentation.

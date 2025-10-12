# KLang

**A Kotlin multiplatform systems programming library that makes Kotlin behave like C** — providing deterministic, bit-exact low-level primitives across all platforms (JavaScript, Native) while remaining pure Kotlin.

## Why KLang?

KLang bridges the gap between Kotlin's high-level abstractions and C's low-level control, enabling:

- **Cross-platform determinism**: Identical behavior across JS and Native targets with explicit little-endian semantics
- **Pure Kotlin implementation**: No native dependencies, GMP, or glibc required — everything is reproducible Kotlin code
- **C compatibility**: Direct mapping of C memory models, pointer arithmetic, and type semantics
- **Performance**: Native-speed operations with optional arithmetic-mode for legacy compatibility

## Key Innovations

### 1. Single-Heap Memory Model
All C-style memory lives in a single `GlobalHeap` backed by a pure Kotlin `ByteArray`. Pointers are `Int` byte offsets, providing:
- **16-byte aligned allocator** (`KMalloc`) with coalescing, splitting, and size-class bins
- **Deterministic typed I/O**: All loads/stores are little-endian across platforms
- **Complete libc surface**: `malloc/calloc/realloc/free`, `strlen/strcmp/memcpy/memmove`, etc.

### 2. Intent-Driven Floating Point
Unlike C's platform-dependent `long double`, KLang provides explicit control:
- **`CDouble`**: IEEE-754 binary64 (exact on all targets)
- **`CLongDouble`**: Selectable flavors — `DOUBLE64`, `EXTENDED80`, or `IEEE128`
- **`CFloat128`**: Double-double arithmetic (~106-bit mantissa precision)

**Benchmark Results** (100M summations of 1e-8, expected: 1.0):
```
Simple double:       1.000000082740371 (error: 8.27e-08)  
Kahan compensated:   1.000000000000000 (error: 1.11e-16, 1.8x slower)
KLang double-double: 1.000000000000000 (error: 4.44e-31, 2.1x slower)
```

Double-double provides **~15 orders of magnitude** better precision than compensated summation while maintaining consistent behavior across all platforms.

### 3. BitShift Engine with Dual Modes
A unified shift interface supporting both performance and determinism:
- **NATIVE mode**: Fast path using Kotlin's native `shl/shr/ushr` operations
- **ARITHMETIC mode**: Pure multiply/divide/add for legacy 8/16-bit code requiring bit-exact compatibility
- **Array operations**: Efficient multi-limb shifts for arbitrary-precision arithmetic

### 4. 128-bit Integer Support
`HeapUInt128` provides full arithmetic and comparison operations with 8 little-endian 16-bit limbs stored directly in the heap — mirroring C's memory layout.

## Platform Support

- **JavaScript**: ES2015+ with ES modules (browser environment)
- **Native**: 
  - macOS (ARM64, x64)
  - Linux (x64, ARM64)  
  - Windows (x64 via MinGW)

All platforms provide **identical semantics** — no endianness leaks, no platform-specific floating-point quirks.

## Use Cases

- **Cross-platform numeric algorithms**: Implement once, get identical results everywhere
- **Legacy code porting**: Port C libraries to pure Kotlin with deterministic behavior
- **High-precision computing**: Extended precision without platform-specific dependencies
- **Systems programming in Kotlin**: Memory management, pointer arithmetic, bit manipulation
- **Cryptography**: Deterministic operations critical for security implementations

## Architecture Highlights

**Memory Management**
- `GlobalArrayHeap`: Single expandable heap with typed load/store operations
- `KMalloc`: Production-ready allocator with coalescing and fragmentation control
- `CPointer<T>`: Type-safe pointer abstraction over Int offsets

**Numerical Computing**
- `BitShiftEngine`: Configurable shift strategies (native vs arithmetic)
- `ArrayBitShifts`: Efficient multi-limb operations for arbitrary-precision math
- `HeapUInt128`: 128-bit unsigned integer with full arithmetic support
- `CFloat128`: Double-double precision (~31 decimal digits)

**C Library Compatibility**
- String operations: `strlen`, `strcmp`, `strcpy`, `strncpy`, `strchr`, `strstr`
- Memory operations: `memcpy`, `memmove`, `memset`, `memchr`, `memcmp`
- All operations use word-at-a-time loops for high throughput

## Documentation

Detailed documentation available in [`docs/klang/`](docs/klang/):
- [00-index.md](docs/klang/00-index.md) — Documentation overview
- [01-overview.md](docs/klang/01-overview.md) — Architecture and key concepts
- [02-global-heap.md](docs/klang/02-global-heap.md) — Memory model
- [03-allocator-kmalloc.md](docs/klang/03-allocator-kmalloc.md) — Allocator design
- [05-bitshift-engine.md](docs/klang/05-bitshift-engine.md) — Shift strategies
- [11-poc-benchmark.md](docs/klang/11-poc-benchmark.md) — Performance analysis

## Building

### Requirements

- JDK 11 or higher
- Gradle 8.x (wrapper included)

### Build Commands

```bash
# Build all targets
./gradlew build

# Build web/JS target
./gradlew jsWeb

# Build native executable (macOS ARM64)
./gradlew buildMac

# Run native executable
./gradlew run

# Build Docker image (Linux ARM64)
./gradlew buildDockerImage
```

### Testing

```bash
# Run all tests
./gradlew test

# Run JS tests only
./gradlew jsTest

# Run native tests (macOS ARM64)
./gradlew macosArm64Test
```

## Project Structure

```
src/
├── commonMain/kotlin/ai/solace/klang/
│   ├── mem/              # Memory management (GlobalHeap, KMalloc, CLib)
│   ├── bitwise/          # BitShiftEngine, array operations
│   ├── fp/               # Floating-point types (CDouble, CLongDouble, CFloat128)
│   ├── int/hpc/          # HeapUInt128 and arbitrary-precision integers
│   └── stringshift/      # String manipulation utilities
├── jsMain/              # JavaScript-specific implementations
└── nativeMain/          # Native platform implementations

tools/                   # C reference implementations for validation
docs/klang/             # Comprehensive documentation
```

## Examples

### Memory Allocation and Pointers
```kotlin
import ai.solace.klang.mem.*

// Initialize heap
GlobalHeap.init(4096)

// Allocate memory
val ptr = Runtime.kmalloc(256)
ptr.store(0x12345678)
val value = ptr.load()

// C-style string operations
val str = "Hello, KLang!".toCString()
val len = CLib.strlen(str)

Runtime.kfree(ptr)
```

### High-Precision Arithmetic
```kotlin
import ai.solace.klang.fp.CFloat128

// Double-double precision
val a = CFloat128(1.0 / 3.0)  // ~106-bit mantissa
val b = CFloat128(2.0 / 3.0)
val sum = a + b
// Result is accurate to ~31 decimal digits
```

### Arbitrary-Precision Shifts
```kotlin
import ai.solace.klang.bitwise.*

// Configure shift mode
BitShiftConfig.defaultMode = BitShiftMode.NATIVE

// Shift large limb arrays
val limbs = intArrayOf(0x1234, 0xABCD, 0x00FF, 0xC0DE)
val shifted = ArrayBitShifts.shl16LEInPlace(limbs, bits = 37)
```

## Performance

KLang is designed for production use with performance characteristics competitive with native C implementations:

- **Native mode shifts**: Near-zero overhead vs Kotlin's built-in operations
- **Allocator**: O(1) small allocations via size-class bins, efficient coalescing
- **String operations**: Word-at-a-time processing (4-8 bytes per iteration)
- **Double-double arithmetic**: 2-3x slower than double, 15+ orders of magnitude more precise

See [`docs/klang/11-poc-benchmark.md`](docs/klang/11-poc-benchmark.md) for detailed performance analysis.

## Contributing

Contributions welcome! Areas of interest:
- Additional C library functions
- Platform-specific optimizations
- Extended precision rounding modes
- Documentation and examples

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

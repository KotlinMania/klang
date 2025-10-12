# KLang

A Kotlin multiplatform systems programming library providing low-level primitives and utilities for native and JavaScript targets.

## Features

- **Multiplatform Support**: Targets JavaScript (browser), macOS (ARM64/x64), Linux (x64/ARM64), and Windows (x64)
- **Low-Level Primitives**: Memory operations, pointer utilities, bitwise operations
- **Numerical Computing**: Float128 support, fixed-point arithmetic
- **String Operations**: Advanced string manipulation utilities

## Platforms

- **JavaScript**: ES2015+ with ES modules
- **Native**: 
  - macOS (ARM64, x64)
  - Linux (x64, ARM64)
  - Windows (x64 via MinGW)

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
├── commonMain/       # Shared Kotlin code
├── jsMain/          # JavaScript-specific code
└── nativeMain/      # Native platform code (if present)
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

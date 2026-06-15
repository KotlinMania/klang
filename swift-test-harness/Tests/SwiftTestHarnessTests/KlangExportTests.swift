import XCTest
import Klang

// Swift tests for the Kotlin → Swift Export → SPM → swift test pipeline.
//
// Successful compilation proves three layers of the pipeline:
//
//   1. `embedSwiftExportForXcode` produced `Klang.swiftmodule/`
//      and the supporting KotlinRuntimeSupport / ExportedKotlinPackages /
//      KotlinRuntime swiftmodule bundles. If any of them were missing,
//      `import Klang` above would fail at compile time.
//
//   2. The static archive `libKlang.a` (produced by the
//      `linkSwiftExportBinaryDebugStaticMacosArm64` and
//      `mergeMacosDebugSwiftExportLibraries` tasks) supplied every
//      `__root____*` and `KotlinError`-related symbol the Swift modules
//      reference. If the archive were missing or empty, this test
//      executable would fail to link with "undefined symbols for
//      architecture arm64".
//
//   3. The Kotlin `swiftExport { moduleName = "Klang" }` and
//      `flattenPackage = "io.github.kotlinmania.klang"` configuration in
//      build.gradle.kts produced a module name that's both syntactically
//      valid as a Swift identifier and reachable from this Package.swift
//      via the `KlangLibrary` product.
//
final class KlangExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Klang swift module imported cleanly")
    }

    func testBitShiftModeEnumExports() throws {
        XCTAssertEqual(bitwise.BitShiftMode.AUTO.rawValue, 0)
        XCTAssertEqual(bitwise.BitShiftMode.NATIVE.description, "NATIVE")
        XCTAssertEqual(bitwise.BitShiftMode(rawValue: 2), .ARITHMETIC)
        XCTAssertEqual(bitwise.BitShiftMode("AUTO"), .AUTO)
    }

    func testArithmeticBitwiseOpsBridge() throws {
        let ops = bitwise.ArithmeticBitwiseOps(bitLength: 8)

        XCTAssertEqual(ops.and(value1: 0b1100, value2: 0b1010), 0b1000)
        XCTAssertEqual(ops.or(value1: 0b1100, value2: 0b1010), 0b1110)
        XCTAssertEqual(ops.xor(value1: 0b1100, value2: 0b1010), 0b0110)
        XCTAssertEqual(ops.not(value: 0b0000), 0xFF)
        XCTAssertEqual(ops.leftShift(value: 0b0011, bits: 2), 0b1100)
        XCTAssertEqual(ops.normalize(value: 0x1FF), 0xFF)
        XCTAssertTrue(ops.isBitSet(value: 0b1000, bitPosition: 3))
        XCTAssertFalse(ops.isBitSet(value: 0b1000, bitPosition: 2))
    }

}

#if canImport(Testing)
import Testing
import Klang

@Suite("Klang Swift Export Suite")
struct KlangExportTests {
    @Test("Swift module loads cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true), "Klang swift module imported cleanly")
    }

    @Test("BitShiftMode enum exports")
    func bitShiftModeEnumExports() {
        #expect(bitwise.BitShiftMode.AUTO.rawValue == 0)
        #expect(bitwise.BitShiftMode.NATIVE.description == "NATIVE")
        #expect(bitwise.BitShiftMode(rawValue: 2) == .ARITHMETIC)
        #expect(bitwise.BitShiftMode("AUTO") == .AUTO)
    }

    @Test("ArithmeticBitwiseOps bridge")
    func arithmeticBitwiseOpsBridge() {
        let ops = bitwise.ArithmeticBitwiseOps(bitLength: 8)

        #expect(ops.and(value1: 0b1100, value2: 0b1010) == 0b1000)
        #expect(ops.or(value1: 0b1100, value2: 0b1010) == 0b1110)
        #expect(ops.xor(value1: 0b1100, value2: 0b1010) == 0b0110)
        #expect(ops.not(value: 0b0000) == 0xFF)
        #expect(ops.leftShift(value: 0b0011, bits: 2) == 0b1100)
        #expect(ops.normalize(value: 0x1FF) == 0xFF)
        #expect(ops.isBitSet(value: 0b1000, bitPosition: 3))
        #expect(!ops.isBitSet(value: 0b1000, bitPosition: 2))
    }
}
#elseif canImport(XCTest)
import XCTest
import Klang

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
#endif

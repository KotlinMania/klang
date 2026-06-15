import XCTest
import KlangTests

final class KlangCommonTestExportTests: XCTestCase {
    func testCommonTestSuiteRunsThroughSwiftExport() throws {
        let suite = SwiftCommonTestSuite()

        XCTAssertEqual(suite.runAll(), suite.expectedTestCount())
        XCTAssertGreaterThan(suite.expectedTestCount(), 0)
    }
}

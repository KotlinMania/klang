// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SwiftCommonTestHarness",
    dependencies: [
        .package(name: "KlangTests", path: "../build/swift-test-packages/KlangTests")
    ],
    targets: [
        .testTarget(
            name: "SwiftCommonTestHarnessTests",
            dependencies: [
                .product(name: "KlangTestsLibrary", package: "KlangTests")
            ],
            linkerSettings: [
                .unsafeFlags([
                    "-L", "../build/swift-test",
                    "-lKlangTests",
                ]),
            ]
        ),
    ]
)

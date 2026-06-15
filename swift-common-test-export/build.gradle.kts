import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = rootProject.group
version = rootProject.version

val frameworkName = providers.gradleProperty("project.frameworkName").get()
val projectNamespace = providers.gradleProperty("project.namespace").get()
val kotlinVersion = providers.gradleProperty("versions.kotlin").getOrElse("2.4.0")
val jvmToolchainVersion = providers.gradleProperty("jvm.toolchain").getOrElse("21").toInt()
val generatedRunnerRoot = layout.buildDirectory.dir("generated/swiftCommonTestRunner/kotlin")
val generatedRunnerFile =
    generatedRunnerRoot.map {
        it.file("${projectNamespace.replace('.', '/')}/SwiftCommonTestSuite.kt")
    }
val commonTestSources =
    rootProject.fileTree(rootProject.layout.projectDirectory.dir("src/commonTest/kotlin")) {
        include("**/*.kt")
    }

data class CommonTestMethod(
    val packageName: String,
    val className: String,
    val methodName: String,
)

fun String.kotlinStringLiteral(): String =
    buildString {
        append('"')
        this@kotlinStringLiteral.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }

fun discoverCommonTests(): List<CommonTestMethod> {
    val packageRegex = Regex("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*$")
    val classRegex = Regex("^\\s*(?:public\\s+)?class\\s+([A-Za-z_][A-Za-z0-9_]*)\\b")
    val testMethodRegex = Regex("^\\s*(?:public\\s+)?fun\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")

    return commonTestSources.files
        .sortedBy { file -> file.relativeTo(rootProject.projectDir).invariantSeparatorsPath }
        .flatMap { file ->
            val text = file.readText()
            val packageName =
                packageRegex.find(text)?.groupValues?.get(1)
                    ?: throw GradleException("Missing package declaration in ${file.relativeTo(rootProject.projectDir)}")
            var className: String? = null
            var pendingTest = false
            val tests = mutableListOf<CommonTestMethod>()

            text.lineSequence().forEach { line ->
                classRegex.find(line)?.let { match ->
                    className = match.groupValues[1]
                }
                if (line.contains("@Test")) {
                    pendingTest = true
                }
                if (pendingTest) {
                    testMethodRegex.find(line)?.let { match ->
                        val owner =
                            className
                                ?: throw GradleException(
                                    "Found @Test before a class declaration in ${file.relativeTo(rootProject.projectDir)}",
                                )
                        tests += CommonTestMethod(packageName, owner, match.groupValues[1])
                        pendingTest = false
                    }
                }
            }
            tests
        }
        .toList()
}

val generateSwiftCommonTestRunner by tasks.registering {
    group = "verification"
    description = "Generates a Swift-exported runner for the root commonTest suite."
    inputs.files(commonTestSources)
    outputs.file(generatedRunnerFile)

    doLast {
        val tests = discoverCommonTests()
        if (tests.isEmpty()) {
            throw GradleException("No commonTest @Test methods were discovered for Swift export.")
        }

        val output = generatedRunnerFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            buildString {
                appendLine("package $projectNamespace")
                appendLine()
                appendLine("public class SwiftCommonTestSuite {")
                appendLine("    public fun expectedTestCount(): Int = ${tests.size}")
                appendLine()
                appendLine("    public fun runAll(): Int {")
                appendLine("        var executed = 0")
                tests.forEach { test ->
                    val label = "${test.packageName}.${test.className}.${test.methodName}".kotlinStringLiteral()
                    appendLine("        run($label) { ${test.packageName}.${test.className}().${test.methodName}() }")
                    appendLine("        executed += 1")
                }
                appendLine("        return executed")
                appendLine("    }")
                appendLine()
                appendLine("    private fun run(name: String, block: () -> Unit) {")
                appendLine("        try {")
                appendLine("            block()")
                appendLine("        } catch (throwable: Throwable) {")
                appendLine("            throw AssertionError(\"Swift commonTest export failed: \$name\", throwable)")
                appendLine("        }")
                appendLine("    }")
                appendLine("}")
            },
        )
    }
}

kotlin {
    jvmToolchain(jvmToolchainVersion)
    applyDefaultHierarchyTemplate()

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xsuppress-version-warnings")
    }

    macosArm64()

    swiftExport {
        moduleName = "${frameworkName}Tests"
        flattenPackage = projectNamespace
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(rootProject.layout.projectDirectory.dir("src/commonTest/kotlin"))
            kotlin.srcDir(generatedRunnerRoot)
            dependencies {
                implementation(project(":"))
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.configureEach {
    if (name.startsWith("compile") && name.contains("Kotlin")) {
        dependsOn(generateSwiftCommonTestRunner)
    }
}

fun patchSwiftExportPackagePlatforms(packageSwift: File) {
    if (packageSwift.exists()) {
        val text = packageSwift.readText()
        if (!text.contains("platforms:")) {
            packageSwift.writeText(
                text.replaceFirst(
                    Regex("(name:\\s*\"[^\"]*\",)"),
                    "\$1\n    platforms: [.macOS(.v14)],",
                ),
            )
        }
    }
}

tasks.matching { it.name == "macosArm64DebugBuildSPMPackage" }.configureEach {
    doFirst {
        patchSwiftExportPackagePlatforms(
            layout.buildDirectory
                .file("SPMPackage/macosArm64/Debug/Package.swift")
                .get()
                .asFile,
        )
    }
}

val patchSwiftExportGeneratedKotlinWarnings by tasks.registering {
    group = "build"
    description = "Normalizes generated Swift Export Kotlin bridge sources before strict compilation."
    dependsOn(
        tasks.matching { task ->
            task.name.endsWith("DebugSwiftExport") ||
                task.name.endsWith("DebugGenerateSPMPackage") ||
                task.name.endsWith("DebugBuildSPMPackage")
        },
    )
    outputs.upToDateWhen { false }
    doLast {
        val swiftExportDir = layout.buildDirectory.dir("SwiftExport").get().asFile
        if (!swiftExportDir.isDirectory) return@doLast

        swiftExportDir
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .forEach { file ->
                var text = file.readText()
                text = text.replace(Regex("(?m)^@file:(?:kotlin\\.)?Suppress\\([^\\r\\n]*\\)\\r?\\n"), "")
                text =
                    "@file:kotlin.Suppress(\"DEPRECATION_ERROR\", \"UNCHECKED_CAST\", \"UNUSED_EXPRESSION\", \"USELESS_ELVIS\")\n$text"
                if (file.name == "KotlinCoroutineSupport.kt") {
                    text =
                        text.replace(
                            Regex(
                                "(?m)^@file:OptIn\\(kotlinx\\.coroutines\\.InternalForInheritanceCoroutinesApi::class\\)\\r?\\n",
                            ),
                            "",
                        )
                    text = "@file:OptIn(kotlinx.coroutines.InternalForInheritanceCoroutinesApi::class)\n$text"
                    text =
                        text.replace(
                            "is State.Completed -> return state.error?.let { throw it } ?: null",
                            """
                            is State.Completed -> {
                                state.error?.let { throw it }
                                return null
                            }
                            """.trimIndent(),
                        )
                }
                file.writeText(text)
            }
    }
}

tasks
    .matching { task ->
        task.name.startsWith("compileSwiftExport") &&
            task.name.contains("Kotlin")
    }.configureEach {
        dependsOn(patchSwiftExportGeneratedKotlinWarnings)
    }

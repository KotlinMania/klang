import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

// Source-code rule: no `import java.*` / `import javax.*` anywhere in `src/`.
// Everything else about targets / plugins is the build's call. See CLAUDE.md.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.taskinfo)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotlin.allopen)
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    idea
}

// kotlinx-benchmark requires the @State class to be open for subclassing.
// Mark the @State annotation so the all-open plugin opens annotated classes.
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("kotlinx.benchmark.State")
}

group = "io.github.kotlinmania"
version = "0.8.0"

val hostOsName: String = System.getProperty("os.name").lowercase()
val isWindowsHost: Boolean = hostOsName.contains("windows") || hostOsName.contains("mingw")
val androidCommandLineToolsRevision = "14742923"
val projectCompileSdk = "34"
val projectAndroidBuildTools = "36.0.0"
val androidSdkOsName =
    when {
        isWindowsHost -> "win"
        hostOsName.contains("mac") -> "mac"
        hostOsName.contains("linux") -> "linux"
        else -> throw GradleException("Unsupported Android SDK setup OS: ${System.getProperty("os.name")}")
    }
val projectAndroidSdkDir = layout.projectDirectory.dir(".android-sdk").asFile
val androidSdkManager = projectAndroidSdkDir.resolve(
    if (isWindowsHost) {
        "cmdline-tools/latest/bin/sdkmanager.bat"
    } else {
        "cmdline-tools/latest/bin/sdkmanager"
    },
)
val androidSdkInstallMarker = projectAndroidSdkDir.resolve(".install-complete")

fun writeAndroidLocalProperties() {
    val sdkDirPropertyValue = projectAndroidSdkDir.absolutePath.replace("\\", "/")
    layout.projectDirectory.file("local.properties").asFile.writeText("sdk.dir=$sdkDirPropertyValue\n")
}

fun sdkManagerCommand(vararg args: String): List<String> =
    if (isWindowsHost) {
        listOf("cmd", "/c", androidSdkManager.absolutePath) + args
    } else {
        listOf(androidSdkManager.absolutePath) + args
    }

fun downloadAndroidCommandLineTools() {
    val zipName = "commandlinetools-$androidSdkOsName-${androidCommandLineToolsRevision}_latest.zip"
    val url = "https://dl.google.com/android/repository/$zipName"
    val tmpDir = projectAndroidSdkDir.resolve(".tmp/commandline-tools")
    val zipFile = tmpDir.resolve(zipName)
    val latestDir = projectAndroidSdkDir.resolve("cmdline-tools/latest")

    println("setup-android-sdk: downloading $url")
    tmpDir.deleteRecursively()
    tmpDir.mkdirs()

    try {
        URI(url).toURL().openStream().use { input ->
            Files.copy(input, zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        latestDir.deleteRecursively()
        latestDir.mkdirs()
        val canonicalLatestDir = latestDir.canonicalFile.toPath()

        ZipInputStream(zipFile.inputStream().buffered()).use { zipInput ->
            generateSequence { zipInput.nextEntry }.forEach { entry ->
                val relativeName = entry.name.removePrefix("cmdline-tools/").trimStart('/')
                if (relativeName.isNotEmpty()) {
                    val target = latestDir.resolve(relativeName).canonicalFile
                    if (!target.toPath().startsWith(canonicalLatestDir)) {
                        throw GradleException("Refusing to extract Android SDK entry outside $latestDir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile.mkdirs()
                        Files.copy(zipInput, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        if (!isWindowsHost && relativeName.startsWith("bin/")) {
                            target.setExecutable(true)
                        }
                    }
                }
                zipInput.closeEntry()
            }
        }

        if (!isWindowsHost) {
            androidSdkManager.setExecutable(true)
        }
    } finally {
        tmpDir.deleteRecursively()
    }
}

fun installProjectAndroidSdk(execOperations: ExecOperations) {
    if (androidSdkInstallMarker.exists() && androidSdkManager.exists()) {
        writeAndroidLocalProperties()
        println("setup-android-sdk: SDK already installed at $projectAndroidSdkDir")
        return
    }

    if (!androidSdkManager.exists()) {
        downloadAndroidCommandLineTools()
    }

    println("setup-android-sdk: accepting licenses")
    val licenseAnswers = "y\n".repeat(200).toByteArray(Charsets.UTF_8)
    val licenseResult = execOperations.exec {
        commandLine(sdkManagerCommand("--sdk_root=${projectAndroidSdkDir.absolutePath}", "--licenses"))
        standardInput = ByteArrayInputStream(licenseAnswers)
        isIgnoreExitValue = true
    }
    if (licenseResult.exitValue != 0) {
        throw GradleException("Android SDK license acceptance failed with exit code ${licenseResult.exitValue}")
    }

    println("setup-android-sdk: installing platform-tools, android-$projectCompileSdk, build-tools;$projectAndroidBuildTools")
    val installLog = projectAndroidSdkDir.resolve("sdkmanager-install.log")
    installLog.parentFile.mkdirs()
    installLog.outputStream().use { output ->
        val installResult = execOperations.exec {
            commandLine(
                sdkManagerCommand(
                    "--sdk_root=${projectAndroidSdkDir.absolutePath}",
                    "platform-tools",
                    "platforms;android-$projectCompileSdk",
                    "build-tools;$projectAndroidBuildTools",
                ),
            )
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        if (installResult.exitValue != 0) {
            throw GradleException(
                "Android SDK package install failed with exit code ${installResult.exitValue}. " +
                    "Install log:\n${installLog.readText()}",
            )
        }
    }
    println("setup-android-sdk: install log at $installLog")

    writeAndroidLocalProperties()
    androidSdkInstallMarker.writeText("")
    println("setup-android-sdk: done")
    println("  SDK at:     $projectAndroidSdkDir")
    println("  configured: local.properties -> $projectAndroidSdkDir")
}

//project.gradle.taskGraph.whenReady { println(project.gradle.taskGraph.allTasks) }

repositories {
    mavenCentral()
}

// The Android Gradle plugin resolves the SDK location during configuration,
// before any task can run. Keep the SDK local to this project and make the
// installer idempotent so CI and local builds use the same path.
val androidSdkExecOperations = serviceOf<ExecOperations>()
installProjectAndroidSdk(androidSdkExecOperations)

//val jsIndex = file("src/jsMain/resources/index.html")
//jsIndex.textContent = jsIndex.textContent.replace(Regex("Kotlin C Compiler WIP Version ([\\d\\.]*)"), "Kotlin C Compiler WIP Version $version")

// KLang project - C compiler and systems programming language

//fun KotlinTargetContainerWithPresetFunctions.common(callback: KotlinOnlyTarget<*>.() -> Unit) {
//    callback(presets.getByName("common").createTarget("common") as KotlinOnlyTarget<*>)
//}

kotlin {
    // Apply the standard KMP source-set hierarchy. This explicitly creates the
    // intermediate source sets klang's `src/nativeMain/` files live under
    // (nativeMain, appleMain, linuxMain). Without this template applied,
    // adding a new compilation (`benchmark`) destabilises the implicit
    // hierarchy and the platform `actual` declarations stop being visible
    // from `commonMain`.
    applyDefaultHierarchyTemplate()

    fun KotlinTarget.configureAll() {
        compilations.all {
            //kotlinOptions.freeCompilerArgs = listOf("-progressive", "-Xskip-metadata-version-check")
        }
        // Add a "benchmark" compilation alongside main and test. The
        // kotlinx-benchmark plugin reads it and generates the platform-specific
        // runner (BenchmarkJS for JS, JMH-style for native). associateWith(main)
        // makes the main compilation's outputs and platform `actual` declarations
        // visible to the benchmark sources.
        val mainCompilation = compilations.getByName("main")
        compilations.create("benchmark") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies {
                implementation(libs.kotlinx.benchmark.runtime)
            }
        }
    }

    fun KotlinNativeTarget.configureNative() {
        configureAll()
        binaries {
            executable {
                entryPoint = "io.github.kotlinmania.klang.poc.main"
            }
        }
    }

    // XCFramework collects per-Apple-target frameworks (macosArm64, iosArm64,
    // iosSimulatorArm64) into a single distributable `KLang.xcframework`
    // consumable by Swift / Xcode projects. Each Apple target opts in via
    // `binaries.framework { xcf.add(this) }`.
    val xcf = XCFramework("KLang")

    // Named JS targets: the executor-keyed split (workspace template fix
    // #3). Kotlin 2.3.21 does not expose separate `browser`/`nodejs`
    // KotlinJsIrCompilation objects on a single `js` target — both runtimes
    // share one `main` compilation, so a Node-only `require('fs')` in
    // `jsMain` lands in the browser bundle. Splitting into named top-level
    // targets gives each runtime its own Main source set
    // (`jsBrowserMain` / `jsNodeMain` / `wasmJsBrowserMain` / `wasmJsNodeMain`)
    // so Node-only `actual`s never reach the webpack browser bundle.
    val jsCompilerOptions: org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions.() -> Unit = {
        this.target = "es2015"
        this.useEsClasses = true
        sourceMap = true
        moduleKind = JsModuleKind.MODULE_ES
    }
    js("jsBrowser") {
        configureAll()
        browser()
        binaries.executable()
        compilerOptions(jsCompilerOptions)
    }
    js("jsNode") {
        configureAll()
        nodejs()
        binaries.executable()
        compilerOptions(jsCompilerOptions)
    }

    // wasmJs stays unsplit: Kotlin 2.3.21 disallows multiple wasmJs targets
    // ("Declaring multiple Kotlin Targets of the same type is not supported"
    // — only `js` has a temporary backdoor for the multi-target pattern, see
    // https://kotl.in/declaring-multiple-targets). Until KGP exposes either
    // per-runtime wasmJs compilations or accepts wasmJs("name"), wasmJsMain
    // stays a single source set across both browser and Node runtimes; any
    // Node-only wasmJs `actual` must use the `typeof process` runtime guard
    // documented in workspace CLAUDE.md as fix #4.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    macosArm64 {
        configureNative()
        binaries.framework {
            baseName = "KLang"
            xcf.add(this)
        }
    }
    linuxX64 { configureNative() }
    linuxArm64 { configureNative() }
    mingwX64 { configureNative() }
    iosArm64 {
        binaries.framework {
            baseName = "KLang"
            xcf.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "KLang"
            xcf.add(this)
        }
    }

    // Android KMP target. Exists so `compileAndroidMain` emits real JVM
    // `.class` files for CodeQL's `kotlinc` LD_PRELOAD tracer to hook.
    // The androidMain source set's actuals must obey the project's
    // `no import java.*` rule — see src/androidMain/.
    androidLibrary {
        namespace = "io.github.kotlinmania.klang"
        compileSdk = 34
        minSdk = 24
    }

    swiftExport {
        moduleName = "KLang"
        flattenPackage = "io.github.kotlinmania.klang"
    }

    // Removed generated sources; build/gen is no longer a source root

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                // Added for io.github.kotlinmania.klang parallel array operations and actors
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-test-common")
            }
        }
        // Executor-keyed split for the JS target family. `js` is the only
        // target type Kotlin 2.3.21 lets us declare multiple times (backdoor
        // pending a proper per-runtime compilation split — see
        // https://kotl.in/declaring-multiple-targets). `jsBrowserMain` and
        // `jsNodeMain` are the auto-created Main source sets for the
        // `js("jsBrowser")` and `js("jsNode")` targets.
        //
        //                  commonMain
        //                  /        \
        //               jsMain     wasmJsMain         (target-family axis)
        //               /    \
        //      jsBrowserMain  jsNodeMain              (executor axis, JS only)
        //
        // `jsMain` is a runtime-agnostic intermediate so the existing
        // `src/jsMain/kotlin/` directory keeps feeding both the browser and
        // Node compilations of the `js` family.
        //
        // `wasmJsMain` remains a single source set across the browser and
        // Node runtimes because KGP rejects multiple wasmJs targets; Node-
        // only wasmJs `actual`s rely on the `typeof process` runtime guard
        // (workspace CLAUDE.md fix #4) until KGP supports the split.
        //
        // `browserMain` / `nodeMain` per-executor intermediates are not
        // wired here: with wasmJs unsplit they would have only one member
        // each on the JS side, making them degenerate. Reintroduce them
        // once Kotlin Multiplatform lifts the wasmJs single-target
        // restriction.
        val jsMain by creating { dependsOn(commonMain.get()) }
        named("jsBrowserMain") { dependsOn(jsMain) }
        named("jsNodeMain") { dependsOn(jsMain) }

        named("jsBrowserTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
        named("jsNodeTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
    }
}

// Wire the per-target benchmark source sets:
//   commonMain → commonBenchmark → <target>Benchmark
//   <target>Main ────────────────→ <target>Benchmark
// commonBenchmark holds shared @Benchmark sources; each per-target benchmark
// source set also dependsOn its corresponding main so platform `actual`
// declarations are visible. Done in afterEvaluate because the per-target
// benchmark source sets are auto-created by `compilations.create("benchmark")`
// and only exist after target configuration completes.
afterEvaluate {
    val ssContainer = kotlin.sourceSets
    // commonBenchmark holds shared @Benchmark sources. It does NOT dependsOn
    // commonMain — the per-target main source sets pull in commonMain via
    // the default hierarchy template (commonMain → nativeMain → appleMain
    // → macosArm64Main, etc.), so going through commonBenchmark to
    // commonMain would create a second path and Kotlin/Native's IR linker
    // would bind every public symbol twice. commonBenchmark code can still
    // reference klang main types because each per-target benchmark
    // compilation has associateWith(main) which puts main on the classpath.
    val commonBenchmarkSs = ssContainer.findByName("commonBenchmark")
        ?: ssContainer.create("commonBenchmark").apply {
            dependencies {
                implementation(libs.kotlinx.benchmark.runtime)
            }
        }
    listOf(
        "macosArm64" to "macosArm64Benchmark",
        "linuxX64"   to "linuxX64Benchmark",
        "linuxArm64" to "linuxArm64Benchmark",
        "mingwX64"   to "mingwX64Benchmark",
        // JS benchmarks run on the Node runtime — webpack/karma overhead
        // distorts measurement otherwise. The `js("jsNode")` named target
        // provides a `jsNodeBenchmark` source set.
        "jsNode"     to "jsNodeBenchmark",
    ).forEach { (_, benchmarkSetName) ->
        val benchmarkSs = ssContainer.findByName(benchmarkSetName) ?: return@forEach
        benchmarkSs.dependsOn(commonBenchmarkSs)
    }
}

// ---------------------------------------------------------------------------
// kotlinx-benchmark configuration
//
// Registers the per-target "benchmark" compilations as benchmark targets so
// `./gradlew benchmark` runs them on every host-supported platform. Each
// target gets a JMH-style runner (or BenchmarkJS for JS) with proper warmup,
// measurement iterations, and structured ops/sec output — independent of
// the regular test harness, so the JS mocha 2-second timeout that broke
// TuiBufferBenchmarkTest is no longer in the path.
// ---------------------------------------------------------------------------
benchmark {
    targets {
        register("jsNodeBenchmark")
        register("macosArm64Benchmark")
        register("linuxX64Benchmark")
        register("linuxArm64Benchmark")
        register("mingwX64Benchmark")
    }
    configurations {
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
        }
    }
}

val mainClassName = "io.github.kotlinmania.klang.KLangExportsKt"

// Browser variant's compilations drive the webpack/karma bundling and the
// `jsWeb*` Copy tasks below that produce the demo page under `docs/`.
val jsCompilations = kotlin.targets["jsBrowser"].compilations

// Removed GenerateSourcesTask and generated includes/runtime; unused in this fork

tasks {
    // No generated sources

    //val runDceJsKotlin = named<KotlinJsDce>("runDceJsKotlin").get()
    // JVM-specific jar tasks removed

    //val jsWebResourcesDce by creating(Copy::class) {
    //    dependsOn(runDceJsKotlin)
    //    into(file("docs"))
    //    includeEmptyDirs = false
    //    from(kotlin.sourceSets["jsMain"].resources)
    //    from(kotlin.sourceSets["commonMain"].resources)
    //}

    val jsWebResources by registering(Copy::class) {
        // Demo site is browser-only — pull from the jsBrowser target's main
        // classes and the (runtime-agnostic) jsMain intermediate's
        // resources (`src/jsMain/resources/`: ace.js, index.html, etc).
        dependsOn("jsBrowserMainClasses")
        into(file("docs"))
        includeEmptyDirs = false
        from(kotlin.sourceSets["jsMain"].resources)
        from(kotlin.sourceSets["commonMain"].resources)
    }

    //val jsWebDce by creating(Copy::class) {
    //    dependsOn(jsWebResourcesDce)
    //    into(file("docs"))
    //    includeEmptyDirs = false
    //    exclude("**/*.kjsm", "**/*.kotlin_metadata", "**/*.kotlin_module", "**/*.MF", "**/*.meta.js", "**/*.map")
    //    from(named("compileProductionExecutableKotlinJs").get().outputs)
    //}

    val jsWeb by registering(Copy::class) {
        dependsOn(jsWebResources)
        into(file("docs"))
        includeEmptyDirs = false
        exclude("**/*.kjsm", "**/*.kotlin_metadata", "**/*.kotlin_module", "**/*.MF", "**/*.meta.js", "**/*.map")
        from(named("compileDevelopmentExecutableKotlinJsBrowser").get().outputs)
    }

    val buildDockerImage by registering(Exec::class) {
        dependsOn("linkReleaseExecutableLinuxArm64")
        commandLine("docker", "build", ".", "-t", "klang:latest")
    }
    val buildDockerImageAndPublish by registering(Exec::class) {
        dependsOn("buildDockerImage")
        commandLine("docker", "push", "klang:latest")
    }
    // Removed generateSources dependencies; no generated files needed
}

// Disable cross-target native tests on hosts that can't actually run their binaries.
// Linux x64 and Windows x64 binaries only execute on their respective hosts; without this
// guard `./gradlew allTests` on macOS fails because cross-compiled test binaries can't run.
// CI runs each native test on the matching host (see .github/workflows/ci.yml).
val isLinuxHost: Boolean = hostOsName.contains("linux")

afterEvaluate {
    if (!isLinuxHost) tasks.findByName("linuxX64Test")?.enabled = false
    if (!isWindowsHost) tasks.findByName("mingwX64Test")?.enabled = false
    // Enable macOS aarch64 native tests
    tasks.findByName("macosArm64Test")?.enabled = true

    // Convenience: make macOS aarch64 the default native run/build target on this machine.
    val runMac = tasks.findByName("runDebugExecutableMacosArm64")
    if (runMac != null) {
        tasks.register("run") {
            group = "application"
            description = "Runs the macOS arm64 debug executable"
            dependsOn(runMac)
        }
        tasks.register("buildMac") {
            group = "build"
            description = "Builds the macOS arm64 release executable"
            dependsOn("linkReleaseExecutableMacosArm64")
        }
    }

    // Register build target tasks
    tasks.register("buildTargets") {
        group = "help"
        description = "Shows available build targets for Kotlin Multiplatform"
        doLast {
            println("""
                |
                |═══════════════════════════════════════════════════════════════════════════════
                |  KLang - Kotlin Multiplatform Build Targets
                |═══════════════════════════════════════════════════════════════════════════════
                |
                |  This is a Kotlin Multiplatform library focused on native builds.
                |  The primary targets are native platforms (Linux, macOS, Windows).
                |  JS and JVM targets are included but native is the focus.
                |
                |  NATIVE TARGETS (Primary Focus):
                |  ─────────────────────────────────────────────────────────────────────────────
                |  • macosArm64Binaries         - Build all macOS ARM64 binaries
                |  • macosX64Binaries            - Build all macOS x64 binaries
                |  • linuxX64Binaries            - Build all Linux x64 binaries
                |  • linuxArm64Binaries          - Build all Linux ARM64 binaries
                |  • mingwX64Binaries            - Build all Windows x64 binaries
                |
                |  SPECIFIC NATIVE BUILDS:
                |  ─────────────────────────────────────────────────────────────────────────────
                |  • linkReleaseExecutableMacosArm64   - macOS ARM64 release
                |  • linkReleaseExecutableMacosX64     - macOS x64 release
                |  • linkReleaseExecutableLinuxX64     - Linux x64 release
                |  • linkReleaseExecutableLinuxArm64   - Linux ARM64 release
                |  • linkReleaseExecutableMingwX64     - Windows x64 release
                |
                |  • linkDebugExecutableMacosArm64     - macOS ARM64 debug
                |  • linkDebugExecutableMacosX64       - macOS x64 debug
                |  • linkDebugExecutableLinuxX64       - Linux x64 debug
                |  • linkDebugExecutableLinuxArm64     - Linux ARM64 debug
                |  • linkDebugExecutableMingwX64       - Windows x64 debug
                |
                |  JAVASCRIPT TARGET:
                |  ─────────────────────────────────────────────────────────────────────────────
                |  • jsBrowserProductionWebpack  - Build optimized JS for browser
                |  • jsBrowserDevelopmentWebpack - Build development JS for browser
                |  • jsWeb                       - Copy JS resources to docs/
                |
                |  TESTING:
                |  ─────────────────────────────────────────────────────────────────────────────
                |  • allTests                    - Run all enabled tests
                |  • macosArm64Test              - Run macOS ARM64 tests (enabled)
                |  • jsTest                      - Run JS tests
                |
                |  ALL TARGETS:
                |  ─────────────────────────────────────────────────────────────────────────────
                |  • assemble                    - Build all configured targets
                |  • build                       - Build and test all targets
                |
                |  CONVENIENCE SHORTCUTS:
                |  ─────────────────────────────────────────────────────────────────────────────
                |  • buildMac                    - Build macOS ARM64 release (convenience)
                |  • run                         - Run macOS ARM64 debug (convenience)
                |
                |  EXAMPLES:
                |  ─────────────────────────────────────────────────────────────────────────────
                |  ./gradlew macosArm64Binaries            # Build all macOS ARM64 binaries
                |  ./gradlew linkReleaseExecutableLinuxX64 # Build Linux x64 release
                |  ./gradlew assemble                      # Build all targets
                |  ./gradlew build                         # Build and test all
                |  ./gradlew buildMac                      # Quick macOS ARM64 build
                |
                |═══════════════════════════════════════════════════════════════════════════════
                |
            """.trimMargin())
        }
    }
}

//compilations.all {
//    kotlinOptions {
//        freeCompilerArgs = ["-progressive", "-Xskip-metadata-version-check"]
//    }
//}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
    //rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
}

// ---------------------------------------------------------------------------
// Kotlin/JS toolchain — pin Node 22 LTS and Yarn 1.22.22, plus webpack/karma
// versions that close the dependabot alerts on the kotlin-js-store yarn lock.
// Mirrors starlark-kotlin's build.gradle.kts; klang has no wasmJs target so
// the Wasm-side configurations are intentionally omitted.
// ---------------------------------------------------------------------------

rootProject.extensions.configure<NodeJsEnvSpec>("kotlinNodeJsSpec") {
    version.set("22.22.2")
}

rootProject.extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") {
    version.set("22.22.2")
}

rootProject.extensions.configure<YarnRootEnvSpec>("kotlinYarnSpec") {
    version.set("1.22.22")
}

rootProject.extensions.configure<WasmYarnRootEnvSpec>("kotlinWasmYarnSpec") {
    version.set("1.22.22")
}

rootProject.extensions.configure<YarnRootExtension>("kotlinYarn") {
    // CVE-driven version overrides for transitive npm deps. Each pair forces
    // both top-level and nested resolution so a downstream package can't pull
    // back in a vulnerable version through its own dependency tree.
    resolution("diff", "8.0.3")
    resolution("**/diff", "8.0.3")
    resolution("serialize-javascript", "7.0.5")
    resolution("**/serialize-javascript", "7.0.5")
    resolution("webpack", "5.106.2")
    resolution("**/webpack", "5.106.2")
    resolution("follow-redirects", "1.16.0")
    resolution("**/follow-redirects", "1.16.0")
    resolution("lodash", "4.18.1")
    resolution("**/lodash", "4.18.1")
    resolution("ajv", "8.20.0")
    resolution("**/ajv", "8.20.0")
    resolution("brace-expansion", "5.0.5")
    resolution("**/brace-expansion", "5.0.5")
    resolution("flatted", "3.4.2")
    resolution("**/flatted", "3.4.2")
    resolution("minimatch", "10.2.5")
    resolution("**/minimatch", "10.2.5")
    resolution("picomatch", "4.0.4")
    resolution("**/picomatch", "4.0.4")
    resolution("qs", "6.15.1")
    resolution("**/qs", "6.15.1")
    resolution("socket.io-parser", "4.2.6")
    resolution("**/socket.io-parser", "4.2.6")
}

// Path to the vendored karma-webpack patch package whose dependencies pin
// glob ^13, minimatch ^10.2.5, webpack-merge ^4 — closing the chain of
// transitive CVEs that the upstream karma-webpack 5.0.1 still pulls in.
val patchedKarmaWebpackPackage: String =
    rootProject.layout.projectDirectory.dir("gradle/npm/karma-webpack").asFile.absolutePath.replace("\\", "/")

rootProject.extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
    versions.webpack.version = "5.106.2"
    versions.webpackCli.version = "7.0.2"
    versions.karma.version = "npm:karma-maintained@6.4.7"
    versions.karmaWebpack.version = "file:$patchedKarmaWebpackPackage"
    versions.mocha.version = "12.0.0-beta-10"
    versions.kotlinWebHelpers.version = "3.1.0"
}

tasks.withType(Test::class.java).all {
    testLogging {

        setEvents(setOf(TestLogEvent.PASSED,TestLogEvent.SKIPPED,TestLogEvent.FAILED,TestLogEvent.STANDARD_OUT,TestLogEvent.STANDARD_ERROR))
        //setEvents(setOf("skipped", "failed", "standardError"))
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

idea {
    module {
        excludeDirs = excludeDirs + setOf(file(".gradle"), file("@old"), file("doc"), file("docs"), file("samples"), file("gradle"), file("build"), file("include"), file(".idea"), file(".github"), file("temp"), file("build/gen"))
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "klang", version.toString())

    pom {
        name.set("klang")
        description.set("Pure Kotlin Multiplatform systems-programming library that replicates C bitwise, numeric, and memory semantics for exact C-to-Kotlin code porting.")
        inceptionYear.set("2024")
        url.set("https://github.com/KotlinMania/klang")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("sydneyrenee")
                name.set("Sydney Renee")
                email.set("sydney@solace.ofharmony.ai")
                url.set("https://github.com/sydneyrenee")
            }
        }

        scm {
            url.set("https://github.com/KotlinMania/klang")
            connection.set("scm:git:git://github.com/KotlinMania/klang.git")
            developerConnection.set("scm:git:ssh://github.com/KotlinMania/klang.git")
        }
    }
}

tasks.register("setupAndroidSdk") {
    group = "setup"
    description = "Downloads and configures the project-local Android SDK."
    doLast {
        installProjectAndroidSdk(androidSdkExecOperations)
    }
}

// CodeQL's Gradle autobuild invokes `./gradlew testClasses`, which is a
// JVM-convention task that Kotlin Multiplatform projects without a JVM
// target do not provide. Without it, CodeQL aborts with
// `Task 'testClasses' not found in root project` and skips the scan.
// Register an aggregate task that depends on every per-target
// test-compile task (jsBrowserTestClasses, jsNodeTestClasses,
// wasmJsTestClasses, and the compileTestKotlin<Target> tasks for native
// targets) so the convention call resolves. We *also* pull in
// `compileAndroidMain` so the dynamic Default Setup / Code Quality
// `autobuild` invocation drives the JVM kotlinc that CodeQL's LD_PRELOAD
// tracer hooks.
tasks.register("testClasses") {
    description = "Aggregate test-compile task for CodeQL and other JVM-convention callers."
    group = "verification"
    dependsOn(tasks.matching { other ->
        val n = other.name
        n != "testClasses" &&
            (n.endsWith("TestClasses") ||
                n.startsWith("compileTestKotlin") ||
                n == "compileAndroidMain")
    })
}

tasks.register("codeqlCompileJvm") {
    description = "Compile Android main sources so CodeQL can extract JVM Kotlin classes."
    group = "build"
    dependsOn("compileAndroidMain")
}

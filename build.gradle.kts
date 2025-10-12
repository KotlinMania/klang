import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.2.20" apply true
    id("org.barfuin.gradle.taskinfo") version "2.2.0"
    idea
}

//project.gradle.taskGraph.whenReady { println(project.gradle.taskGraph.allTasks) }

repositories {
    mavenLocal()
    mavenCentral()
}

//val jsIndex = file("src/jsMain/resources/index.html")
//jsIndex.textContent = jsIndex.textContent.replace(Regex("Kotlin C Compiler WIP Version ([\\d\\.]*)"), "Kotlin C Compiler WIP Version $version")

// KLang project - C compiler and systems programming language

//fun KotlinTargetContainerWithPresetFunctions.common(callback: KotlinOnlyTarget<*>.() -> Unit) {
//    callback(presets.getByName("common").createTarget("common") as KotlinOnlyTarget<*>)
//}

//val enableNative = false
val enableNative = System.getenv("KLANG_NO_ENABLE_NATIVE").isNullOrBlank()

kotlin {
    fun KotlinTarget.configureAll() {
        compilations.all {
            //kotlinOptions.freeCompilerArgs = listOf("-progressive", "-Xskip-metadata-version-check")
        }
    }

    fun KotlinNativeTarget.configureNative() {
        configureAll()
        binaries {
            executable {
                entryPoint = "ai.solace.klang.poc.main"
            }
        }
    }



    //common {
    //}

    // JVM target removed: pure multiplatform (JS + Native only)
    js {
        configureAll()
        browser()
        binaries.executable()
        compilerOptions {
            this.target = "es2015"
            this.useEsClasses = true
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_ES
        }
    }

    if (enableNative) {
        macosArm64 { configureNative() }
        macosX64 { configureNative() }
        linuxX64 { configureNative() }
        linuxArm64 { configureNative() }
        mingwX64 { configureNative() }

        //sourceSets {
        //    val nativeMain = this.create("nativeMain")
        //    val nativeTest = this.create("nativeTest")
        //    configure(listOf(this.getByName("macosX64Main"), this.getByName("macosArm64Main"), this.getByName("linuxX64Main"), this.getByName("mingwX64Main"))) {
        //        dependsOn(nativeMain)
        //    }
        //}
    }

    // Removed generated sources; build/gen is no longer a source root

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                // Added for ai.solace.klang parallel array operations and actors
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-test-common")
            }
        }
        jsTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
    }
}

val mainClassName = "ai.solace.klang.KLangExportsKt"

val jsCompilations = kotlin.targets["js"].compilations

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

    val jsWebResources by creating(Copy::class) {
        dependsOn("jsMainClasses")
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

    val jsWeb by creating(Copy::class) {
        dependsOn(jsWebResources)
        into(file("docs"))
        includeEmptyDirs = false
        exclude("**/*.kjsm", "**/*.kotlin_metadata", "**/*.kotlin_module", "**/*.MF", "**/*.meta.js", "**/*.map")
        //from(named("compileProductionExecutableKotlinJs").get().outputs)
        from(named("compileDevelopmentExecutableKotlinJs").get().outputs)
    }

    val buildDockerImage by creating {
        afterEvaluate {
            dependsOn("linkReleaseExecutableLinuxArm64")
        }
        doLast {
            exec { commandLine = listOf("docker", "build", ".", "-t", "klang:latest") }
        }
    }
    val buildDockerImageAndPublish by creating {
        dependsOn("buildDockerImage")
        doLast {
            exec { commandLine = listOf("docker", "push", "klang:latest") }
        }
    }
    // Removed generateSources dependencies; no generated files needed
}

afterEvaluate {
    tasks.findByName("linuxX64Test")?.enabled = false
    tasks.findByName("macosX64Test")?.enabled = false
    tasks.findByName("mingwX64Test")?.enabled = false
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
}

//compilations.all {
//    kotlinOptions {
//        freeCompilerArgs = ["-progressive", "-Xskip-metadata-version-check"]
//    }
//}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
    //rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
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

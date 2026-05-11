Build and Run
=============

Targets
- macOS arm64 is the primary native run target; JS compiles for purity checks.

Gradle tasks
- Run (native debug, macOS arm64): `./gradlew run`
- Build native release: `./gradlew buildMac`
- JS dev compile: `./gradlew compileDevelopmentExecutableKotlinJs`

Entry point
- Native entry is `io.github.kotlinmania.klang.poc.main` (set in build.gradle.kts).

Notes
- We removed JVM deps; project is Kotlin Multiplatform (Kotlin/JS + Kotlin/Native).
- No `jvm()` target; source code does not import `java.*` / `javax.*`. See [`CLAUDE.md`](../../CLAUDE.md) for the full rule and the circumstances under which an Android KMP target is permissible.


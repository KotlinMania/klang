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
- Source-code rule: no `import java.*` / `import javax.*` in `src/`. See [`CLAUDE.md`](../../CLAUDE.md).


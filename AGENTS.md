# Claude Code Project Instructions — klang

## Hard rule: **don't import JVM libraries**

klang's source code must be **pure Kotlin**, no JVM-library imports. The shipped runtime targets are **Kotlin/JS and Kotlin/Native**; the JVM target was removed deliberately and is not coming back.

### What "no JVM libraries" means concretely

In `src/` (commonMain, jsMain, nativeMain, and any other source set):

- **No `import java.*` / `import javax.*`.** Period. Not `java.io.File`, not `java.text.SimpleDateFormat`, not `java.util.Date`, not `java.util.TimeZone`. If a JVM convenience is tempting, find or write a multiplatform equivalent (`kotlinx-io`, `kotlinx-datetime`, or stdlib).
- **No `jvm()` target.** klang publishes Kotlin/JS + Kotlin/Native artifacts; the JVM target was removed and stays removed. The comment line in `build.gradle.kts` is authoritative:
  ```kotlin
  // JVM target removed: pure multiplatform (JS + Native only)
  ```
- **Kotlin stdlib calls on JVM are OK when they're the actual Kotlin API surface.** `System.getenv` on JVM is Kotlin's stdlib idiom for JVM target — not a `java.*` import, just JVM-side stdlib resolution. The line you should not cross is *adding new `java.io.*` / `java.text.*` / `java.util.*` dependencies.*
- **`kotlin.jvm.*` annotations (`@JvmInline`, `@JvmName`, `@JvmStatic`, `@JvmField`, `@JvmOverloads`) are JVM-only metadata.** They compile to a no-op on Native and JS so they're tolerated when strictly necessary (e.g. `@JvmInline` on a value class so it compiles under a JVM-bytecode target), but prefer the unannotated form when it works. If you find yourself reaching for `@JvmName` to dodge a JVM-erasure clash, **stop** — the workspace memory says "JVM signature clash: distinct names only", not "@JvmName your way out".

### Where targets stand

Currently shipping:
```
macosArm64, linuxX64, linuxArm64, mingwX64, js
```

Notably absent and not currently planned:
- `jvm()` — removed deliberately; do not reintroduce.
- `wasmJs` — planned but not yet wired.
- `iosArm64` / `iosSimulatorArm64` — no consumer.

An **Android KMP target is permissible** (it produces JVM bytecode but is multiplatform-shaped) **provided** that:
1. The plugin and `android { ... }` / `androidLibrary { ... }` block are added explicitly with the user's go-ahead, **not** as a stealth fix for a CodeQL extraction problem.
2. The `androidMain` source set provides actuals **without `import java.*` / `import javax.*`** — use `kotlinx-io` / `kotlinx-datetime` / `System.getenv` / multiplatform equivalents instead.
3. `commonMain`'s generic `CPointer<T>` operator overloads (in `Runtime.kt` / `PointerExtensions.kt`) — which clash under JVM erasure — are sheltered in a `nonAndroidMain` intermediate source set that JS and Native (but not Android) depend on. This pattern is already proven on `starlark-syntax-kotlin` and is the right shape; just don't add it without saying so first.

### Why the rule exists

- **Determinism is the product.** klang gives C-porting consumers bit-exact behaviour across compilers. JVM-only library calls (`SimpleDateFormat` locale fallback, `String.intern`, `System.currentTimeMillis` timezone handling, …) are determinism leaks that break the contract.
- **`CPointer<T>` generic operator overloads.** `Runtime.kt` defines `operator fun CPointer<Byte>.plus(Int)` / `CPointer<Short>.plus(Int)` / etc. The type parameter survives on Native and JS but erases on JVM. *Adding a JVM-bytecode target (Android included) without sheltering those files breaks the build with "platform declaration clash" and the only fix is renaming every C-pointer operator — a sweeping API break across every consumer port.* If Android is added, do the source-set surgery alongside it.
- **Workspace memory.** `threadlocal-kotlin` is the documented one-off allowed to have a `jvm()` target. klang isn't an exception.

### "But CodeQL fails on `java-kotlin` without a JVM target!"

Yes. GitHub Code Quality's autobuild can't extract Kotlin source when no JVM-bytecode compilation runs. That's a CodeQL preview limitation, not a klang bug. The options:

1. Leave the dynamic `Analyze (java-kotlin)` job red. The custom `.github/workflows/codeql.yml` already scans actions + javascript-typescript and exits clean.
2. Disable Code Quality in repo Settings UI.
3. With prior approval, add an Android KMP target *and* the `nonAndroidMain` source set described above *and* `androidMain` actuals that don't import `java.*`. Don't take this on uninstructed.

## Build and test

```
./gradlew test            # macosArm64Test + jsNodeTest + wasmJsNodeTest (the project-stated gate)
./gradlew compileKotlinJs compileKotlinMacosArm64 compileKotlinLinuxX64 compileKotlinMingwX64
```

There is no `jvmTest`. If `compileKotlinJvm` ever shows up in `./gradlew tasks --all`, something added a `jvm()` target — revert it.

## Commit messages

- No AI branding or attribution.
- Clear, descriptive, focused on what changed and why.
- No `Co-Authored-By` lines, no robot emoji, no "Generated with" footers.

## See also

- `docs/general/build-and-run.md` — authoritative build doc ("We removed JVM deps; project is Kotlin Multiplatform (JS + Native)")
- `docs/general/overview.md` and `docs/general/philosophy-and-design.md` — describe bit-exact behavior reproducible "across any Kotlin compiler"; that's about *C-semantics determinism*, not about klang publishing a JVM artefact.

# Claude Code Project Instructions — klang

## Hard rule: **NO JVM. NOT EVEN VIA ANDROID.**

klang is a Kotlin Multiplatform library targeting **Kotlin/JS + Kotlin/Native only**. The JVM target was removed deliberately and **must not be reintroduced under any guise** — not as a `jvm()` target, not as an `androidTarget` / `androidLibrary` block, not as a `kotlin-jvm-only-helper` source set, not "just for tests", not "just for CodeQL", not "just for benchmarks".

The build.gradle.kts comment line is law:
```kotlin
// JVM target removed: pure multiplatform (JS + Native only)
```

### What "no JVM" forbids

1. **No `jvm()` target.** No JVM compilation. No `.class` files. No JAR output. No `kotlin-stdlib-jdk*` dependencies.
2. **No `androidTarget` / `androidLibrary` / `com.android.kotlin.multiplatform.library` plugin.** An Android KMP target compiles to JVM bytecode and is, for the purposes of this rule, a JVM target wearing a hat. Adding it pulls in the Google Maven repo, requires an Android SDK on every developer's machine, and forces all of commonMain to be JVM-erasure-compatible — which klang is deliberately not (see the `CPointer<T>` generic operator overloads in `Runtime.kt` / `PointerExtensions.kt`).
3. **No imports from `java.*` or `javax.*`** anywhere. No `System.getenv`, no `java.io.File`, no `java.text.SimpleDateFormat`, no `java.util.Date`. The `kotlin.jvm.*` namespace is also off-limits.
4. **No `@JvmInline` / `@JvmName` / `@JvmStatic` / `@JvmField` / `@JvmOverloads`** unless the only purpose is satisfying a JVM contract — which klang doesn't have one of. Value classes work without the annotation on Native and JS.
5. **`expect`/`actual` is per-target.** All `actual` declarations must live in `jsMain` and `nativeMain` (or one of the apple/linux/mingw leaf source sets). No `jvmMain`, no `androidMain`.

### Why

- **Determinism is the product.** klang exists to give C-porting work bit-exact behaviour across platforms. Every JVM-only dependency or JVM-only intrinsic is a determinism leak — `String.intern`, `System.currentTimeMillis`, `SimpleDateFormat` locale handling — that breaks the contract for the only consumer that matters (other ports in this workspace).
- **Generic `CPointer<T>` operator overloads.** klang's runtime defines `operator fun CPointer<Byte>.plus(Int)` / `CPointer<Short>.plus(Int)` / `CPointer<Long>.plus(Int)` / etc. On Native and JS the type parameter survives; on JVM it erases to a single `plus(CPointer, Int): CPointer` and the compiler aborts with "platform declaration clash". Reintroducing JVM means renaming every C-pointer operator to a primitive-suffixed name — a sweeping API break across every consumer port that uses klang. Don't.
- **Workspace memory.** The kotlinmania workspace memory entry "threadlocal-kotlin JVM exception" calls out that `threadlocal-kotlin` is the *one* repo allowed to declare a JVM target (per a specific downstream user request). The strict-KMP rule applies everywhere else. klang is everywhere else.

### "But CodeQL fails on `java-kotlin` without a JVM target!"

Yes, GitHub Code Quality's autobuild can't extract Kotlin source from a no-JVM-target build. That is a **CodeQL preview limitation**, not a klang bug. Solutions, in order of preference:

1. Leave the dynamic `Analyze (java-kotlin)` job red. The custom `.github/workflows/codeql.yml` already scans actions + javascript-typescript and exits clean.
2. Disable Code Quality in the repo's Settings UI (no REST API for this yet — GitHub roadmap says mid-2026).
3. **Do not** add an Android KMP target to satisfy CodeQL. That violates this file's hard rule and pulls JVM into the build.

### "What about benchmarks / linting / tooling?"

- `kotlinx-benchmark` produces JS and Native runners — both are allowed.
- The `taskinfo` plugin is JVM-only Gradle plugin code that runs at Gradle time, not at klang's runtime — that's fine; Gradle itself is JVM-hosted by definition.
- The Kotlin/Native distribution that backs `compileKotlinMacosArm64` / `compileKotlinLinuxX64` / `compileKotlinMingwX64` runs on a JVM under the hood. That's Gradle's problem, not klang's; klang doesn't link against the JVM.

The distinction is: **klang source code and its dependency graph at runtime contain no JVM.** Tooling that wraps it is whatever it is.

## Targets (authoritative list)

```
macosArm64, linuxX64, linuxArm64, mingwX64, js, wasmJs
```

When in doubt, run `./gradlew tasks --all | grep compileKotlin` and check the list contains exactly those. If `compileKotlinJvm`, `compileAndroidMain`, or anything similar appears in the output: revert whatever introduced it.

`linuxArm64` is a deliberate addition past the workspace canonical set (used by downstream native-arm64 consumers). `iosArm64` / `iosSimulatorArm64` are deliberate omissions — klang has no consumers on Apple-mobile platforms. `wasmJs` is a planned add but not yet wired.

## Build and test

```
./gradlew test            # macosArm64Test + jsNodeTest + wasmJsNodeTest (the project-stated gate)
./gradlew compileKotlinJs compileKotlinMacosArm64 compileKotlinLinuxX64 compileKotlinMingwX64
```

There is no `jvmTest`. There is no `androidTest`. There is no `compileAndroidMain`. If any of those exist in your task list, something is wrong.

## Commit messages

- No AI branding or attribution.
- Clear, descriptive, focused on what changed and why.
- No `Co-Authored-By` lines, no robot emoji, no "Generated with" footers.

## See also

- `docs/general/build-and-run.md` — authoritative build doc (says "We removed JVM deps; project is Kotlin Multiplatform (native + JS)")
- `docs/general/overview.md` and `docs/general/philosophy-and-design.md` — describe bit-exact behavior "across JVM, JS, and Native"; that phrasing is **about C semantics being reproducible** in any host's Kotlin compiler, **not** a statement that klang ships a JVM target. The build doesn't and won't.

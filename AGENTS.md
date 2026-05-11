# Claude Code Project Instructions — klang

## Hard rule: **no `import java.*` lines.**

That is the rule. Source files under `src/` do not `import java.*` and do not `import javax.*`. If you need a thing the JVM provides via `java.*`, find a multiplatform equivalent (`kotlinx-io`, `kotlinx-datetime`, etc.) or call into Kotlin stdlib functions that resolve to JVM-side `java.lang` symbols at use sites without a `java.*` import (`System.getenv`, etc.).

That is **the entire JVM-relevant rule** for klang's source. There is no separate prohibition on having a `jvm()` target, an Android KMP target, an `androidLibrary { ... }` block, a `jvmMain` source set, a `kotlin.jvm.*` annotation, or anything else of that shape. If you want one of those, add it. Just don't `import java.*`.

## Template

starlark-kotlin is the workspace template for klang-shaped projects, generally — though it's currently lagging behind the canonical 8-target set the rest of the workspace uses. When in doubt about build wiring (targets, source-set hierarchy, plugin versions, gradle config), look at starlark-kotlin first.

## Commit messages

- No AI branding or attribution.
- Clear, descriptive, focused on what changed and why.
- No `Co-Authored-By` lines, no robot emoji, no "Generated with" footers.

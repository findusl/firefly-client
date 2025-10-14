# `checkAgentsEnvironment` task runtime

## Measurement approach
- Commands were executed from a clean checkout using the Gradle wrapper with the plain console to capture every task line.
- Wall-clock time was collected via the shell's `time` builtin for three scenarios: a first run after dependency downloads, a warm run reusing the configuration cache, and a repeat run with `--rerun-tasks` plus `--profile` to force work without re-downloading artifacts.

## Execution times
| Scenario | Command | Elapsed time | Notes |
| --- | --- | --- | --- |
| Cold run with caches populated | `time ./gradlew checkAgentsEnvironment --console=plain` | `real 2m25.598s` | Gradle executed 37 tasks and stored a configuration cache entry. |
| Warm run | `time ./gradlew checkAgentsEnvironment --console=plain` | `real 0m2.789s` | Configuration cache hit; only one task executed. |
| Forced re-execution | `time ./gradlew checkAgentsEnvironment --console=plain --profile --rerun-tasks` | `real 0m29.971s` | Generates a profiling report showing per-task cost. |

The very first invocation also downloaded the Gradle 8.14.3 distribution before any build logic executed, which adds additional minutes when the wrapper cache is empty.【2e791f†L1-L118】【1dc617†L1-L144】【38137d†L1-L8】【54672d†L2-L4】【9225ca†L1-L2】

## Download footprint
Caching the Gradle user home keeps the bulk of the startup cost out of repeat builds:

- Gradle distribution downloaded by the wrapper: 146 MiB.【154d59†L1-L3】
- Maven dependencies in `~/.gradle/caches/modules-2`: 346 MiB overall, with 330 MiB under `files-2.1`.【78e5da†L1-L3】【66e1aa†L1-L2】
- Largest modules downloaded during the run include Kotlin compiler/tooling (≈155 MiB), Skiko, Compose, and Ktor artifacts, so trimming Kotlin/Compose dependencies would yield the biggest savings rather than Java modules alone.【dcba40†L1-L11】【70a3a9†L1-L10】

## Work performed per run
The profiling run shows the tasks that dominate the forced rebuild. Times below come from the generated profile report.

| Task | Duration |
| --- | --- |
| `:composeApp:compileKotlinJvm` | 22.877 s |
| `:composeApp:runKtlintCheckOverAndroidMainSourceSet` | 13.746 s |
| `:composeApp:runKtlintCheckOverCommonMainSourceSet` | 5.992 s |
| `:composeApp:compileTestKotlinJvm` | 2.341 s |
| `:composeApp:jvmNonUiTest` | 2.235 s |
| Other individual `ktlint` source-set checks | <1 s each (cumulatively ≈6 s) |

_Total build time_ for the profiling run was 29.508 s, with the sum of task durations at 1 m 11.30 s because many tasks run sequentially and block on ktlint invocations.【dd63a0†L45-L71】【16b8e7†L138-L220】

## Improvement opportunities
1. **Persist Gradle caches between machines** – The cold run still depends on 146 MiB of wrapper binaries plus 300 MiB of Maven artifacts. Cache `~/.gradle/wrapper` and `~/.gradle/caches` (or configure a remote build cache) in CI to avoid re-downloading on ephemeral agents.【154d59†L1-L3】【78e5da†L1-L3】【66e1aa†L1-L2】
2. **Warm the build before running checks** – Executing `./gradlew checkAgentsEnvironment --rerun-tasks --profile` once during image preparation fetches all dependencies and compiles Kotlin so later runs hit the configuration/build cache and fall to ~3 s.【1dc617†L1-L144】【38137d†L1-L8】
3. **Target ktlint to the JVM source sets** – ktlint invocations account for ~20 s of the forced rebuild despite many Android/iOS source-set tasks reporting `NO-SOURCE`. Narrow the Gradle plugin filter to the source sets exercised by `jvmNonUiTest` (e.g., common/jvm) or disable redundant per-platform tasks so ktlint runs once per relevant directory instead of per target.【16b8e7†L144-L211】
4. **Audit optional UI/preview dependencies** – Compose preview components, desktop UI tooling, and UI test libraries live in `commonMain`/`jvmTest` but the `jvmNonUiTest` suite excludes UI tests. Moving preview/test-only libraries behind debug/test-specific source sets would reduce the 155 MiB Kotlin toolchain and 14 MiB Skiko downloads if those artifacts are unnecessary for headless environments.【dcba40†L1-L11】【70a3a9†L1-L10】
5. **Share compiled outputs through the build cache** – `:composeApp:compileKotlinJvm` is the single longest task. Ensuring `org.gradle.caching=true` (already set) plus a shared remote cache would let CI agents reuse compiled classes instead of recompiling on every fresh machine.【16b8e7†L138-L166】【a8e458†L1-L8】

Because the heavy downloads stem from Kotlin/Compose Gradle plugins rather than Java runtime modules, switching to Java modules alone will not significantly shrink the initial download; trimming Kotlin/Compose dependencies or caching their artifacts yields a larger payoff.【dcba40†L1-L11】【70a3a9†L1-L10】

# Agent instructions (Team Sneakymouse / Lords of Minecraft)

This repo is the **DippGen** Paper plugin (Kotlin, Paper 26.2, Gradle): assorted server utilities. Plots and build protection moved to SneakyPlots. All coding agents (Cursor, Claude Code, Codex, Antigravity, Hermes, etc.) should follow this file.

<!-- BEGIN TEAM-POLICY MANAGED BOOTSTRAP -->
This project is enrolled in Team-Sneakymouse development policy via a sibling checkout (see `.team-policy/pointer.json`).

Resolve the policy root: try each `local_candidates` path in the pointer relative to this project root. If none exist, `git clone` the pointer `remote` into the first candidate using the developer's existing Git credentials. If a checkout exists but is behind `origin/<track_branch>`, `git fetch` and fast-forward that branch when safe; if local edits block the update, report and ask.

Before implementing, review `<policy-root>/README.md` as the current policy index and load the linked guidance relevant to the task. At minimum, when authorization or risk is unclear, read `<policy-root>/policy/core.md`; read `<policy-root>/policy/scope-and-testing.md` when classifying risk or changing behavior; and read `<policy-root>/workflows/development.md` for nontrivial multi-step work. For substantial Paper features, persistent world state, or changes spanning components, read `<policy-root>/policy/paper-plugin-maintainability.md`. Read `<policy-root>/policy/sneakyutils.md` when adding or sharing Paper util/manager code. Load optional skills from `<policy-root>/skills/` only when relevant.

Do not substitute remembered rules, a web mirror, or an unlisted path. Follow local build/architecture facts and explicit exceptions below. When host instructions and team policy repeat the same norm, follow the host wording. If they require incompatible actions, surface the conflict before expanding work. Distinguish running checks from implementing tests; a source edit does not automatically require a new test.
<!-- END TEAM-POLICY MANAGED BOOTSTRAP -->

## Project facts

| | |
|--|--|
| Project | DippGen (Paper plugin) |
| Runtime targets | Paper API 26.2, Java/Kotlin JVM 25 |
| Plugin descriptor | Bukkit `plugin.yml` (commands declared there; `AutoRegistrar` wires every `ICommandImpl` in `Commands/` and every `Listener` in `Events/`) |
| Server dependencies | WorldGuard (required), PlaceholderAPI (optional) |
| Shared Paper utils | Not using SneakyUtils yet; read team policy `sneakyutils.md` before adding shared util/manager code |
| Deployment | push to `main` triggers publish via `.github/workflows/publish.yml` |
| Build output | `~/.cache/dippgen-gradle-build` (keeps Gradle off FUSE/NTFS checkouts) |

Custom item look, CraftEngine/pack model id, server resource pack, or ItemDisplay place/orient → read `<policy-root>/policy/paper-items.md`.

## Commands

| Task | Command |
|------|---------|
| Build + unit tests + publish preflight | `./gradlew check` |
| Publish preflight only | `./gradlew publishPreflight` or `bash scripts/publish-preflight.sh` |
| Unit tests only | `./gradlew test` |
| Local Paper server | `./gradlew runServer` (downloads WorldEdit + WorldGuard from Modrinth) |
| Publish (CI) | `./gradlew publishMavenPublicationToSneakyrpRepository` (requires Maven credentials) |

Builds require a **git checkout** with at least one commit (version is derived from `HEAD`).

## Layout

```text
src/main/kotlin/com/danidipp/dippgen/
  Core.kt                    # Plugin class (bootstrap, config parsing)
  AutoRegistrar.kt           # Reflection-based command/listener registration
  Commands/                  # ICommandImpl per command; name must be declared in plugin.yml
  Events/                    # Listeners, auto-registered
src/test/kotlin/com/danidipp/dippgen/   # JUnit 5 tests
```

When adding a command, declare it in `plugin.yml`; `PluginYmlTest` fails otherwise. New logic that doesn't need Bukkit should live outside listeners and command executors so it can be unit-tested with plain JUnit.

## Testing

Run `./gradlew check` before finishing. Tests are plain **JUnit 5** (no MockBukkit yet). Gameplay changes need a manual **`runServer`** smoke test. Do not rely on `runServer` as the only verification for logic that can be unit-tested.

Team-wide Paper rules (threads, reload, public hooks, stable ids, docs) live in `<policy-root>/policy/paper-conventions.md`.

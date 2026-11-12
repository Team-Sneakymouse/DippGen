# Agent instructions (Team Sneakymouse / Lords of Minecraft)

This repo is the **DippGen** Paper plugin (Kotlin, Paper 26.2, Gradle): assorted server utilities. Plots and build protection moved to SneakyPlots. All coding agents (Cursor, Claude Code, Codex, Antigravity, Hermes, etc.) should follow this file.

<!-- BEGIN TEAM-POLICY MANAGED BOOTSTRAP -->
This project follows the Team-Sneakymouse development policy, kept in a sibling checkout that `.team-policy/pointer.json` names.

The policy root is the first `local_candidates` path in the pointer, resolved from this project's root, that exists. If none exists, clone the pointer's `remote` into the first candidate with the developer's Git credentials and say so. Before implementation work (not for read-only investigation), run `git fetch` in the checkout, then fast-forward it to `origin/<track_branch>` if it is behind and clean; if the fetch fails or local changes block the fast-forward, report it and ask.

Before implementing, read `<policy-root>/README.md`. It is the policy index and says which documents apply to which kind of change. If the policy root can't be resolved or read, stop and report that instead of working from memory or another copy.

Build commands, architecture facts and recorded local exceptions in this file are authoritative for this repository. Where this file and the policy require incompatible actions, say so before expanding the work. Running checks and writing tests are separate decisions: a source edit does not by itself require a new test.
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

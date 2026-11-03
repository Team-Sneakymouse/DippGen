# DippGen

Paper plugin for Lords of Minecraft: WorldGuard plot management (claiming, deeds, members, plot chests), homestones, timed block replacement, autopickup, book spying and other server utilities.

Requires **WorldGuard** on the server; **PlaceholderAPI** is optional (registers the `dipp` expansion).

## Development

| Task | Command |
|------|---------|
| Build, unit tests and publish preflight | `./gradlew check` |
| Unit tests only | `./gradlew test` |
| Local Paper 26.2 server | `./gradlew runServer` |

Builds need a git checkout with at least one commit: the version is `yyyy.MM.dd-<12-char-git-hash>` of `HEAD`.
Build output goes to `~/.cache/dippgen-gradle-build`, which keeps Gradle off slow FUSE/NTFS checkouts.

`runServer` downloads WorldEdit and WorldGuard from Modrinth into `run/plugins`.

Coding agents follow [AGENTS.md](./AGENTS.md).

## Publishing

Every push to `main` publishes `io.github.team-sneakymouse:dippgen:<version>` to `https://maven.sneakyrp.com/releases`
through the shared [`Team-Sneakymouse/ci`](https://github.com/Team-Sneakymouse/ci) workflow
(`.github/workflows/publish.yml`). Pull requests are built and tested by `.github/workflows/ci.yml`.
The repository must be allowlisted for Infisical OIDC; private repositories also need the
`INFISICAL_IDENTITY_ID` and `INFISICAL_DOMAIN` secrets.

# Frequency Works — Minecraft 1.21.1 / NeoForge source

**Alpha A4 Security Source — SOURCE ONLY. Not an installable or verified `.jar`.** Target: NeoForge 21.1.265 / ATM10 8.4.

A3 introduced: **interactive Transfer Buffer inventory actions** (deposit one/stack from a selected player inventory slot, withdraw one/stack from a chosen frequency item row), stable item entry identities for safe sorting changes, deterministic inventory capacity checks, and adaptive GUI row pagination. The Port and frequency logic from A2 remains.

A4 adds private Buffer break/pickup/use restrictions and tier-specific explosion drops and immunity. Read [A4 security notes](docs/ALPHA_A4_SECURITY.md), including its limitations.

Read [Alpha A3 milestone notes](docs/ALPHA_A3_INVENTORY.md) for implementation and test details, and [the technical design specification](docs/DESIGN_SPEC.md) for the full intended gameplay.

## Local tests (Java 21)

```sh
bash scripts/test-core.sh
bash scripts/test-bridge.sh
bash scripts/test-buffer-gui.sh
bash scripts/test-security.sh
```

To make a genuine mod `.jar`, the project still needs an internet-connected Java 21 + Gradle 8.10.2 environment to run `gradle build`. A GitHub Actions build workflow is included, but it **has not run**; all Minecraft-facing code remains compile- and gameplay-unverified.

**Do not install this source ZIP into Minecraft's `mods` folder.**


## Online build

A repeatable GitHub Actions build and failure-log upload are included. See [BUILD_ON_GITHUB.md](docs/BUILD_ON_GITHUB.md) and [BUILD_PREP_NOTES.md](docs/BUILD_PREP_NOTES.md). The online build has NOT run; do not assume this project compiles or is safe for an existing world.

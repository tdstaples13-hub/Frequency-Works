# Frequency Works — development status

**Alpha source milestone A1:** 2026-10-31 / 2026-11-01. Source-only, **not a compiled `.jar`**, and **not yet tested in Minecraft**.

This is the next coding increment after the original Phase 0 scaffold, not a completed version of the mod.

## Newly coded

- `FrequencySavedData`: a server-wide inventory, saved to the Overworld and accessible cross-dimension without requiring a placed Buffer. Supports three-digit public/private frequency keys, permanent highest-ever tier, per-item counts above normal stack sizes, NeoForge item-data-component-preserving item serialization, and four sorting modes.
- `TransferBufferBlock` / `TransferBufferBlockEntity` / `TransferBufferItem`: permanent binding, Tier I–V registration, retention on normal player mining, restore upon placement, held-item inspection, private-owner checks and capacity activation.
- `TransferPortBlock` / `TransferPortBlockEntity`: fresh/unbound placement, clean ordinary 64-stackable dropped items, persisted config *only while placed*, target block attachment facing, six-sided capability lookup, independent tick scheduling, capped 4 transfer attempts, idle backoff.
- `PortTransferService`: real sided `IItemHandler` input/output using simulation for space checks, stack batching and actual moved-count tracking. No item entities.
- Temporary `/fw` debug commands for binding and configuring the above, until the requested in-game menus are implemented.
- Test-only fake inventory classes for a JDK-only integration test of the **real** `PortTransferService` Java source. These classes are in `src/testHarness` and are *not* packaged in the mod.

## Temporary alpha commands (for developers/playtesters, after compiling)

Look at a placed Transfer Buffer, or **hold** an unbound Buffer in your main hand:

`/fw buffer 042 public` — creates public frequency 042 and permanently binds the Buffer.

`/fw buffer 042 private` — creates your own private frequency 042 and binds the Buffer.

When connecting to an *existing* frequency, repeat with `confirm` at the end (example: `/fw buffer 042 public confirm`).

Look at a newly placed Transfer Port:

`/fw port 042 public` — connects to an existing public frequency.

`/fw port 042 private` — connects to an existing private frequency owned by you.

Configure its attached machine's sided inventory (machine→frequency is **input**; frequency→machine is **output**):

`/fw face up input`

`/fw face down output`

`/fw face north none`

`/fw mode stack` or `/fw mode single`

Look at an assigned Buffer (or hold it):

`/fw sort quantity_asc` / `/fw sort quantity_desc` / `/fw sort alphabetical` / `/fw sort mod_alphabetical`

`/fw info` — basic frequency or Port status.

**IMPORTANT:** All Port faces start in `NONE`. Bidirectional same-face transfers are intentionally blocked in this milestone because individual input/output filters and loop protections are not implemented. Other modules (Item Sequencer, Monitor) are still placeholders.

## Existing known limitations / testing needed

- **No menus yet.** Frequency and Port configuration is temporarily by debug commands. Buffer inventory browsing is text-only, not a GUI.
- **No item-filter logic yet.** Input and Output filter banks, tag cards and dual-direction conflict handling await the next milestone.
- **No complete upgrade recipes for Buffers II–V yet.** Custom crafting must preserve the Buffer's bound frequency (normal shaped JSON recipes cannot safely do this).
- **Block-item data edge cases:** Normal player mining restores the Buffer binding. Explosions, non-player breaking tools, and nonstandard modded block-breaking methods are not yet tested; do not use valuable storage during early tests.
- **Third-party inventory compatibility:** The code expects NeoForge `IItemHandler` contracts to be obeyed. Handler mismatches and save/reload edge cases still require tests; this is not a release-ready loss/duplication guarantee.
- **No NeoForge build or actual Minecraft run.** The environment has Java 21 but neither Gradle/Minecraft dependency downloads nor an ATM10 game instance. These Minecraft-facing classes are therefore **not yet API-compile-verified**.
- Placeholders have simple cube models and are not final textures or tiny attachment shapes.

## Validated here

`bash scripts/test-core.sh` — passed: identity isolation, tier promotion, hopper timing, frequency ordering, shared-hopper behavior.

`bash scripts/test-bridge.sh` — passed: input/output transfers, simultaneous independent access to one frequency, partially full/full destinations with standard inventory-handler behavior.

JSON resources — syntax validated. Actual Minecraft resource loading not yet validated.

## Next technical milestone

1. Build under official NeoForge 1.21.1 ModDevGradle and fix any API mappings/version issues.
2. Run on a **copy** of ATM10; verify basic transfers/save-reload in a disposable world.
3. Implement in-game frequency and Port settings screens (including two filter banks and Tag Cards) and proper Buffer item upgrade crafting.
4. Only then benchmark high-tier throughput and continue with the Item Sequencer.

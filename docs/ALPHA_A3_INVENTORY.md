# Alpha A3 — Interactive Buffer inventory milestone

**Status: SOURCE CHECKPOINT, NOT A PLAYTESTABLE MOD.** Tested against local Java stubs, *not* Minecraft/NeoForge. The project needs a successful NeoForge compile and an ATM10 test before any `.jar` can be called usable.

## What was implemented

- Previously **view-only Transfer Buffer screen** can now withdraw one item or up to a normal vanilla stack (64 maximum per click). The actual stack limit of the item is respected.
- Player can select an inventory slot (0–35) in the Buffer screen and deposit one item or its stack, including from a held Buffer's UI when an appropriate other slot contains the item.
- Successful actions update the server's saved frequency inventory and the player's inventory. The existing private-frequency and tier access checks apply to all actions.
- **Stale selection protection:** Each distinct stored item entry has an internally assigned persistent ID. GUI clicks reference that ID, not their current row, to avoid withdrawing a different item when a connected Port changes sorting or storage contents.
- Withdrawal first calculates the available storage in the player's 36 inventory slots, then moves only what fits. A full inventory blocks withdrawal; there is no item dropping.
- Deposits cannot exceed frequency capacity. All operations occur on the logical server thread; no extra ticking or inventory polling is needed.
- Buffer UI adapts its number of displayed rows to the available Minecraft screen height; navigation pages through the shared inventory.
- Port filtering, owner checks, transfer caps, and clean-stack Port drops are unchanged.

## What the first A3 GUI looks like

The GUI remains a **basic text-and-button prototype**, NOT a full Refined-Storage-style slot menu. On a bound Buffer screen, use `1` or `64` on the desired storage row to withdraw. Select a player inventory slot with `<` and `>` and use `Store 1` or `Store Stack` to deposit. Player inventory slot selection is a deliberately temporary UX; drag-and-drop and item icons are not implemented.

## Local validation

From the project directory with Java 21:

```bash
bash scripts/test-core.sh
bash scripts/test-bridge.sh
bash scripts/test-buffer-gui.sh
```

`test-buffer-gui.sh` compiles and executes the **actual** `BufferInventoryActions.java` against small stand-in Minecraft classes to test deposit/withdraw arithmetic and rejected operations. These tests are useful, but **do not validate NeoForge API compatibility**.

## How to compile in an Internet-enabled development environment

Option A: Install Gradle 8.10.2 and Java 21, and run `gradle build` from the project directory. Minecraft and NeoForge dependencies must be downloaded from public repositories.

Option B: Push the project root to a GitHub repository and run the provided `Build NeoForge mod` GitHub Actions workflow. Its output will be in the action's **Artifacts** section if the build succeeds. It may still report compilation errors that require code changes. The workflow **has not been executed or verified** in this environment.

## Important remaining blockers

1. Full **NeoForge 1.21.1 compilation** is not verifiable in this container because the dependency repositories and Gradle distribution are unreachable.
2. **ATM10 playtesting** must verify item-handling compatibility, multiplayer packet flows, all block lifecycle events, save/restart and dropped-item persistence.
3. The proper item-grid GUI, crafting upgrades preserving bound Buffer data, Sequencer execution, and Frequency Monitor are still outstanding.
4. Do not use the prototype in an important save until item preservation has been tested in-game. Keep backups.

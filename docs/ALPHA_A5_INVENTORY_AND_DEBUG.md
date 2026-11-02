# Frequency Works Alpha A5 — Inventory UI replacement and debug reset

## New GUI

Bound Transfer Buffers now open a real `AbstractContainerMenu` + `AbstractContainerScreen`, replacing the temporary text-and-button view. The new GUI displays 45 real Minecraft item-icon cells per page (9 columns × 5 rows), with a scroll/page arrow pair, a sorting button, and the normal 27-slot inventory and nine-slot hotbar beneath. Unassigned Buffers still open the frequency-selection screen. Buffer items work when right-clicked in air as well as when placed.

* **Left click storage item with empty cursor:** withdraw up to the item's normal stack size.
* **Right click storage item with empty cursor:** withdraw one.
* **Left/right click with matching item carried:** insert stack / one.
* **Click an empty storage cell while carrying an item:** deposit into the shared frequency (up to capacity).
* **Shift-click storage cell:** withdraw into the player's inventory.
* **Shift-click player inventory slot:** insert its stack into the frequency.
* **Sort:** cycle the four previously approved frequency sorting modes.
* **Page arrows:** browse all 27/36/45/54/81 storage slots depending on the shared tier.

**Safety:** Storage slots are server-authoritative virtual views of the saved frequency. Real item quantities stay as `long` counts rather than oversized vanilla `ItemStack`s. The client receives ordinary item-icon slot updates and a bounded packet of 45 actual quantities. Unsupported clone, drag, and double-click interactions on virtual storage slots are ignored to avoid duplication; normal inventory slots remain standard. This alpha is a first functional slot GUI, not yet a perfect reproduction of every Dank Storage shortcut. Always test with disposable items first.

## Creative Frequency Debugger

No recipe or normal creative-tab entry; use `/give @s frequencyworks:frequency_debugger` in a cheats-enabled creative test world. Using the item requires **both** creative mode and operator permission level 2 or higher, enforced on the server.

Right-click air with the item, choose Public or your own Private scope, enter `000`–`999`, and click `Delete...`. A second explicit, server-verified confirmation is required. Confirmation expires after 600 game ticks (30 seconds). Deletion irrevocably destroys the chosen frequency's inventory and permanently increments its persistent generation number.

**Reset semantics:** Bound physical Buffers and portable Buffer items on a deleted frequency are invalidated immediately by generation. On their next placement, opening, or use, they are cleared to unassigned. Unloaded chunks and offline inventories therefore cannot silently restore the deleted network. Already placed blocks need not be force-loaded to reset. Debugging this way **does not remove connected Transfer Ports**; old Ports should be removed/reconfigured before reusing a deleted frequency ID.

**Scope:** The debugger can erase any Public frequency or the acting player's own Private frequency. It does not target someone else's Private UUID.

## Build and test

Run the included `scripts/test-*.sh` and `scripts/check-resources.py` locally. **The full NeoForge compiler is only available in the GitHub Actions workflow for this project**. Submit the updated source to the repo and inspect the `frequencyworks-build-log` artifact if a red X appears. Do not test against an existing world until compilation and disposable-world tests pass.

### First in-game checks

1. Back up/create a clean test world. Place Tier I Buffer, bind Public `042`, reopen.
2. Verify the grid, inventory and hotbar are all sharp and readable.
3. Left-click, right-click and Shift-click cobblestone into/out of the frequency.
4. Verify normal item icons, right-click single item handling and sorting.
5. Use `/give @s frequencyworks:frequency_debugger`, select Public `042`, delete and confirm.
6. Reopen previously linked Buffer and verify that it now requests a fresh frequency.
7. Re-create Public `042` and verify its former contents are **not** restored.

The code has not yet been compiled/verified against Minecraft 1.21.1 in this environment.

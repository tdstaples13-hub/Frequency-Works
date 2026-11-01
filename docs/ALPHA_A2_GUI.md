# Alpha A2 — Configuration GUI development milestone

**Build status:** source only; NeoForge compilation and live ATM10 testing NOT completed.

Target: **Minecraft Java 1.21.1**, **NeoForge 21.1.265** (ATM10 8.4).

## New source code

- `client/ConfigScreen.java` and `client/ConfigClient.java`: prototype Minecraft GUI screens using simple text/buttons (no texture or inventory-grid interaction yet).
- `network/ConfigNetworking.java`: registered NeoForge clientbound snapshots and serverbound configuration requests. Server resolves player identity; checks block proximity and existing frequency ownership before changing settings. Do not accept a client-supplied owner UUID.
- Transfer Buffers can initiate the screen from a placed block or held item. Unbound buffers choose 000–999 and Public/Private; they display a warning before joining existing networks. Bound buffers can view a *text-based, view-only* item inventory and cycle the four sorting modes. Normal Buffer frequency/tier restrictions still apply.
- Transfer Ports now present a setup screen on placement and on interaction, require an existing frequency, configure six machine access faces (None/Input/Output/Both), and select full-stack vs individual mode.
- Input and Output are **independent whole-Port filter banks**, with the existing tier's filter capacity in **each** bank: 1/2/4/9/20. Each bank has a separate Whitelist/Blacklist mode. Empty filters permit all items.
- Each filter entry can contain an exact registry item ID like `minecraft:iron_ingot` or a tag prefixed with `#`, such as `#minecraft:logs`. An installed `TagCardItem` can also be configured in its own handheld screen and applied to a selected Port filter slot while held in the other hand.
- When opposite directions are enabled simultaneously, the Port validates that no currently registered item ID is permitted by both filter banks; overlapping settings are rejected. This validation runs on configuration changes, not per tick.
- The live `PortTransferService` now receives predicates for input and output directions; filter checks are part of existing bounded attempts. Source entity is not polled when all faces are None.

## What the player would click (once successfully built)

1. Place a Tier I Transfer Buffer or right-click it in hand. Choose a three-digit frequency, select Private or Public, and confirm. If the frequency already exists, a second confirmation is required.
2. Place a Transfer Port on a compatible block. Choose the *existing* frequency and scope. Set machine faces to input (machine -> frequency) or output (frequency -> machine).
3. Cycle the Port to stack or individual mode and choose a bank. Select a filter slot and enter an item ID or a `#tag`, then click Set. Repeat for the other bank. Use the whitelist/blacklist button as needed.
4. Optional: hold a Tag Card, right-click air, give it a tag. Hold the configured card in the offhand while using the Port GUI, then click **Use held Tag Card** for the current filter slot.
5. Right-click the bound Buffer to inspect the frequency contents and select sorting mode.

## Important limitations

- **Not compile-verified**: Minecraft/NeoForge dependencies, Gradle and an ATM10 client are unavailable in this environment. Expected API adjustments may be required in a real MDK build.
- **Buffer inventory is view-only** in this milestone, using a text listing. Player deposit/withdraw and inventory-grid interactions are not built yet.
- Filter selection uses item IDs and tag names rather than item-icon ghost slots or drag-and-drop from JEI. Tag Card entry is a textual tag name until that UI is upgraded.
- Auto-opening a screen immediately on block placement is implemented in source but not tested against multiplayer timing or NeoForge interaction lifecycle.
- No live Minecraft integration tests or performance benchmarks. Core Java tests and fake-handler bridge tests still pass, but they do not test the GUI or Tag Card.
- Avoid using any experimental compiled version in an important world until cross-mod item transfer and save/reload behavior have been tested.
- Other outstanding features remain: full Buffer inventory UI, Buffer II-V dynamic upgrade crafting, fancy Port attachments, Item Sequencer, Sequence Tablets, Interaction/Redstone cards, Frequency Monitor, and real ATM10 playtesting.

## Third-party documentation consulted

- NeoForge 1.21.1 Screens: https://docs.neoforged.net/docs/1.21.1/gui/screens/
- NeoForge 1.21.1 Registering Payloads: https://docs.neoforged.net/docs/1.21.1/networking/payload/
- ATM10 8.4 version and NeoForge requirement: https://www.curseforge.com/minecraft/modpacks/all-the-mods-10/files/9366926

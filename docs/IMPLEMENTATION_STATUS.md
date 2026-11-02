# Frequency Works — implementation status (Alpha A6E)

## Implemented for integration testing

- Five shared-frequency Transfer Buffer tiers and five Transfer Port tiers, with the existing A5H interfaces.
- Item Sequencer: a compact block with exactly nine actual Tablet slots, server-authoritative 2-tick instruction processing, a directly-underneath Host inventory, 16-block linked-face range, two-way transfer, partial counts and persisted progress.
- Sequence Tablet: nine ghost instruction cells with selected item or configured Tag Card; link faces by shift-right-click, cycle send/receive, counts (1–4096), whitelist/blacklist, and tablet failure policies.
- Redstone Card instructions: powered, unpowered, rising, falling edge gates.
- Save/reload runtime progress, emergency refund preservation, tablet drops on block break, and sneak-right-click restart for Stop-on-failure.
- Legacy Buffer, Port, debugger, frequencies and crafting recipes retained.

## Not yet implemented

- Interaction Card tool-equipped fake-player use and tool durability handling are not yet implemented. Empty-hand actions are coded, but must be tested with real modded machinery.
- Per-face client outline/highlights while linking tablets.
- JEI/EMI ingredient drag-and-drop integration; ghost steps currently use normal cursor selection.
- Redstone Card has power thresholds 1–15 and the four signal/edge modes, but gameplay verification remains.
- End-to-end client playtest and real modded-machine compatibility; GitHub compilation alone is not gameplay verification.

## Next

Run the Java 21 NeoForge GitHub Action, fix any compiler errors, then test in a disposable ATM10 world. Do not promote this dev branch to main until gameplay is verified.

- Placed Transfer Buffers now expose a standard NeoForge item handler for automation, including Sequencer Host usage.
- Sequencers cache idle programs and Redstone Card supports configured thresholds 1–15.

- A6B UI revision planned: compact 1×9 Tablet selectors, 1×9 ghost filters, 1×9 per-step whitelist/blacklist toggles; native player inventory below. Quantity arrows appear only on ordinary item ghosts; Shift changes 10 at a time. All screen panels explicitly render before widgets/slots.

- Alpha A6B UI refinement: user-provided colored squares were a layout legend, not a visual palette. All Tablet controls now use neutral Minecraft-style grays. Removed inherited world blur from Tablet and Sequencer screens; cards reset their hidden count to 1 and never display quantity arrows.

- Alpha A6D: remove the always-visible resume arrow and extra gray right sidebar from the nine-Tablet Sequencer inventory. Standard panel is 176 pixels wide. To resume a genuinely stopped Sequencer, sneak-right-click its block with an empty hand; regular right-click still opens the inventory. No permanent UI control is shown.

- Alpha A6E: Interaction Card top-row button cycles Right-click / Sneak-right-click / Punch / Mine; the bottom control selects the target face independently of item import/export direction. Server-side fake-player block actions are implemented with permission checks and a 10-tick cooldown; in-game behavior has not been verified.

# Frequency Works — implementation status (Alpha A6A)

## Implemented for integration testing

- Five shared-frequency Transfer Buffer tiers and five Transfer Port tiers, with the existing A5H interfaces.
- Item Sequencer: a compact block with exactly nine actual Tablet slots, server-authoritative 2-tick instruction processing, a directly-underneath Host inventory, 16-block linked-face range, two-way transfer, partial counts and persisted progress.
- Sequence Tablet: nine ghost instruction cells with selected item or configured Tag Card; link faces by shift-right-click, cycle send/receive, counts (1–4096), whitelist/blacklist, and tablet failure policies.
- Redstone Card instructions: powered, unpowered, rising, falling edge gates.
- Save/reload runtime progress, emergency refund preservation, tablet drops on block break, and a manual resume button for Stop-on-failure.
- Legacy Buffer, Port, debugger, frequencies and crafting recipes retained.

## Not yet implemented

- Interaction Card fake-player use/sneak-use/left-click semantics. Card can be placed as a step, but intentionally fails safely rather than taking unintended actions.
- Per-face client outline/highlights while linking tablets.
- JEI/EMI ingredient drag-and-drop integration; ghost steps currently use normal cursor selection.
- More complex UI controls such as threshold 1–15 for Redstone Card; current A6A handles four boolean/edge modes.
- End-to-end client playtest and real modded-machine compatibility; GitHub compilation alone is not gameplay verification.

## Next

Run the Java 21 NeoForge GitHub Action, fix any compiler errors, then test in a disposable ATM10 world. Do not promote this dev branch to main until gameplay is verified.

- Placed Transfer Buffers now expose a standard NeoForge item handler for automation, including Sequencer Host usage.
- Sequencers cache idle programs and Redstone Card supports configured thresholds 1–15.

- A6B UI revision planned: compact 1×9 Tablet selectors, 1×9 ghost filters, 1×9 per-step whitelist/blacklist toggles; native player inventory below. Quantity arrows appear only on ordinary item ghosts; Shift changes 10 at a time. All screen panels explicitly render before widgets/slots.

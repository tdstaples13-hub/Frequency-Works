# Frequency Works Alpha A5C — Scrollable Transfer Buffer storage

This is a focused update **on top of Alpha A5B**, not a completed mod release.

## Changes

- Removes the two inventory paging buttons.
- Tier I: 27 slots (3 rows); Tier II: 36 slots (4 rows); Tier III: 45 slots (5 rows). All visible at once.
- Tier IV: 54 total slots; Tier V: 81 total slots. Both show 45 slots (5 rows) at a time, scrolling **one 9-slot row** with the mouse wheel or by dragging the scrollbar.
- Player inventory and hotbar stay in place below the storage grid.
- Virtual slots and quantity overlays follow the scrolling window; server handles all scroll requests and item transfers.
- Sorting is unchanged, with its own dedicated row above the grid.
- Actual capacity and per-slot quantity limits are unchanged.

## Current alpha feature status

- **Transfer Buffers**: assigned storage and inventory GUI implemented. Custom large quantities and shared frequencies coded; not yet proven lossless under real ATM10 multiplayer / modded machines.
- **Transfer Ports**: assigned frequency, sided NeoForge transfer, speed scheduling, and two configured filter banks coded. Still requires thorough in-game tests and final small attachment models.
- **Tag Card**: item tag configuration and Port filter use coded. It does not operate as a Sequencer instruction yet.
- **Creative Frequency Debugger**: destructive deletion, two-step confirmation, and binding invalidation coded; game testing still needed.
- **Item Sequencer**: placeholder block and item, no functional instruction executor or tablet UI yet.
- **Sequence Tablet**: placeholder item, no instruction storage or face-selection yet.
- **Interaction Card / Redstone Card**: placeholder items; not functional instructions yet.
- **Frequency Monitor**: placeholder block, no view-only network UI yet.

No feature has been declared production-ready. Use a disposable world. Full NeoForge compilation and client testing must happen via the GitHub workflow and your test instance.

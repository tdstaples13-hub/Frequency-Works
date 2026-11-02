# Frequency Works — implementation status (Alpha A5D)

See `docs/ALPHA_A5D_VANILLA_INVENTORY.md` and `docs/ALPHA_A5C_SCROLLING.md`.

## Implemented for testing

- Five Transfer Buffer tiers with shared frequency storage, bindings and capacity logic.
- Vanilla chest-style player inventory, hotbar and inventory shortcuts below a scrollable 9-column Buffer storage grid. Custom code is restricted to long-count frequency storage operations, sorting and scrolling.
- Five Transfer Port tiers with autonomous item-handler transfers and configurable input/output filters, including configured Tag Cards.
- Creative operator-only Frequency Debugger with two-step frequency deletion and invalidation of old Buffer bindings.
- Server-side frequency persistence and basic standalone test harnesses.

## Incomplete

- Item Sequencer execution, Sequence Tablet programming, Interaction Card and Redstone Card instructions.
- Frequency Monitor UI and behavior.
- Final models, textures, crafting/upgrade edge cases, modpack compatibility and comprehensive in-game verification.

## Build and test status

A5C previously compiled on GitHub. A5D must pass GitHub Actions before a `.jar` is released; gameplay is not validated by compilation alone. Use a disposable ATM10 test world.

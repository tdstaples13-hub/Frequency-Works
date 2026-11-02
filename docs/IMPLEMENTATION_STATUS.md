# Frequency Works — implementation status (Alpha A5C)

Refer to `docs/ALPHA_A5C_SCROLLING.md` for the current release-specific feature matrix.

## Implemented source components

- Five Transfer Buffer tiers with shared frequency storage, bindings and capacity logic.
- Minecraft inventory menu with a 9-column storage grid and normal player inventory; IV/V use row-by-row scrolling (mouse wheel and draggable scrollbar).
- Five Transfer Port tiers with autonomous item-handler transfers and configurable input/output filters, including configured Tag Card tag rules.
- Creative operator-only Frequency Debugger with two-step frequency deletion and invalidation of old Buffer bindings.
- Server-side frequency storage, saved generations, and basic standalone test harnesses.

## Incomplete / nonfunctional as of A5C

- Item Sequencer execution, Sequence Tablet programming, Interaction Card instruction logic, Redstone Card instruction logic.
- View-only Frequency Monitor UI and behavior.
- Final textures, models, crafting/upgrade edge cases, modpack compatibility and broad in-game verification.

## Build status

A4 compiled and loaded in the user's Minecraft instance. A5/A5B/A5C source changes have NOT been confirmed to compile under NeoForge here. GitHub Actions and an isolated ATM10 instance are required for real integration tests. Existing standalone tests do not validate Minecraft UI or gameplay.

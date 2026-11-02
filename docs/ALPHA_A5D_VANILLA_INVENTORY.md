# Alpha A5D — Vanilla chest-style player inventory

The Transfer Buffer now uses Minecraft's standard `generic_54` chest screen background and chest-menu slot coordinates. The normal player inventory and hotbar are retained without a custom skin or slot behavior.

## Layout

- Player inventory: three rows of nine, with the normal nine-slot hotbar below, separated by the same gap as a vanilla chest.
- Buffer tiers I, II and III: 3, 4 and 5 visible rows, respectively.
- Buffer tiers IV and V: five visible rows, mouse-wheel and scrollbar navigation for the remaining slots.
- Sorting and scrolling controls are on a narrow strip to the **right** of the vanilla inventory; they do not overlap player slots.

## Interaction rules

- Vanilla player inventory clicks now pass through to `AbstractContainerMenu`: pickup/splitting, number-key swaps, drop, quick craft/drag distribution, and double-click collection.
- Shift-click from player inventory deposits into the frequency; shift-click from the Buffer withdraws up to one vanilla stack.
- A Buffer frequency slot displays one virtual item with a separate long-count label; it is not a normal mutable stack. Only these virtual slots need server-side custom pickup/deposit logic.
- Vanilla drag and double-click gathering explicitly exclude the Buffer's virtual slots, so their one-item projection cannot accidentally be removed or duplicated.
- No previous Buffer storage counts, sorting settings, frequency bindings, or capacities are intentionally changed.

## Testing

All standalone tests pass; the GitHub build verifies NeoForge compilation. Real client testing is still required for standard mouse/keyboard shortcuts and modded inventory overlays.

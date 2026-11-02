# Frequency Works — implementation status (Alpha A5H)

## Implemented for testing

- Five shared-frequency Transfer Buffer tiers, vanilla player inventory, long item quantities, scrolling and sorting.
- Five Transfer Port tiers with a Powah-style six-face selector; independent, simultaneous Import/Export ghost filters with per-tier 3/6/9/12/15 slots per bank; full native player inventory below.
- Server-validated ghost item selection (cursor item or Tag Card), whitelist/blacklist toggles, transfer-rate toggle, and existing Port automation.
- Operator debugger, shared frequency persistence and generation invalidation.
- Full NeoForge compilation is required before any jar is offered.

## Incomplete / next alpha

- **Next alpha priority:** Item Sequencer execution, Sequence Tablet programming, and instruction processing; Interaction and Redstone Cards.
- Frequency Monitor UI/functionality; final models, compatibility testing and crafting edge cases.
- In-game verification of Port ghost filters, direction handling, server synchronization and item counts.

See `docs/ALPHA_A5G_PORT_GUI.md` for Port UI/filter migration and `docs/ALPHA_A5H_PORT_GUI.md` for visual bugfix details.

- A5H moved the Port 1/64 transfer toggle below the side selector and corrected the explicit GUI background draw order. Gameplay verification remains pending.

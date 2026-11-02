# Alpha A5F — multi-slot item storage and Transfer Port routing UI

## Frequency storage

- Physical storage slots remain 27/36/45/54/81, with the existing per-slot long-count limits.
- When one type of item fills its first physical slot, additional inserts fill another slot of the same type. A given item type may occupy multiple physical slots.
- Insertion fills partially occupied slots of the same type first, then allocates empty slots.
- Network item counts aggregate duplicate types, while the Buffer GUI displays each individual physical storage slot.
- Clicks and shift-clicks from a Buffer slot debit that exact stable entry ID; automation can draw from the same type across multiple slots.
- Existing saved frequencies remain compatible. Duplicate entries for one item type are now allowed when loading saved frequency data.

## Transfer Port

- Mode buttons cycle Off -> Import -> Export -> Both -> Off, with **Import** meaning the attached inventory sends items into frequency storage and **Export** meaning items leave frequency storage for the attached inventory.
- A configured Port screen has Routing, Import Filter and Export Filter tabs, color-coded face modes and previews for exact item filter rules.
- The Port configuration panel is drawn before Minecraft's control widgets. Default blurred-world background drawing is suppressed, so the configuration panel remains readable.
- The prior global bidirectional filter validation prevented users from cycling modes and was removed.
- In Both mode on a single face, export excludes items eligible for import on that face, preventing that face from ping-ponging the same item between the inventory and frequency. Separate unidirectional faces operate normally.

## Safety and testing

- Existing stored counts, tier limits, binding security, sorting and frequency reset logic remain unchanged.
- Run GitHub Actions NeoForge build and validate in a disposable ATM10 test world; Minecraft client behavior cannot be proven through standalone tests.
- The Sequencer, Tablets and Frequency Monitor remain unfinished.

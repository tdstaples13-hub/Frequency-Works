# Alpha A6E — Item Sequencer / programmable Tablets

Place the Sequencer on a compatible Host inventory. Right-click to open its 1×9 real Tablet slots. Right-click a held Tablet in the air to program its 1×9 ghost instruction row.

Hold the Tablet and sneak-right-click the face of a machine to cycle its saved link through Input (Host -> machine), Output (machine -> Host), Both, and deselected. No scanning or chunk loading; linked targets must be within 16 blocks in the same dimension.

The Tablet editor has three compact 1×9 rows:
- **Top**: regular item/Tag Card steps select linked face and transfer direction. Interaction Card steps select the action (Right-click, Sneak-right-click, Punch, Mine), with icons R/S/P/M and a full tooltip. Redstone Card steps select their linked signal face.
- **Middle**: ghost item/tag/card program slots. Clicking a filled cell clears it. Only normal item ghosts have compact up/down quantity arrows (Shift = 10, otherwise 1).
- **Bottom**: regular item/tag steps toggle whitelist/blacklist. Interaction Card steps cycle their **linked target face** instead of a meaningless filter toggle. All controls use a restrained vanilla-style gray skin, not the color-legend examples.

Interaction Cards use a server-side dedicated fake player with empty hands, a 10-tick minimum interval between successful actions per Sequencer, and block/world checks. Punch invokes a single left-click hook without mining; Mine attempts to harvest the block via the server game mode and may remove it. The normal server interaction and break protections can veto actions. If a target refuses the action, the Tablet's failure policy applies. A target might not respond to an empty-hand right click; selecting a held tool from the Host inventory is **not implemented yet**. Gameplay behavior requires testing in an isolated world.

Other instructions move exact amounts directly through the Host inventory beneath the Sequencer, or wait for Redstone signal conditions. The program automatically loops. Missing items/inventory capacity cause a wait, not a silent skip. An intentionally stopped Sequencer can be restarted with sneak-right-click on its block while empty-handed; no always-present UI arrow.

Remaining work: per-face client outlines, JEI/EMI ghost dragging, tool-equipped fake-player interactions, modded-machine compatibility verification, and end-to-end in-game testing.

# Alpha A5H — Transfer Port GUI visual repair

After A5G playtesting showed transparent/missing GUI panels and invisible
inventory/filter slot backgrounds, A5H explicitly renders the opaque frame,
both three-row filter slot grids, and Minecraft's normal chest inventory panel
*before* drawing slots, labels, buttons and tooltips.

- Port configuration still uses the compact six-face, 3×3 selector.
- The transfer amount control is now a narrow **1 / 64** button immediately
  below the selector. Its tooltip spells out single-item/full-stack mode.
- Import and Export filter grids remain visible side by side, with tier
  capacities of 3, 6, 9, 12 and 15 slots per bank.
- The player inventory and hotbar are standard Minecraft slots over vanilla
  chest inventory texture, with slot positions aligned to the texture.
- No changes to transfer, filter matching, stored frequencies or Port tier
  capacity were made in this visual revision.

A successful build verifies compilation; **the in-game rendering still needs
a fresh screenshot before this is considered resolved**.

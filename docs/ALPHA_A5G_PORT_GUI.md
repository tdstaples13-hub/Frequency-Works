## Alpha A5G — Transfer Port interface

Transfer Ports now use a Powah-style 3×3 six-side selector and two visible ghost-filter banks (Import and Export) on a real Minecraft container screen. Player inventory and hotbar retain vanilla item pickup, placement, and shortcuts; placing a carried item in a ghost filter does **not** consume the item. A carried configured Tag Card adds its `#tag` without consuming the card. Right-click a ghost slot to clear it. A frequency is still selected on first placement before the configuration screen opens.

Per-bank filter capacity by Port tier: I 1×3 = 3, II 2×3 = 6, III 3×3 = 9, IV 4×3 = 12, V 5×3 = 15. The grids always have three rows and grow sideways without paging. Every bank has its own whitelist/blacklist control. The compass has six operational squares and three disabled corners. Direction actions cycle Off → Import → Export → Both; relative to frequency, Import means inventory → frequency and Export means frequency → inventory.

**Migration:** Legacy filter entries outside the new capacity, if any, are retained in the Port's saved data for rollback, but do not participate in filtering. They do not occupy visible ghost slots.

**Known constraint:** Both mode deliberately blocks export of any item allowed by its import filter to prevent immediate transfer loops. With an unconfigured, unrestricted import filter, export on that *same* Both face will not activate until the import filter excludes the desired export items. Use dedicated Import/Export faces as an alternative.

**Testing:** compilation and standalone source checks cannot validate actual cursor UX or third-party modded GUIs; check in a disposable ATM10 world. Subsequent alpha work shifts to Item Sequencer and Sequence Tablets.

# Alpha A5B — Dynamic Buffer Inventory Grid

This patch changes the Alpha A5 Transfer Buffer container UI only.

## Behavior

- Tier I frequency: 27 **real** storage cells, 3 rows.
- Tier II frequency: 36 **real** storage cells, 4 rows.
- Tier III frequency: 45 **real** storage cells, 5 rows.
- Tier IV frequency: 54 cells, shown as 45 on page one and 9 on page two.
- Tier V frequency: 81 cells, shown as 45 on page one and 36 on page two.
- No grey/inactive placeholder slot squares are rendered. On short pages, only available cells are drawn and enabled.
- For Tiers I and II, the player inventory and hotbar move up to fill the space, and the entire menu becomes shorter.
- Page navigation is hidden unless more than one page exists.
- The server sends the tier to the client **before** constructing the menu so both sides have matching slot counts and indexes.
- No change to the stored number of slots, quantities, frequencies, or inventory contents.

## Build / use

Replace the four files included in `FrequencyWorks_Alpha_A5B_GitHub_Changes.zip` in the existing public or private GitHub repository, keeping their paths. Committing to `main` should run the existing GitHub Actions build. If the build fails, send the log; this patch has NOT been compiled against NeoForge inside ChatGPT.

Do not install the unverified source ZIP as a mod. Download the .jar artifact from a successful GitHub build and test it in a **copy** of the ATM10 instance.

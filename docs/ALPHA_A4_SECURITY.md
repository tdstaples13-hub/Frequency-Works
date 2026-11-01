# Alpha A4 — Private Buffer security and blast resistance

**Status:** Source implementation complete. Minecraft/NeoForge compilation and ATM10 in-game behavior are NOT yet verified. Do not install the source archive into the mods folder.

## Added

- NeoForge `BlockEvent.BreakEvent` denies non-owners' private Buffer breaks, including creative-player packet breaks.
- Right-click opening and held-item opening refuse private Buffers belonging to another UUID. Existing server-side frequency GUI validation remains in force.
- Tier III–V Buffers have bedrock-like explosion resistance and are removed from NeoForge explosion affected-block lists. The Block explosion callback also refuses removal.
- Tier I/II Public Buffers generate ONE bound item drop upon explosion; Tier I/II Private Buffers produce NO drop; all frequency inventory data is persistent regardless of the blocks' fate.
- Normal survival mining still returns one bound Buffer with assigned frequency. Buffer block loot tables are explicitly disabled to prevent duplicate unbound drops.
- An unauthorized player cannot collect another owner's Private Buffer item from the ground or place/use it; the client/server slot-stacking hook denies some container manipulations, and an unauthorized private Buffer that enters the player's main inventory via another route is ejected during its inventory tick.

## Limits and known risks

1. Minecraft/NeoForge build not tested; possible version-specific API issues remain.
2. Preventing *every possible* unauthorized inventory GUI action or third-party mod's item-transfer operation is not guaranteed; a custom container click hook or mixin may be needed for complete prevention. The inventory-tick fallback protects ordinary retained player inventory slots but does not cover every modded container or cursor state.
3. External inventory item capability for Buffers, Tier II–V Buffer upgrade recipes and larger gameplay integrations remain separate unfinished milestones.
4. Explosion behavior must be tested for TNT, creepers, beds/respawn anchors and end crystals in ATM10; other mods can manipulate blocks independently of standard explosion events.
5. This protection does not stop server administrators or commands, nor is it a substitute for claim/land-protection mods.

## Local test commands

Run these from the extracted `FrequencyWorks` folder with Java 21:

```sh
bash scripts/test-security.sh
bash scripts/test-core.sh
bash scripts/test-bridge.sh
bash scripts/test-buffer-gui.sh
python3 scripts/check-resources.py
```

`test-security.sh` tests decision rules in plain Java. It does not execute real Minecraft events.

## In-game acceptance tests once the project builds

1. Alice binds a Private 042 Tier I Buffer; Bob cannot open or break it in survival or creative, and Alice can break and recover it with the frequency intact.
2. Bob cannot collect Alice's bound Private Buffer dropped item. Alice can collect and replace it. Other private player ownership is isolated.
3. Public Tier I and II Buffers hit by TNT yield a bound item; Private Tier I and II Buffers hit by TNT yield no item; the frequency still exists with all contents.
4. Tier III–V Public and Private Buffers remain in the world after repeated TNT and charged creeper explosions.
5. Public Buffers remain accessible by other players; no Port or player receives an extra unbound Buffer when a bound Buffer breaks.
6. Restart the server and verify private frequency and storage persistence.

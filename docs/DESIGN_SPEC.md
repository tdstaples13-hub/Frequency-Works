# Frequency Works — Technical Design Specification

**Status:** gameplay specification v0.1, recorded 2026-10-31. **Name:** "Frequency Works" is a provisional project name, not an agreed final mod name.

**Target:** Minecraft Java 1.21.1, NeoForge 21.1.x / Java 21; All the Mods 10 is the primary integration test environment. One universal vanilla-material recipe set; no ATM10-specific recipes required.

## 1. Intent and boundaries

The mod has two separate systems:

1. **Transfer system:** persistent server-wide, frequency-indexed temporary inventories (Transfer Buffers), accessed by autonomous high-speed inventory endpoints (Transfer Ports). No required placed/loaded Buffer; loaded Ports transfer on their own. It is intended as a transport buffer, not a general digital storage/crafting network.
2. **Crafting automation:** the Item Sequencer uses the inventory of the block **directly below** it as its host, and executes up to nine Sequence Tablets with up to nine ordered instructions each. No implicit or direct connection to Transfer Frequencies. A player *may* set a Transfer Buffer beneath a Sequencer to connect the systems through an ordinary inventory interface.

**Primary objective:** minimize unnecessary inventory calls and item entities, protect against item loss/duplication, and maintain clear player control. No performance claim until measured in an ATM10 profile.

## 2. Transfer frequencies

- Three-digit IDs 000–999. Key is `(scope, owner_uuid_if_private, frequency_id)`.
- **Public:** shared by all players. **Private:** distinct namespace per player's UUID; only that owner's authorized Private Buffers/Ports may access it. Do not trust client-supplied owner information; the server derives/validates it.
- **Only a Transfer Buffer can create a frequency.** Ports cannot bind to nonexistent or unauthorized frequencies.
- Once created, a frequency persists indefinitely across dimensions and operates without a Buffer present or loaded. Inactive frequencies do not tick.
- On first binding an unassigned Buffer, player chooses Public/Private and a three-digit frequency. If the frequency already exists in the chosen namespace, show a warning and require confirmation before joining. Binding is permanent, including its scope and (for private) owner UUID.
- The Buffer UI opens by right-clicking a placed Buffer **or by right-clicking air holding a Buffer item**. After binding, this UI can only open that frequency; it cannot browse/assign another.
- The frequency has **one shared inventory**, independent of number of linked Buffers. Multiple Buffers are access points, not additive capacity or speed.
- Frequency storage tier is the highest Buffer tier ever activated by placement or by opening its UI (including while in-hand). Higher-tier capacity upgrades are permanent; capacity never shrinks. Crafting an upgrade alone does not activate it.
- A lower-tier Buffer attempting to view a higher-tier frequency shows: `This Buffer cannot display the contents of [frequency] due to tier incompatibility.` It cannot browse its contents. A Port's tier does **not** restrict which frequency tier it transfers from/to.
- Mined Buffers retain binding, scope, and owner on their item data components; crafting the next tier from one also preserves binding. Private network access is enforced server-side even if such an item changes hands.
- Frequency contents are world-level saved data (attach to overworld), separate from any block entity. Each unique stored-item key includes item ID **and data components**, not just item ID.

### Storage capacity (per shared frequency)

| Buffer tier | Name | Equivalent Dank tier | Distinct slots | Max count per slot |
|---:|---|---:|---:|---:|
| I | Tethered | 3 | 27 | 4,096 |
| II | Gilded | 4 | 36 | 16,384 |
| III | Blazing | 5 | 45 | 65,536 |
| IV | Endwoven | 6 | 54 | 262,144 |
| V | Dragonbound | 7 | 81 | 2,147,483,647 |

The large per-slot count must NOT be represented as a huge vanilla `ItemStack`. Store `(one-item prototype, long count)` and expose at most an ordinary stack-size chunk per inventory operation. Explicitly define handling for nonstackables, component-bearing items and nested storage safety before release.

### Frequency inventory sorting

Exactly four selectable, shared frequency settings:

1. **Quantity: Smallest → Largest**
2. **Quantity: Largest → Smallest** (**default**)
3. **Alphabetical** (display name)
4. **By Mod: Alphabetical** (mod namespace A→Z, then item display name A→Z)

Port filters determine *eligibility* first, then selected sort order determines the next eligible item. Ties use a stable item ID/components key. Cache/maintain ordering rather than sorting on every transfer where practical. Sorting and reordering must not change stored item quantities.

## 3. Transfer Ports

- Five independent tiers; Port tier affects **only transfer rate and filter capacity**, not frequency capacity or eligibility.
- An item-form Transfer Port is ALWAYS an ordinary clean item; no custom item data components whatsoever. Tier is distinguished by item type. Mined Port drops only its tier item, allowing identical Ports to stack to **64**.
- Upon **every placement**, Port is unbound and inactive. Player chooses frequency ID + Public/Private; server validates existence and Private ownership. After submission, configure six faces, all initially **None**. All settings exist only on the placed block entity and persist over unload/restart; all are discarded when broken.
- Each face: `None`, `Input` (machine → frequency), `Output` (frequency → machine), and `Input/Output` (where explicitly filtered). The face configuration refers to the **machine's sided item handler**, not the Port attachment face. Unsupported faces may be selected; attempts simply fail without item loss.
- Independent **Input and Output filter banks**, each with the tier's full slot allowance. Each bank has its own whitelist/blacklist toggle. Filters accept ghost exact items or Tag Cards. Empty/unconfigured filters permit all, including an empty whitelist. If a face operates in both directions, disallow overlapping eligible item classes when the system can verify them; otherwise expose a warning and treat potentially cyclic setups conservatively. The intent is to prevent same-dot loops without scanning every transfer.
- Each Port has selectable quantity from 1 item to a full native stack; speed limits are calculated per transfer mode. No global Buffer arbiter: each loaded Port checks/takes the next eligible available item, like two hoppers under a double chest. If enough of one item remains, both Ports may take it. No reservation, ownership partition, or forced round-robin.
- When inserting into a frequency, transfer only the amount it can accept; if full, leave items in source inventory. If target inventory cannot accept, leave items in frequency. No intermediate Port inventory, drop, or deletion.
- Ports can work across dimensions without an upgrade. Only **Port** chunks need to be loaded, by the game or external chunk loaders; no force loading by this mod.

### Beta throughput targets (at 20 TPS)

| Port tier | Input filter slots | Output filter slots | Individual mode | Stack mode |
|---:|---:|---:|---|---|
| I | 1 | 1 | 1 item / 8 ticks | 1 stack / 8 ticks |
| II | 2 | 2 | 2 items / tick | 1 stack / 3 ticks |
| III | 4 | 4 | 4 items / tick | 3 stacks / 4 ticks |
| IV | 9 | 9 | 9 items / tick | 5 stacks / 3 ticks |
| V | 20 | 20 | 20 items / tick | 4 stacks / tick |

- Use **batch-count operation semantics**, not one API call per individual item; Tier V's 20 individual-mode items can be one 20-item transfer when possible.
- Cap transfer work at **four logical transfer attempts per Port per tick**, and limit capability/slot scans separately to avoid high-cost failed attempts. Actual rate may be lower for fragmented inventories, unfavorable sides, or full destinations. Stagger periodic work and back off blocked attempts; never sacrifice correctness for rate.
- A server tick serializes concurrent operations against the current frequency state to avoid double withdrawal.

## 4. Item Sequencer and Sequence Tablets

- Item Sequencer is a torch-sized world block, supported by a block with an item-handler inventory immediately **below** (Host Inventory). No internal item buffer and no automatic wireless connection.
- **Exactly 9 slots** for Sequence Tablets, run in slot order. Each tablet has **exactly 9 instruction slots**, run left to right. Maximum theoretical 9×9 = **81 operations** in the program.
- On completing the last instruction, automatically wrap to the beginning; **no loop toggle** or global Redstone control. Player controls halting with ingredient availability, failure behavior, and Redstone Cards.
- Fixed **16-block target range**, same dimension, no line-of-sight requirement. Never scan the radius; only explicitly linked locations are queried. No force loading. If moving a configured tablet to a different Sequencer, linked positions are retained but revalidated for distance/dimension/chunk access.
- Every Sequence Tablet can link **multiple individual block faces**, including different sides of the same block. Holding the tablet and Shift + right-clicking a particular face cycles that `(dimension, position, face)` through: **Input (green; Host → target)** → **Output (red; target → Host)** → **Input/Output (purple)** → **Deselected (no outline)**. Colors highlight only the selected faces, client-side for the holder of that tablet; no world glow entity effects. Target roles here use the *machine's* perspective, opposite the frequency Port's naming convention: clearly label direction arrows in both UIs.
- Right-click air with a Sequence Tablet opens its programming UI. Nine left-to-right **ghost instruction slots** can reference an exact ghost item, a configured Tag Card, an Interaction Card, or a Redstone Card.
- An item/tag instruction has explicit item/tag, requested count, a **per-slot whitelist/blacklist toggle**, and a **per-slot linked face+direction selector** above the slot. A face linked as bidirectional appears as TWO separate choices: `[face] input` and `[face] output`. Each instruction performs **one** direction, never ambiguous bidirectional action.
- Exact order and exact requested quantities matter. A step must complete its quantity before the next instruction; partial progress (remaining amount + step index) is tracked in the Sequencer, not stored on the tablet. Output instructions wait for the specified product and recover it to Host. For tag/blacklist transfers, avoid oscillating between item kinds midway through one step without a chosen matching stack.
- Ghost item selection supports normal inventory-cursor clicks and recipe-viewer drag/drop (JEI/EMI as installed), with no item consumed. Validate integration against the actual ATM10 recipe viewer and version.
- Processing target: a fastest-Modular-Routers-like **2-tick processing cycle**, bounded at **9 instruction attempts per cycle**; only current active instruction advances. Redstone edge monitoring may run each tick while active to avoid missed pulses. This is a benchmark target, not a guaranteed rate.
- Each Tablet has a single `On Failure` cycle button: **Pause / Continue** (default: checkpoint and recheck blocking condition), **Skip** (only if safe before a partially committed batch), **Stop** (requires manual restart). Do not confuse a normal processing wait with failure. Never rewind successfully completed item moves when resuming.
- Break Item Sequencer: automatically drop its installed **programmed Tablets** separately, with their targets/instructions preserved. Drop clean unprogrammed Item Sequencer item. Host Inventory and items already delivered to crafting targets are untouched. Runtime progress is abandoned only upon block break; normal unload/save/restart preserves it.

### Specialized objects usable inside a Sequence Tablet

- **Tag Card:** A configured Minecraft *item tag* selector (`#namespace:tag`), not an executable tablet and not placed directly into Sequencer. The tablet interprets its tag when executing the instruction. Can also be referenced by Port filters.
- **Interaction Card:** Configures fake-player-style use action (right-click, sneak-right-click, carefully bounded left click), optional exact held tool from Host Inventory, and target face selected by Tablet. Execute once on confirmed success, do not repeatedly spam interactions. Some mods reject fake players or require special adapters; respect protection checks and tool durability/returns.
- **Redstone Card:** An instruction-level gate for `Powered`, `Unpowered`, `Rising Edge`, `Falling Edge`, or specified strength threshold (1–15). It observes the linked target face and blocks progression until satisfied. No item move or activation and no global Sequencer redstone mode.

## 5. Transfer Frequency Monitor

- Separate inexpensive block, read-only frequency browser; all public frequencies and the viewing player's own private frequencies (never anyone else's private network).
- Previous/Next and three-digit direct navigation; display frequency tier, slot occupancy/capacity, total item count, sorting order, and inventory contents. No insert/extract, upgrades, creation, or sort editing from Monitor.
- Only sync selected frequency while GUI is open; lazy paging/update snapshots; no inventory scanning/ticking when closed.
- Monitor is not a permanent frequency-bound Buffer; all tiers can be viewed regardless of the viewer's Buffer tier.

## 6. Recipes — universal vanilla materials

All 3×3 shaped recipes; do not require ATM10-specific resources. **Previous-tier same-item component goes in center for Tiers II–V.** Tier I Buffer center accepts *either* Chest or Barrel; Tier I Port center is Redstone Torch.

**Buffer pattern:** `A F A / B X B / D F D`.

**Port pattern:** `B F B / P X P / B F B`.

| Tier | Buffer A (upper structure) | Buffer D (lower) | Port P (structure) | Focus F | Bond B |
|---|---|---|---|---|---|
| I Tethered | Copper Ingot | Iron Ingot | Copper Ingot | Amethyst Shard | String |
| II Gilded | Gold Ingot | Gold Ingot | Gold Ingot | Diamond | Slimeball |
| III Blazing | Gold Ingot | Blaze Rod | Blaze Rod | Eye of Ender | Magma Cream |
| IV Endwoven | Netherite Scrap | Shulker Shell | Shulker Shell | Echo Shard | Phantom Membrane |
| V Dragonbound | Netherite Ingot | Netherite Ingot | Netherite Ingot | End Crystal | Dragon's Breath |

Yields: Tier I Buffer ×1; tier upgrades Buffer ×1 and carry bound frequency data; Tier I Port ×4; Port tier upgrades ×1 and carry **no** saved data.

**Item Sequencer:** `Gold, Gilded Port, Gold / Magma Cream, Eye of Ender, Magma Cream / Blaze Rod, Gilded Port, Blaze Rod` → 1 Item Sequencer.

**Sequence Tablet:** `#c:stones, Redstone Dust, #c:stones / #c:stones, Nether Quartz, #c:stones / #c:stones, Redstone Dust, #c:stones` → **2 Tablets**.

**Three special cards:** `#c:stones, #c:stones, #c:stones / #c:stones, SPECIAL, #c:stones / #c:stones, Redstone Dust, #c:stones` → 1 card. Special ingredient: Comparator for Tag Card; Dispenser for Interaction Card; Observer for Redstone Card.

**Transfer Frequency Monitor:** `Glass Pane, Nether Quartz, Glass Pane / Iron Ingot, Comparator, Iron Ingot / Redstone Dust, Ender Pearl, Redstone Dust` → 1 Monitor.

Note: `#c:stones` must be verified against the installed NeoForge/common tag set. Exact-shaped recipe collisions must be checked in a running ATM10 environment; previous public search found no confirmed clashes for special cards, not a full registry audit.

## 7. Engineering rules and acceptance tests

- Server-authoritative frequency assignments and ownership; client must not be able to name an unrelated private UUID or invent storage. Protect the exact binding data against spoofed client actions.
- Full item identity includes data components. Treat huge quantity as *long count* + a one-item prototype; handle overflow, and never return an invalid overlarge ItemStack through `IItemHandler`.
- Transaction flow checks insertion capacity before extracting from machine; after actual extraction, any unexpected insertion remainder must be restored to source safely or held in a recovery queue, never silently deleted. Beware misbehaving third-party handlers and non-atomic cross-inventory operations.
- Port filters and `None` faces perform no gratuitous scanning; cache item handler capabilities with invalidation. Avoid unbounded scans when no eligible item exists.
- Save frequency upgrades only upon Buffer placement or bound UI access; do not shrink data when lower-tier access points are broken.
- Test two autonomous output Ports racing for same eligible item; same-item extraction if supply sufficient, no duplication if insufficient.
- Test public/private namespaces, frequencies 000/999, unauthorized players holding a private Buffer, server restart, Nether/End, and no loaded Buffer.
- Test partial inventory space and incompatible sided handlers; no item drops/deletes.
- Test clean Port item stacking (all five tiers), configured tablet drops, preserved Buffer item frequency through upgrade crafting, and on-hand Buffer UI upgrade.
- Test 9×9 Tablet/Sequencer instructions and Pause / Continue across chunk unload and server restart.
- Profile server MSPT with 10 / 100 / 1,000 loaded Ports against Pipez in ATM10, with identical inventory workloads and throughput; **do not call it lag-free before profiling.**

## 8. Implementation milestones

1. Baseline MDK, registry, block/item models and data files.
2. Server frequency SavedData, private/public authorization, Buffer binding/upgrade/read UI and persistence.
3. Per-Port placed-only configuration/UI, side-specific capabilities, independent bounded ticking, filters, batch transfers.
4. Dynamic crafting recipes copying bound Buffer data; full universal tier recipes.
5. Read-only Frequency Monitor UI and scoped pagination.
6. Item Sequencer + 9×9 state machine, tablet data/target selection and client face highlights.
7. Tag/Interaction/Redstone Card implementations, JEI/EMI integration, real mod compatibility adapters.
8. Automated save/reload tests, ATM10 integration test, CPU/MSPT benchmarks, tuning.

**Source references:** https://docs.neoforged.net/docs/1.21.1/ ; https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle .


## Buffer ownership and explosion protection (A4 security amendment)

- Private Buffer GUI access and intentional player breaking are restricted to its owner's UUID. Public and unbound Buffers are not owner-locked.
- Tier I (Tethered) and Tier II (Gilded) Buffers can be destroyed by normal explosions. When destroyed in an explosion, a **Public** bound Buffer drops its tier item with its assigned frequency preserved; an unbound Buffer drops its normal unbound item. A **Private** bound Buffer drops nothing. Frequency storage survives regardless.
- Tier III (Blazing), IV (Endwoven), and V (Dragonbound) Buffers are explosion-immune, whether Public, Private, or unbound.
- Only owners may collect Private Buffer item entities from the ground or use/place Private Buffer items. Non-owner attempts to retain such items in their personal inventory are rejected/ejected; the items are never intentionally deleted.
- Physical third-party block moving (for example Mekanism Cardboard Box), admin commands and other mods directly manipulating inventories are not guaranteed to be blocked. This is a gameplay security measure, not a server-wide claim/protection substitute.
- Standard external inventory automation remains available on private Buffers once their IItemHandler capability is implemented; ownership checks apply to player actions rather than every machine transfer.

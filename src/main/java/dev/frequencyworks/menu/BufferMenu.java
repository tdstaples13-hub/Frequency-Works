package dev.frequencyworks.menu;

import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.core.NetworkInventory;
import dev.frequencyworks.network.ConfigNetworking;
import dev.frequencyworks.registration.ModContent;
import dev.frequencyworks.world.FrequencySavedData;
import dev.frequencyworks.world.FrequencySavedData.StoredFrequency;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
import java.util.List;

/** Server-authoritative 9-column scrolling view of SavedData frequency storage.
 * Scroll position changes in whole rows (9 items) without switching pages.
 * Virtual displayed stacks always have count 1; real amounts remain long values.
 */
public final class BufferMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 9;
    public static final int VIEW_SLOTS = 45;
    public static final int COLUMNS_PER_ROW = 9;
    public static final int SORT_BUTTON = 2;
    public static final int SCROLL_TO_ROW_BASE = 100;
    // Smaller frequencies have fewer *real* clickable slots, not disabled placeholders.
    // Higher tiers have a 45-slot viewport over their longer inventory.
    private final int viewSlots;
    private final int visibleRows;
    private final SimpleContainer projection;
    private final long[] entryIds;
    private final long[] quantities;
    private final ServerPlayer serverPlayer;
    private final FrequencyId frequencyId;
    private final int physicalTier;
    private final net.minecraft.core.BlockPos blockPos;
    private final int hand;
    private int scrollRow;
    private int capacity;
    private NetworkInventory.SortMode sorting = NetworkInventory.SortMode.QUANTITY_DESC;
    private boolean viewDirty = true;
    private long lastRevision = Long.MIN_VALUE;
    private int lastScrollRow = -1;

    /** Receives the physical Buffer tier before the menu slots are constructed. */
    public BufferMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, null, null, data.readVarInt(), null, -1);
    }

    public BufferMenu(int containerId, Inventory inventory, ServerPlayer serverPlayer,
                      FrequencyId id, int tier, net.minecraft.core.BlockPos pos, int hand) {
        super(ModContent.BUFFER_MENU.get(), containerId);
        this.serverPlayer = serverPlayer;
        this.frequencyId = id;
        this.physicalTier = tier;
        this.blockPos = pos;
        this.hand = hand;
        // Tier I–III: 27/36/45; Tier IV–V: 45 visible slots, scroll by row.
        int[] tierSlots = {27, 36, 45, 54, 81};
        int boundedTier = Math.max(1, Math.min(tier, tierSlots.length));
        this.viewSlots = Math.min(VIEW_SLOTS, tierSlots[boundedTier - 1]);
        this.visibleRows = (viewSlots + COLUMNS - 1) / COLUMNS;
        this.projection = new SimpleContainer(viewSlots);
        this.entryIds = new long[viewSlots];
        this.quantities = new long[viewSlots];
        Arrays.fill(entryIds, -1);
        for (int i = 0; i < viewSlots; i++) {
            final int cell = i;
            // Never allow an out-of-capacity cell to be used for deposits.
            addSlot(new Slot(projection, i, 8 + (i % COLUMNS) * 18, 18 + (i / COLUMNS) * 18) {
                @Override public boolean isActive() { return cell < visibleInView(); }
                // These stacks are *displays*, not real vanilla-sized inventory stacks.
                // The server handles frequency deposits/withdrawals in clicked().
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        // Exact vanilla ChestMenu layout: 3x9 inventory, 9-slot hotbar, 4px gap.
        // None of these slots are virtual; they retain vanilla click behavior.
        final int inventoryY = inventoryY();
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            addSlot(new Slot(inventory, 9 + r * 9 + c, 8 + c * 18, inventoryY + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(inventory, c, 8 + c * 18, inventoryY + 58));
    }

    private StoredFrequency storage() {
        if (serverPlayer == null || frequencyId == null || !stillValid(serverPlayer)) return null;
        return FrequencySavedData.get(serverPlayer.serverLevel()).find(frequencyId).orElse(null);
    }

    public int scrollRow() { return scrollRow; }
    public int capacity() { return capacity; }
    public int viewSlots() { return viewSlots; }
    public boolean supportsScrolling() { return physicalTier >= 4; }
    public int visibleRows() { return visibleRows; }
    public int inventoryY() { return 31 + visibleRows * 18; }
    public int maxScrollRow() { return Math.max(0, (capacity - viewSlots + COLUMNS - 1) / COLUMNS); }
    public boolean canScroll() { return maxScrollRow() > 0; }
    public int visibleInView() { return Math.max(0, Math.min(viewSlots, capacity - scrollRow * COLUMNS)); }
    public long count(int slot) { return slot >= 0 && slot < viewSlots ? quantities[slot] : 0; }
    public NetworkInventory.SortMode sortMode() { return sorting; }

    public void updateClient(int nextScrollRow, int nextCapacity, String mode, long[] incoming) {
        capacity = nextCapacity;
        scrollRow = Math.max(0, Math.min(nextScrollRow, maxScrollRow()));
        try { sorting = NetworkInventory.SortMode.valueOf(mode); }
        catch (IllegalArgumentException ignored) { sorting = NetworkInventory.SortMode.QUANTITY_DESC; }
        Arrays.fill(quantities, 0L);
        System.arraycopy(incoming, 0, quantities, 0, Math.min(incoming.length, quantities.length));
    }

    private void refreshView(boolean force) {
        StoredFrequency store = storage();
        if (store == null) return;
        // A row-aligned sliding window preserves the 9-column grid as it scrolls.
        int maxRow = Math.max(0, (store.tier().slots() - viewSlots + COLUMNS - 1) / COLUMNS);
        scrollRow = Math.max(0, Math.min(scrollRow, maxRow));
        // O(1) on idle GUI ticks. Sort at most 81 entries on actual changes.
        if (!force && !viewDirty && lastRevision == store.revision() && lastScrollRow == scrollRow) return;
        List<StoredFrequency.Entry> entries = store.sortedSnapshot();
        lastRevision = store.revision();
        lastScrollRow = scrollRow;
        viewDirty = false;
        capacity = store.tier().slots();
        sorting = store.sorting();
        for (int i = 0; i < viewSlots; i++) {
            int index = scrollRow * COLUMNS + i;
            if (index < entries.size() && index < capacity) {
                var entry = entries.get(index);
                entryIds[i] = entry.id();
                quantities[i] = entry.quantity();
                projection.setItem(i, entry.prototype().copyWithCount(1));
            } else {
                entryIds[i] = -1L;
                quantities[i] = 0L;
                projection.setItem(i, ItemStack.EMPTY);
            }
        }
        ConfigNetworking.sendBufferCounts(serverPlayer, containerId, scrollRow, capacity, sorting.name(), quantities);
    }

    @Override public void broadcastChanges() {
        if (serverPlayer != null) refreshView(false);
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (serverPlayer == null || player != serverPlayer || storage() == null) return false;
        if (id >= SCROLL_TO_ROW_BASE && id <= SCROLL_TO_ROW_BASE + maxScrollRow()) {
            scrollRow = id - SCROLL_TO_ROW_BASE;
        } else if (id == SORT_BUTTON) {
            var modes = NetworkInventory.SortMode.values();
            StoredFrequency store = storage();
            store.sorting(modes[(store.sorting().ordinal() + 1) % modes.length]);
            FrequencySavedData.get(serverPlayer.serverLevel()).changed();
            scrollRow = 0;
        } else return false;
        viewDirty = true;
        broadcastChanges();
        return true;
    }

    /** Avoid vanilla Slot.remove semantics for long-count network storage; no client
     * cursor or fake projection is ever allowed to determine how many items exist. */
    @Override public void clicked(int slotId, int mouseButton, ClickType click, Player player) {
        if (slotId >= 0 && slotId < viewSlots) {
            if (!(player instanceof ServerPlayer server) || server != serverPlayer) return;
            if (slotId >= visibleInView()) return;
            StoredFrequency store = storage();
            if (store == null) return;
            // Use the stable entry ID from the last server-synced view. A Port
            // may change sorting between mouse-down and server processing.
            long id = entryIds[slotId];
            ItemStack cursor = getCarried();
            if (click == ClickType.QUICK_MOVE) {
                if (id != -1L) {
                    var entry = store.findEntry(id).orElse(null);
                    if (entry != null) {
                        int amount = (int)Math.min(entry.quantity(), entry.prototype().getMaxStackSize());
                        net.minecraft.world.item.ItemStack result = moveToInventory(entry.prototype(), amount);
                        int moved = amount - result.getCount();
                        if (moved > 0) store.withdrawEntry(entry.id(), moved);
                    }
                }
            } else if (click == ClickType.PICKUP && (mouseButton == 0 || mouseButton == 1)) {
                if (!cursor.isEmpty()) {
                    if (id == -1L || store.findEntry(id).map(e -> ItemStack.isSameItemSameComponents(e.prototype(),cursor)).orElse(false)) {
                        int desired = mouseButton == 1 ? 1 : cursor.getCount();
                        int accepted = store.insert(cursor.copyWithCount(desired));
                        if (accepted > 0) cursor.shrink(accepted);
                    }
                } else if (id != -1L) {
                    var entry = store.findEntry(id).orElse(null);
                    if (entry != null) {
                        int max = entry.prototype().getMaxStackSize();
                        int request = mouseButton == 1 ? 1 : max;
                        int taken = store.withdrawEntry(entry.id(), request);
                        if (taken > 0) setCarried(entry.prototype().copyWithCount(taken));
                    }
                }
            }
            // Other click types are intentionally ignored on virtual frequency cells:
            // creative clones/double-click/drop/drag must not create phantom items.
            FrequencySavedData.get(server.serverLevel()).changed();
            viewDirty = true;
            broadcastChanges();
            return;
        }
        // Let Minecraft handle ALL clicks outside the virtual frequency grid.
        // This includes player-inventory pickup, hotbar swapping, drop, shift-click,
        // double-click collection, offhand swapping and drag distribution.
        // canTakeItemForPickAll/canDragTo exclude virtual network slots.
        super.clicked(slotId, mouseButton, click, player);
    }

    /** Drag distribution must never enter long-count virtual storage cells.
     * Vanilla drag handling is preserved for all player inventory and hotbar slots. */
    @Override public boolean canDragTo(Slot slot) {
        return slot.container != projection && super.canDragTo(slot);
    }

    /** Double-click collection should work normally in the player's inventory,
     * but may not sweep items out of the virtual storage projection. */
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != projection && super.canTakeItemForPickAll(stack, slot);
    }

    /** Inserts into player's normal inventory, returns remainder (can be empty). */
    private ItemStack moveToInventory(ItemStack prototype, int requested) {
        if (requested <= 0) return ItemStack.EMPTY;
        ItemStack moving = prototype.copyWithCount(requested);
        // merge with existing matching stacks first, then fill empty slots.
        for (int i = 0; i < 36 && !moving.isEmpty(); i++) {
            ItemStack current = serverPlayer.getInventory().getItem(i);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, moving)) continue;
            int accepted = Math.min(moving.getCount(), current.getMaxStackSize() - current.getCount());
            if (accepted > 0) { current.grow(accepted); moving.shrink(accepted); }
        }
        for (int i = 0; i < 36 && !moving.isEmpty(); i++) {
            if (!serverPlayer.getInventory().getItem(i).isEmpty()) continue;
            int accepted = Math.min(moving.getCount(), moving.getMaxStackSize());
            serverPlayer.getInventory().setItem(i, moving.copyWithCount(accepted));
            moving.shrink(accepted);
        }
        serverPlayer.getInventory().setChanged();
        return moving;
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!(player instanceof ServerPlayer server) || server != serverPlayer || slotIndex < viewSlots || slotIndex >= slots.size()) return ItemStack.EMPTY;
        StoredFrequency store = storage();
        if (store == null) return ItemStack.EMPTY;
        Slot source = slots.get(slotIndex);
        ItemStack current = source.getItem();
        if (current.isEmpty()) return ItemStack.EMPTY;
        int accepted = store.insert(current.copy());
        if (accepted == 0) return ItemStack.EMPTY;
        ItemStack original = current.copy();
        current.shrink(accepted);
        source.setChanged();
        FrequencySavedData.get(server.serverLevel()).changed();
        viewDirty = true;
        return original;
    }

    @Override public boolean stillValid(Player player) {
        if (serverPlayer == null) return true;
        if (player != serverPlayer || frequencyId == null || !frequencyId.accessibleBy(player.getUUID())) return false;
        if (hand >= 0) {
            ItemStack stack = player.getItemInHand(hand == 0 ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
            if (!(stack.getItem() instanceof dev.frequencyworks.item.TransferBufferItem buffer)) return false;
            if (!frequencyId.equals(dev.frequencyworks.item.TransferBufferItem.binding(stack))) return false;
            return dev.frequencyworks.world.FrequencySavedData.get(serverPlayer.serverLevel()).isCurrent(frequencyId, dev.frequencyworks.item.TransferBufferItem.generation(stack)) && buffer.tier().ordinal()+1 == physicalTier;
        }
        if (blockPos == null || !player.level().hasChunkAt(blockPos)) return false;
        if (player.distanceToSqr(blockPos.getX()+.5D, blockPos.getY()+.5D, blockPos.getZ()+.5D) > 64) return false;
        if (!(player.level().getBlockEntity(blockPos) instanceof dev.frequencyworks.blockentity.TransferBufferBlockEntity buffer)) return false;
        return frequencyId.equals(buffer.binding()) && dev.frequencyworks.world.FrequencySavedData.get(serverPlayer.serverLevel()).isCurrent(frequencyId,buffer.generation());
    }
}

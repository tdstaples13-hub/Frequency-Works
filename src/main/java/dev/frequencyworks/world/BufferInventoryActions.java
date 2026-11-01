package dev.frequencyworks.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import dev.frequencyworks.world.FrequencySavedData.StoredFrequency;

/** GUI-triggered, server-authoritative transfers. Does not tick or scan inventories.
 * Handles only a single vanilla-sized item stack per button press, even for Dank-sized counts.
 */
public final class BufferInventoryActions {
    private BufferInventoryActions() {}

    /** Source slots 0-35 are the player's main inventory and hotbar. */
    public static int deposit(ServerPlayer player, StoredFrequency frequency, int sourceSlot, int maximum) {
        if (sourceSlot < 0 || sourceSlot >= 36 || maximum <= 0) return 0;
        ItemStack source = player.getInventory().getItem(sourceSlot);
        if (source.isEmpty()) return 0;
        int amount = Math.min(source.getCount(), maximum);
        int accepted = frequency.insert(source.copyWithCount(amount));
        if (accepted > 0) {
            source.shrink(accepted);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return accepted;
    }

    /** A row is identified by its persistent ID, never its current display index.
     * We first measure capacity in the player's 36 storage slots, debit only what
     * fits, then fill exact slots ourselves. No drop and no best-effort auto-add.
     */
    public static int withdraw(ServerPlayer player, StoredFrequency frequency, long entryId, int maximum) {
        if (entryId <= 0 || maximum <= 0) return 0;
        var selected = frequency.findEntry(entryId);
        if (selected.isEmpty()) return 0;
        var entry = selected.get();
        ItemStack prototype = entry.prototype();
        int perSlot = Math.min(64, prototype.getMaxStackSize());
        if (perSlot <= 0) return 0;
        int room = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack present = player.getInventory().getItem(i);
            if (present.isEmpty()) room += perSlot;
            else if (ItemStack.isSameItemSameComponents(present, prototype))
                room += Math.max(0, perSlot - present.getCount());
        }
        int request = (int) Math.min(entry.quantity(), Math.min(Math.min(maximum, perSlot), room));
        if (request <= 0) return 0;
        int removed = frequency.withdraw(prototype, request);
        int remaining = removed;
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack present = player.getInventory().getItem(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, prototype)) {
                int toAdd = Math.min(remaining, Math.max(0, perSlot - present.getCount()));
                if (toAdd > 0) { present.grow(toAdd); remaining -= toAdd; }
            }
        }
        for (int i = 0; i < 36 && remaining > 0; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                int toAdd = Math.min(remaining, perSlot);
                player.getInventory().setItem(i, prototype.copyWithCount(toAdd));
                remaining -= toAdd;
            }
        }
        if (remaining != 0) throw new IllegalStateException("Inventory changed during synchronous withdrawal");
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return removed;
    }
}

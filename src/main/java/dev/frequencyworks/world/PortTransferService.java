package dev.frequencyworks.world;

import dev.frequencyworks.world.FrequencySavedData.StoredFrequency;
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;
import net.neoforged.neoforge.items.IItemHandler;

/** Small, bounded inventory operations. No item entities, no central frequency tick.
 * Conforming IItemHandler implementations are assumed (simulation cannot mutate them).
 */
public final class PortTransferService {
    private PortTransferService() {}
    private static final int MAX_SLOT_PROBES = 16;

    /** INPUT: machine -> frequency. Returns moved item count, not number of calls. */
    public static int pullFromMachine(IItemHandler source, StoredFrequency frequency, int requested, int startSlot) {
        return pullFromMachine(source, frequency, requested, startSlot, stack -> true);
    }

    public static int pullFromMachine(IItemHandler source, StoredFrequency frequency, int requested,
                                      int startSlot, Predicate<ItemStack> filter) {
        int size = source.getSlots();
        if (requested <= 0 || size == 0) return 0;
        int probes = Math.min(MAX_SLOT_PROBES, size);
        for (int p = 0; p < probes; p++) {
            int slot = Math.floorMod(startSlot + p, size);
            ItemStack simulated = source.extractItem(slot, requested, true);
            if (simulated.isEmpty() || !filter.test(simulated)) continue;
            int permitted = (int) Math.min(simulated.getCount(), frequency.capacityFor(simulated));
            if (permitted <= 0) continue;
            ItemStack extracted = source.extractItem(slot, permitted, false);
            if (extracted.isEmpty()) continue;
            int inserted = frequency.insert(extracted);
            if (inserted < extracted.getCount()) {
                // Defensive restoration for a misbehaving or stateful third-party handler.
                ItemStack extra = extracted.copyWithCount(extracted.getCount() - inserted);
                ItemStack leftover = source.insertItem(slot, extra, false);
                if (!leftover.isEmpty()) {
                    // Do not silently destroy the item: broken handlers should cause a visible
                    // hard failure, not an invisible loss or an untracked duplicate.
                    throw new IllegalStateException("Non-transactional inventory refused refund: " + leftover);
                }
            }
            return inserted;
        }
        return 0;
    }

    /** OUTPUT: frequency -> machine. Writes destination FIRST, then debits what it accepted.
     * The shared frequency cannot be concurrently mutated by another Port mid-call on
     * Minecraft's authoritative server thread. */
    public static int pushToMachine(IItemHandler destination, StoredFrequency frequency, int requested, int startSlot) {
        return pushToMachine(destination, frequency, requested, startSlot, stack -> true);
    }

    public static int pushToMachine(IItemHandler destination, StoredFrequency frequency, int requested,
                                    int startSlot, Predicate<ItemStack> filter) {
        if (requested <= 0 || destination.getSlots() == 0) return 0;
        var first = frequency.first(filter);
        if (first.isEmpty()) return 0;
        var entry = first.get();
        ItemStack prototype = entry.prototype();
        int amount = (int) Math.min(Math.min(entry.quantity(), requested), prototype.getMaxStackSize());
        if (amount <= 0) return 0;
        ItemStack sending = prototype.copyWithCount(amount);
        int slots = destination.getSlots();
        for (int p = 0; p < Math.min(MAX_SLOT_PROBES, slots); p++) {
            int slot = Math.floorMod(startSlot + p, slots);
            ItemStack simulatedRemainder = destination.insertItem(slot, sending, true);
            if (simulatedRemainder.getCount() >= amount) continue;
            ItemStack remainder = destination.insertItem(slot, sending.copy(), false);
            int accepted = amount - remainder.getCount();
            if (accepted <= 0) continue;
            // The count is guaranteed present because every Port operates on the server thread.
            int debited = frequency.withdrawEntry(entry.id(), accepted);
            if (debited != accepted)
                throw new IllegalStateException("Shared frequency changed during synchronous insertion");
            return accepted;
        }
        return 0;
    }
}

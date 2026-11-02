package dev.frequencyworks.core;

import java.util.*;
import java.util.function.Predicate;

/**
 * Main-thread-only inventory core. NO vanilla ItemStack contains the long stored count.
 * Each distinct (item ID + data components) occupies one slot.
 */
public final class NetworkInventory {
    public enum SortMode { QUANTITY_ASC, QUANTITY_DESC, ALPHABETICAL, MOD_ALPHABETICAL }
    public record Entry(ItemIdentity item, long count) {}

    private BufferTier tier;
    private SortMode sortMode = SortMode.QUANTITY_DESC;
    private final ArrayList<Entry> quantities = new ArrayList<>();
    private List<Entry> sortedCache = List.of();
    private boolean invalidated = true;

    public NetworkInventory(BufferTier tier) { this.tier = Objects.requireNonNull(tier); }
    public BufferTier tier() { return tier; }
    public int occupiedSlots() { return quantities.size(); }
    public SortMode sortMode() { return sortMode; }
    public long count(ItemIdentity item) { return quantities.stream().filter(e -> e.item().equals(item)).mapToLong(Entry::count).sum(); }
    public long totalCount() { return quantities.stream().mapToLong(Entry::count).sum(); }

    /** Idempotent promotion. Returns true only when capacity was actually increased. */
    public boolean upgrade(BufferTier candidate) {
        Objects.requireNonNull(candidate);
        if (candidate.ordinal() <= tier.ordinal()) return false;
        tier = candidate;
        invalidated = true;
        return true;
    }

    public void setSortMode(SortMode mode) {
        if (mode != sortMode) {
            sortMode = Objects.requireNonNull(mode);
            invalidated = true;
        }
    }

    /** An item type may occupy several slots; fill matching slots before opening another. */
    public long insert(ItemIdentity item, long requested) {
        Objects.requireNonNull(item);
        if (requested <= 0) return 0;
        long remaining = requested;
        for (int i=0; i<quantities.size() && remaining>0; i++) {
            Entry current = quantities.get(i);
            if (!current.item().equals(item)) continue;
            long accepted = Math.min(remaining, tier.perSlot() - current.count());
            if (accepted <= 0) continue;
            quantities.set(i, new Entry(item, current.count() + accepted));
            remaining -= accepted;
        }
        while (remaining > 0 && quantities.size() < tier.slots()) {
            long accepted = Math.min(remaining, tier.perSlot());
            quantities.add(new Entry(item, accepted));
            remaining -= accepted;
        }
        if (remaining < requested) invalidated = true;
        return requested - remaining;
    }

    public long extract(ItemIdentity item, long requested) {
        Objects.requireNonNull(item);
        if (requested <= 0) return 0;
        long remaining = requested;
        for (int i=0; i<quantities.size() && remaining>0;) {
            Entry current = quantities.get(i);
            if (!current.item().equals(item)) { i++; continue; }
            long taken = Math.min(remaining, current.count());
            remaining -= taken;
            if (taken == current.count()) quantities.remove(i);
            else { quantities.set(i, new Entry(item, current.count() - taken)); i++; }
        }
        if (remaining < requested) invalidated = true;
        return requested - remaining;
    }

    public Optional<Entry> firstEligible(Predicate<ItemIdentity> filter) {
        return orderedEntries().stream().filter(entry -> filter.test(entry.item())).findFirst();
    }

    public List<Entry> orderedEntries() {
        if (invalidated) {
            ArrayList<Entry> entries = new ArrayList<>(quantities.size());
            entries.addAll(quantities);
            Comparator<Entry> stable = Comparator.comparing(e -> e.item().stableKey());
            Comparator<Entry> cmp = switch(sortMode) {
                case QUANTITY_ASC -> Comparator.comparingLong(Entry::count).thenComparing(stable);
                case QUANTITY_DESC -> Comparator.comparingLong(Entry::count).reversed().thenComparing(stable);
                case ALPHABETICAL -> Comparator.comparing((Entry e) -> e.item().displayName(), String.CASE_INSENSITIVE_ORDER).thenComparing(stable);
                case MOD_ALPHABETICAL -> Comparator.comparing((Entry e) -> e.item().namespace())
                    .thenComparing(e -> e.item().displayName(), String.CASE_INSENSITIVE_ORDER).thenComparing(stable);
            };
            entries.sort(cmp);
            sortedCache = List.copyOf(entries);
            invalidated = false;
        }
        return sortedCache;
    }

    /** Restore saved inventory after validating every key/count, never overflow. */
    public void restore(ItemIdentity item, long count) {
        if (count <= 0 || count > tier.perSlot()) throw new IllegalArgumentException("Invalid saved count");
        if (quantities.size() >= tier.slots()) throw new IllegalArgumentException("Too many saved slots");
        quantities.add(new Entry(item, count));
        invalidated = true;
    }
}

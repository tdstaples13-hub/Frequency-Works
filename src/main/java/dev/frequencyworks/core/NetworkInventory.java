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
    private final LinkedHashMap<ItemIdentity, Long> quantities = new LinkedHashMap<>();
    private List<Entry> sortedCache = List.of();
    private boolean invalidated = true;

    public NetworkInventory(BufferTier tier) { this.tier = Objects.requireNonNull(tier); }
    public BufferTier tier() { return tier; }
    public int occupiedSlots() { return quantities.size(); }
    public SortMode sortMode() { return sortMode; }
    public long count(ItemIdentity item) { return quantities.getOrDefault(item, 0L); }
    public long totalCount() { return quantities.values().stream().mapToLong(Long::longValue).sum(); }

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

    /** Returns accepted quantity; caller must leave any remainder at its source. */
    public long insert(ItemIdentity item, long requested) {
        Objects.requireNonNull(item);
        if (requested <= 0) return 0;
        long present = count(item);
        if (present == 0 && quantities.size() >= tier.slots()) return 0;
        long accepted = Math.min(requested, tier.perSlot() - present);
        if (accepted <= 0) return 0;
        quantities.put(item, present + accepted);
        invalidated = true;
        return accepted;
    }

    /** Removes no more than requested and returns the actual quantity removed. */
    public long extract(ItemIdentity item, long requested) {
        Objects.requireNonNull(item);
        if (requested <= 0) return 0;
        long present = count(item);
        long taken = Math.min(present, requested);
        if (taken <= 0) return 0;
        if (taken == present) quantities.remove(item);
        else quantities.put(item, present - taken);
        invalidated = true;
        return taken;
    }

    public Optional<Entry> firstEligible(Predicate<ItemIdentity> filter) {
        return orderedEntries().stream().filter(entry -> filter.test(entry.item())).findFirst();
    }

    public List<Entry> orderedEntries() {
        if (invalidated) {
            ArrayList<Entry> entries = new ArrayList<>(quantities.size());
            quantities.forEach((item, count) -> entries.add(new Entry(item, count)));
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
        if (!quantities.containsKey(item) && quantities.size() >= tier.slots()) throw new IllegalArgumentException("Too many saved slots");
        quantities.put(item, count);
        invalidated = true;
    }
}

package dev.frequencyworks.world;

import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.core.NetworkInventory;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** The only source of truth for stored items. This data lives in the Overworld even
 * when all access blocks are broken or all other dimensions are unloaded.
 * All reads and writes MUST occur on the logical server thread. */
public final class FrequencySavedData extends SavedData {
    private final Map<FrequencyId, StoredFrequency> frequencies = new HashMap<>();

    public static FrequencySavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FrequencySavedData::new, FrequencySavedData::load),
                "frequencyworks_frequencies");
    }

    public boolean exists(FrequencyId id) { return frequencies.containsKey(id); }

    /** Call ONLY after a player has successfully bound a Buffer to this frequency. */
    public StoredFrequency activateBuffer(FrequencyId id, BufferTier tier) {
        StoredFrequency data = frequencies.get(id);
        if (data == null) {
            data = new StoredFrequency(tier);
            frequencies.put(id, data);
            setDirty();
        } else if (data.promote(tier)) {
            setDirty();
        }
        return data;
    }

    public Optional<StoredFrequency> find(FrequencyId id) {
        return Optional.ofNullable(frequencies.get(id));
    }

    public Optional<StoredFrequency> portAccess(FrequencyId id, UUID owner) {
        return id.accessibleBy(owner) ? find(id) : Optional.empty();
    }

    public void changed() { setDirty(); }

    /** Each entry represents ONE kind of item (including all data components).
     * Counts are long, never stored inside an oversized vanilla ItemStack. */
    public static final class StoredFrequency {
        public static final class Entry {
            private final long id;
            private final ItemStack prototype; // count always 1
            private long quantity;
            private Entry(long id, ItemStack prototype, long quantity) {
                this.id = id;
                this.prototype = prototype.copyWithCount(1);
                this.quantity = quantity;
            }
            public long id() { return id; }
            public ItemStack prototype() { return prototype.copy(); }
            public long quantity() { return quantity; }
        }

        private BufferTier tier;
        private NetworkInventory.SortMode sorting = NetworkInventory.SortMode.QUANTITY_DESC;
        private final List<Entry> entries = new ArrayList<>();
        private long nextEntryId = 1L;
        private StoredFrequency(BufferTier tier) { this.tier = tier; }
        public BufferTier tier() { return tier; }
        public int occupiedSlots() { return entries.size(); }
        public NetworkInventory.SortMode sorting() { return sorting; }
        public long totalItems() { return entries.stream().mapToLong(e -> e.quantity).sum(); }
        public boolean promote(BufferTier upgraded) {
            if (upgraded.ordinal() <= tier.ordinal()) return false;
            tier = upgraded;
            return true;
        }
        public void sorting(NetworkInventory.SortMode setting) { sorting = Objects.requireNonNull(setting); }

        private Entry matching(ItemStack stack) {
            for (Entry entry : entries) {
                if (ItemStack.isSameItemSameComponents(entry.prototype, stack)) return entry;
            }
            return null;
        }

        public long count(ItemStack item) {
            Entry entry = matching(item);
            return entry == null ? 0 : entry.quantity;
        }

        public long capacityFor(ItemStack item) {
            if (item.isEmpty()) return 0;
            Entry entry = matching(item);
            if (entry == null) return entries.size() < tier.slots() ? tier.perSlot() : 0L;
            return Math.max(0L, tier.perSlot() - entry.quantity);
        }

        /** Returns the ACTUAL amount stored. Caller must handle any unexpected remainder. */
        public int insert(ItemStack item) {
            if (item.isEmpty()) return 0;
            int accepted = (int) Math.min((long) item.getCount(), capacityFor(item));
            if (accepted <= 0) return 0;
            Entry entry = matching(item);
            if (entry == null) entries.add(new Entry(nextEntryId++, item, accepted));
            else entry.quantity += accepted;
            return accepted;
        }

        /** Stable identity for GUI rows. A stale click must never retrieve a different item
         * just because another Port changed the sorting order meanwhile. */
        public Optional<Entry> findEntry(long id) {
            return entries.stream().filter(e -> e.id == id).findFirst();
        }

        public int withdraw(ItemStack item, int count) {
            Entry entry = matching(item);
            if (entry == null || count <= 0) return 0;
            int withdrawn = (int) Math.min(entry.quantity, count);
            entry.quantity -= withdrawn;
            if (entry.quantity == 0) entries.remove(entry);
            return withdrawn;
        }

        /** Small bounded scan, max 81 entries. No allocating/sorting on every Port tick. */
        public Optional<Entry> first(Predicate<ItemStack> eligible) {
            Comparator<Entry> cmp = switch (sorting) {
                case QUANTITY_ASC -> Comparator.comparingLong(Entry::quantity);
                case QUANTITY_DESC -> Comparator.comparingLong(Entry::quantity).reversed();
                case ALPHABETICAL -> Comparator.comparing(e -> e.prototype.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
                case MOD_ALPHABETICAL -> Comparator.comparing((Entry e) ->
                       net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.prototype.getItem()).getNamespace())
                       .thenComparing(e -> e.prototype.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
            };
            cmp = cmp.thenComparing(e -> e.prototype.getItem().toString())
                     .thenComparing(e -> e.prototype.getComponents().toString());
            Entry best = null;
            for (Entry e : entries) {
                if (e.quantity > 0 && eligible.test(e.prototype) && (best == null || cmp.compare(e, best) < 0)) best = e;
            }
            return Optional.ofNullable(best);
        }

        /** Used when opening a GUI, not during Port tick processing. */
        public List<Entry> sortedSnapshot() {
            ArrayList<Entry> ordered = new ArrayList<>(entries);
            Comparator<Entry> cmp = switch (sorting) {
                case QUANTITY_ASC -> Comparator.comparingLong(Entry::quantity);
                case QUANTITY_DESC -> Comparator.comparingLong(Entry::quantity).reversed();
                case ALPHABETICAL -> Comparator.comparing(e -> e.prototype.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
                case MOD_ALPHABETICAL -> Comparator.comparing((Entry e) ->
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.prototype.getItem()).getNamespace())
                    .thenComparing(e -> e.prototype.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
            };
            ordered.sort(cmp.thenComparing(e -> e.prototype.getItem().toString())
                    .thenComparing(e -> e.prototype.getComponents().toString()));
            return ordered;
        }
        public List<Entry> snapshot() { return List.copyOf(entries); }
    }

    private static FrequencySavedData load(CompoundTag root, HolderLookup.Provider registries) {
        FrequencySavedData result = new FrequencySavedData();
        ListTag networks = root.getList("Networks", Tag.TAG_COMPOUND);
        for (int n = 0; n < networks.size(); n++) {
            CompoundTag t = networks.getCompound(n);
            try {
                FrequencyId id = FrequencyNbt.decode(t.getCompound("Identity"));
                int tier = t.getInt("Tier");
                if (tier < 0 || tier >= BufferTier.values().length) continue;
                StoredFrequency network = new StoredFrequency(BufferTier.values()[tier]);
                try { network.sorting = NetworkInventory.SortMode.valueOf(t.getString("Sorting")); }
                catch (IllegalArgumentException ignored) { /* keep default */ }
                ListTag items = t.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag itemTag = items.getCompound(i);
                    ItemStack prototype = ItemStack.parseOptional(registries, itemTag.getCompound("Prototype"));
                    long count = itemTag.getLong("Amount");
                    if (prototype.isEmpty() || count <= 0 || count > network.tier.perSlot()
                            || network.entries.size() >= network.tier.slots()
                            || network.matching(prototype) != null) continue;
                    long entryId = itemTag.getLong("EntryId");
                    if (entryId <= 0 || network.findEntry(entryId).isPresent()) entryId = network.nextEntryId;
                    network.entries.add(new StoredFrequency.Entry(entryId, prototype, count));
                    network.nextEntryId = Math.max(network.nextEntryId, entryId + 1L);
                }
                result.frequencies.put(id, network);
            } catch (IllegalArgumentException ignored) { /* malformed network is not authorized */ }
        }
        return result;
    }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (var pair : frequencies.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.put("Identity", FrequencyNbt.encode(pair.getKey()));
            StoredFrequency network = pair.getValue();
            t.putInt("Tier", network.tier.ordinal());
            t.putString("Sorting", network.sorting.name());
            ListTag items = new ListTag();
            for (StoredFrequency.Entry entry : network.entries) {
                CompoundTag item = new CompoundTag();
                item.putLong("EntryId", entry.id);
                item.put("Prototype", entry.prototype.save(registries));
                item.putLong("Amount", entry.quantity);
                items.add(item);
            }
            t.put("Items", items);
            list.add(t);
        }
        root.put("Networks", list);
        return root;
    }
}

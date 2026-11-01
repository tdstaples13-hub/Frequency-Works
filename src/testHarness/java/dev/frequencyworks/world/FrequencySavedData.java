package dev.frequencyworks.world;
import dev.frequencyworks.core.*;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
/** Test-only simulated frequency using the separately tested real dependency-free core. */
public final class FrequencySavedData {
    public static final class StoredFrequency {
        public record Entry(ItemStack prototype, long quantity) {}
        private final NetworkInventory core = new NetworkInventory(BufferTier.TETHERED);
        private static ItemIdentity id(ItemStack s) { return new ItemIdentity(s.id(),"",s.id()); }
        public long capacityFor(ItemStack stack) {
            long present = core.count(id(stack));
            if (present == 0 && core.occupiedSlots() >= core.tier().slots()) return 0;
            return core.tier().perSlot() - present;
        }
        public int insert(ItemStack stack) { return (int) core.insert(id(stack), stack.getCount()); }
        public int withdraw(ItemStack stack, int count) { return (int) core.extract(id(stack), count); }
        public long count(String item) { return core.count(new ItemIdentity(item,"",item)); }
        public Optional<Entry> first(Predicate<ItemStack> filter) {
            return core.firstEligible(key -> filter.test(new ItemStack(key.registryId(),1,64)))
                .map(e -> new Entry(new ItemStack(e.item().registryId(),1,64),e.count()));
        }
    }
}

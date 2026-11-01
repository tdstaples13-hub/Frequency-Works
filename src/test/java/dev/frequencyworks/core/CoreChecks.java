package dev.frequencyworks.core;

import java.util.UUID;

/** Run: javac -d out src/main/java/dev/frequencyworks/core/*.java src/test/java/dev/frequencyworks/core/CoreChecks.java && java -ea -cp out dev.frequencyworks.core.CoreChecks */
public final class CoreChecks {
    public static void main(String[] args) {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        FrequencyId public042 = FrequencyId.publicFrequency(42);
        FrequencyId alice042 = FrequencyId.privateFrequency(42, alice);
        FrequencyId bob042 = FrequencyId.privateFrequency(42, bob);
        assert !alice042.equals(bob042);
        assert !alice042.equals(public042);
        FrequencyRegistry registry = new FrequencyRegistry();
        assert registry.portAccess(public042, alice).isEmpty(); // Port cannot create frequency
        var inv = registry.bindBuffer(alice042, BufferTier.TETHERED);
        assert inv == registry.bindBuffer(alice042, BufferTier.TETHERED);
        assert registry.portAccess(alice042, bob).isEmpty();
        assert registry.portAccess(alice042, alice).orElseThrow() == inv;
        assert registry.viewBuffer(alice042, alice, BufferTier.TETHERED).isPresent();
        registry.bindBuffer(alice042, BufferTier.DRAGONBOUND);
        assert inv.tier() == BufferTier.DRAGONBOUND;
        assert registry.viewBuffer(alice042, alice, BufferTier.TETHERED).isEmpty();
        assert !inv.upgrade(BufferTier.TETHERED); // never downgrade

        ItemIdentity iron = new ItemIdentity("minecraft:iron_ingot", "", "Iron Ingot");
        ItemIdentity gold = new ItemIdentity("minecraft:gold_ingot", "", "Gold Ingot");
        assert inv.insert(iron, 192) == 192;
        assert inv.insert(gold, 64) == 64;
        assert inv.firstEligible(i -> true).orElseThrow().item().equals(iron);
        assert inv.extract(iron, 64) == 64; // port A
        assert inv.extract(iron, 64) == 64; // port B takes SAME item
        assert inv.count(iron) == 64;
        assert inv.extract(iron, 999) == 64;
        assert inv.count(iron) == 0;
        assert inv.extract(iron, 1) == 0;
        assert inv.insert(gold, Long.MAX_VALUE) == BufferTier.DRAGONBOUND.perSlot() - 64;
        assert inv.count(gold) == BufferTier.DRAGONBOUND.perSlot();
        assert inv.insert(gold, 1) == 0; // full
        inv.setSortMode(NetworkInventory.SortMode.ALPHABETICAL);
        inv.setSortMode(NetworkInventory.SortMode.MOD_ALPHABETICAL);

        var itemBudget = new PortRates.ItemBudget();
        var stackBudget = new PortRates.StackBudget();
        int count = 0, ops = 0;
        for (int tick=0; tick<80; tick++) {
            count += itemBudget.next(0);
            ops += stackBudget.next(0);
        }
        assert count == 10 && ops == 10; // hopper pace
        for (int tier = 1; tier < 5; tier++) {
            var iBudget = new PortRates.ItemBudget();
            var sBudget = new PortRates.StackBudget();
            long iTotal = 0, sTotal = 0;
            for (int tick=0; tick<60; tick++) {
                iTotal += iBudget.next(tier);
                int chunk = sBudget.next(tier);
                assert chunk <= 4;
                sTotal += chunk;
            }
            assert iTotal == (long)60 * PortRates.TIERS[tier].singleItemsPerTickNumerator()/PortRates.TIERS[tier].singleItemsPerTickDenominator();
            assert sTotal == (long)60 * PortRates.TIERS[tier].stackOpsNumerator()/PortRates.TIERS[tier].stackOpsDenominator();
        }
        assert registry.visibleTo(bob).isEmpty();
        assert registry.visibleTo(alice).contains(alice042);
        System.out.println("All Frequency Works core checks passed.");
    }
}

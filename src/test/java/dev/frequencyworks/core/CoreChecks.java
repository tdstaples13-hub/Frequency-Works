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
        assert inv.insert(gold, Long.MAX_VALUE) == (long) BufferTier.DRAGONBOUND.slots() * BufferTier.DRAGONBOUND.perSlot() - 64;
        assert inv.occupiedSlots() == BufferTier.DRAGONBOUND.slots();
        assert inv.count(gold) == (long) BufferTier.DRAGONBOUND.slots() * BufferTier.DRAGONBOUND.perSlot();
        assert inv.insert(gold, 1) == 0; // all physical slots full
        var small = new NetworkInventory(BufferTier.TETHERED);
        ItemIdentity cobble = new ItemIdentity("minecraft:cobblestone", "", "Cobblestone");
        assert small.insert(cobble, 4096) == 4096;
        assert small.occupiedSlots() == 1;
        assert small.insert(cobble, 64) == 64; // fills next slot automatically
        assert small.occupiedSlots() == 2;
        assert small.count(cobble) == 4160;
        assert small.extract(cobble, 4098) == 4098;
        assert small.count(cobble) == 62;
        assert small.occupiedSlots() == 1;
        assert small.insert(cobble, 4096 * 26) == 4096 * 26;
        assert small.occupiedSlots() == 27;
        assert small.insert(cobble, 4096) == 4034;
        assert small.insert(cobble, 1) == 0;
        assert small.count(cobble) == 4096L * 27;
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
        // Sequencer backoff: one-second checks for a minute, then 100 seconds.
        long firstFailure=10_000L;
        assert SequencerRetrySchedule.nextAttempt(firstFailure,firstFailure)==firstFailure+20;
        assert SequencerRetrySchedule.nextAttempt(firstFailure,firstFailure+1_199)==firstFailure+1_219;
        assert SequencerRetrySchedule.nextAttempt(firstFailure,firstFailure+1_200)==firstFailure+3_200;
        assert SequencerRetrySchedule.nextAttempt(firstFailure,firstFailure+3_200)==firstFailure+5_200;
        // Any successful move resets firstFailure to the next blockage tick.
        assert SequencerRetrySchedule.nextAttempt(firstFailure+3_201,firstFailure+3_201)==firstFailure+3_221;
        assert registry.visibleTo(bob).isEmpty();
        assert registry.visibleTo(alice).contains(alice042);
        System.out.println("All Frequency Works core checks passed.");
    }
}

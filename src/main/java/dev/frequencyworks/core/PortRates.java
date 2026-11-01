package dev.frequencyworks.core;

/** Beta throughput. Tokens are operations, not individual item entities. */
public final class PortRates {
    private PortRates() {}
    public record Tier(int filterSlotsPerDirection, int singleItemsPerTickNumerator,
                       int singleItemsPerTickDenominator, int stackOpsNumerator,
                       int stackOpsDenominator) {}
    public static final Tier[] TIERS = {
        new Tier(1, 1, 8, 1, 8),
        new Tier(2, 2, 1, 1, 3),
        new Tier(4, 4, 1, 3, 4),
        new Tier(9, 9, 1, 5, 3),
        new Tier(20, 20, 1, 4, 1)
    };

    /** Provides a bounded number of logical operations each tick, with no idle burst banking. */
    public static final class StackBudget {
        private int credit;
        public int next(int tierIndex) {
            Tier t = TIERS[tierIndex];
            credit += t.stackOpsNumerator();
            int ops = credit / t.stackOpsDenominator();
            credit %= t.stackOpsDenominator();
            return Math.min(4, ops);
        }
    }

    /** Single mode: max count to transfer this tick, to batch identical items. */
    public static final class ItemBudget {
        private int credit;
        public int next(int tierIndex) {
            Tier t = TIERS[tierIndex];
            credit += t.singleItemsPerTickNumerator();
            int amount = credit / t.singleItemsPerTickDenominator();
            credit %= t.singleItemsPerTickDenominator();
            return amount;
        }
    }
}

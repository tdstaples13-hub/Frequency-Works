package dev.frequencyworks.security;

import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import java.util.UUID;

/** Pure policy shared by placed blocks, inventory items and NeoForge event hooks.
 * Null binding means an as-yet-unassigned, unprotected Buffer. */
public final class BufferSecurityPolicy {
    private BufferSecurityPolicy() {}

    public static boolean mayPlayerAccess(FrequencyId binding, UUID player) {
        return binding == null || binding.accessibleBy(player);
    }

    public static boolean mayPlayerBreak(FrequencyId binding, UUID player) {
        return mayPlayerAccess(binding, player);
    }

    public static boolean mayPlayerKeepItem(FrequencyId binding, UUID player) {
        return mayPlayerAccess(binding, player);
    }

    public static boolean survivesExplosion(BufferTier tier) {
        return tier.ordinal() >= BufferTier.BLAZING.ordinal();
    }

    public static boolean dropsOnExplosion(BufferTier tier, FrequencyId binding) {
        return !survivesExplosion(tier)
                && (binding == null || binding.scope() == FrequencyId.Scope.PUBLIC);
    }
}

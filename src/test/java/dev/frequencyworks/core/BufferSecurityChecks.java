package dev.frequencyworks.core;

import dev.frequencyworks.security.BufferSecurityPolicy;
import java.util.UUID;

public final class BufferSecurityChecks {
    private static void expect(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    public static void main(String[] args) {
        UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bob   = UUID.fromString("00000000-0000-0000-0000-000000000002");
        FrequencyId privateId = FrequencyId.privateFrequency(42, alice);
        FrequencyId publicId = FrequencyId.publicFrequency(42);
        expect(BufferSecurityPolicy.mayPlayerAccess(privateId, alice), "Owner GUI");
        expect(!BufferSecurityPolicy.mayPlayerAccess(privateId, bob), "No stranger GUI");
        expect(!BufferSecurityPolicy.mayPlayerBreak(privateId, bob), "No stranger break");
        expect(BufferSecurityPolicy.mayPlayerBreak(publicId, bob), "Public break");
        expect(!BufferSecurityPolicy.mayPlayerKeepItem(privateId, bob), "No stranger item");
        expect(BufferSecurityPolicy.mayPlayerKeepItem(privateId, alice), "Owner item");
        expect(BufferSecurityPolicy.mayPlayerKeepItem(publicId, bob), "Public item");
        expect(BufferSecurityPolicy.mayPlayerAccess(null, bob), "Unbound allowed");
        for (BufferTier tier : BufferTier.values()) {
            boolean sturdy = tier.ordinal() >= 2;
            expect(BufferSecurityPolicy.survivesExplosion(tier) == sturdy, "Immune tier " + tier);
            expect(BufferSecurityPolicy.dropsOnExplosion(tier, publicId) == !sturdy, "Public drop " + tier);
            expect(!BufferSecurityPolicy.dropsOnExplosion(tier, privateId), "Private no drop " + tier);
        }
        System.out.println("Buffer security policies: PASS");
    }
}

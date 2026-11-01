package dev.frequencyworks.core;

import java.util.*;

/** Mutable shared-frequency registry; call on the Minecraft server thread only. */
public final class FrequencyRegistry {
    private final Map<FrequencyId, NetworkInventory> networks = new HashMap<>();

    public boolean exists(FrequencyId key) { return networks.containsKey(key); }

    /** Only called by authorized first-time Buffer binding, never by a Transfer Port. */
    public NetworkInventory bindBuffer(FrequencyId key, BufferTier tier) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(tier);
        NetworkInventory existing = networks.get(key);
        if (existing != null) {
            existing.upgrade(tier);
            return existing;
        }
        NetworkInventory created = new NetworkInventory(tier);
        networks.put(key, created);
        return created;
    }

    /** Port may connect only to existing network, within its owner namespace. */
    public Optional<NetworkInventory> portAccess(FrequencyId key, UUID actingPlayer) {
        if (!key.accessibleBy(actingPlayer)) return Optional.empty();
        return Optional.ofNullable(networks.get(key));
    }

    public Optional<NetworkInventory> viewBuffer(FrequencyId key, UUID actingPlayer, BufferTier bufferTier) {
        if (!key.accessibleBy(actingPlayer)) return Optional.empty();
        NetworkInventory net = networks.get(key);
        if (net == null || net.tier().ordinal() > bufferTier.ordinal()) return Optional.empty();
        return Optional.of(net);
    }

    public Set<FrequencyId> visibleTo(UUID player) {
        Set<FrequencyId> result = new HashSet<>();
        for (var id : networks.keySet()) if (id.accessibleBy(player)) result.add(id);
        return Set.copyOf(result);
    }

    public Map<FrequencyId, NetworkInventory> all() { return Collections.unmodifiableMap(networks); }
}

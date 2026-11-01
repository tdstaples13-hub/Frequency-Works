package dev.frequencyworks.core;

import java.util.Objects;

/** Immutable item signature. componentsKey includes all item data relevant to stack equality. */
public record ItemIdentity(String registryId, String componentsKey, String displayName) {
    public ItemIdentity {
        Objects.requireNonNull(registryId);
        Objects.requireNonNull(componentsKey);
        Objects.requireNonNull(displayName);
        if (registryId.isBlank()) throw new IllegalArgumentException("item registry id missing");
    }
    public String namespace() {
        int colon = registryId.indexOf(':');
        return colon < 0 ? "minecraft" : registryId.substring(0, colon);
    }
    public String stableKey() { return registryId + "#" + componentsKey; }

    // Display names are language-dependent metadata, NOT part of item stack identity.
    @Override public boolean equals(Object other) {
        return other instanceof ItemIdentity id
                && registryId.equals(id.registryId)
                && componentsKey.equals(id.componentsKey);
    }
    @Override public int hashCode() { return Objects.hash(registryId, componentsKey); }
}

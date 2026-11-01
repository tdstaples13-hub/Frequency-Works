package dev.frequencyworks.core;

import java.util.Objects;
import java.util.UUID;

/** Server-side network identity. The owner is never supplied by an untrusted client. */
public record FrequencyId(int number, Scope scope, UUID owner) {
    public enum Scope { PUBLIC, PRIVATE }

    public FrequencyId {
        if (number < 0 || number > 999) throw new IllegalArgumentException("Frequency must be 000–999");
        Objects.requireNonNull(scope, "scope");
        if (scope == Scope.PRIVATE && owner == null) throw new IllegalArgumentException("Private frequency requires owner UUID");
        if (scope == Scope.PUBLIC && owner != null) throw new IllegalArgumentException("Public frequency may not have owner");
    }

    public static FrequencyId publicFrequency(int number) {
        return new FrequencyId(number, Scope.PUBLIC, null);
    }

    public static FrequencyId privateFrequency(int number, UUID owner) {
        return new FrequencyId(number, Scope.PRIVATE, Objects.requireNonNull(owner));
    }

    public String displayNumber() { return "%03d".formatted(number); }
    public boolean accessibleBy(UUID player) { return scope == Scope.PUBLIC || owner.equals(player); }
}

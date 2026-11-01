package dev.frequencyworks.world;

import dev.frequencyworks.core.FrequencyId;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Compact immutable frequency binding used by block entities and Buffer items. */
public final class FrequencyNbt {
    private FrequencyNbt() {}

    public static CompoundTag encode(FrequencyId id) {
        CompoundTag t = new CompoundTag();
        t.putInt("Number", id.number());
        t.putBoolean("Private", id.scope() == FrequencyId.Scope.PRIVATE);
        if (id.owner() != null) t.putUUID("Owner", id.owner());
        return t;
    }

    public static FrequencyId decode(CompoundTag t) {
        int number = t.getInt("Number");
        if (number < 0 || number > 999) throw new IllegalArgumentException("Bad frequency");
        if (!t.getBoolean("Private")) return FrequencyId.publicFrequency(number);
        if (!t.hasUUID("Owner")) throw new IllegalArgumentException("Missing private owner");
        UUID owner = t.getUUID("Owner");
        return FrequencyId.privateFrequency(number, owner);
    }
}

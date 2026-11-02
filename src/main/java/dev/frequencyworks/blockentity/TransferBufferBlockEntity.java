package dev.frequencyworks.blockentity;

import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.registration.ModContent;
import dev.frequencyworks.world.FrequencyNbt;
import dev.frequencyworks.world.FrequencySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One-time binding only. The inventory is NOT in this block entity. */
public final class TransferBufferBlockEntity extends BlockEntity {
    private FrequencyId binding;
    private long generation;

    public TransferBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BUFFER_ENTITY.get(), pos, state);
    }

    public FrequencyId binding() { return binding; }
    public long generation() { return generation; }
    public void clearBinding() { binding = null; generation = 0; setChanged(); }
    /** Lazily reset any Buffer from a deleted frequency, including unloaded chunks. */
    public void ensureCurrent() {
        if (binding != null && level instanceof ServerLevel server
                && !FrequencySavedData.get(server).isCurrent(binding, generation)) clearBinding();
    }

    public boolean bind(FrequencyId target, BufferTier tier) {
        if (binding != null || !(level instanceof ServerLevel server)) return false;
        binding = target;
        FrequencySavedData data = FrequencySavedData.get(server);
        data.activateBuffer(target, tier);
        generation = data.generation(target);
        setChanged();
        return true;
    }

    public void restoreFromItem(FrequencyId id, long storedGeneration, BufferTier tier, java.util.UUID placer) {
        if (binding != null || !(level instanceof ServerLevel server)) return;
        FrequencySavedData data = FrequencySavedData.get(server);
        if (!data.isCurrent(id, storedGeneration)) { clearBinding(); return; }
        binding = id;
        generation = storedGeneration;
        setChanged();
        if (id.accessibleBy(placer)) data.activateBuffer(id, tier);
    }

    public void activate(BufferTier tier) {
        ensureCurrent();
        if (binding != null && level instanceof ServerLevel server)
            FrequencySavedData.get(server).activateBuffer(binding, tier);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (binding != null) { tag.put("Frequency", FrequencyNbt.encode(binding)); tag.putLong("Generation", generation); }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        binding = tag.contains("Frequency") ? FrequencyNbt.decode(tag.getCompound("Frequency")) : null;
        generation = tag.getLong("Generation");
    }
}

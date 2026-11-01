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

    public TransferBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BUFFER_ENTITY.get(), pos, state);
    }

    public FrequencyId binding() { return binding; }

    public boolean bind(FrequencyId target, BufferTier tier) {
        if (binding != null || !(level instanceof ServerLevel server)) return false;
        binding = target;
        setChanged();
        FrequencySavedData.get(server).activateBuffer(target, tier);
        return true;
    }

    public void restoreFromItem(FrequencyId id, BufferTier tier, java.util.UUID placer) {
        if (binding != null || !(level instanceof ServerLevel server)) return;
        binding = id;
        setChanged();
        if (id.accessibleBy(placer)) FrequencySavedData.get(server).activateBuffer(id, tier);
    }

    public void activate(BufferTier tier) {
        if (binding != null && level instanceof ServerLevel server)
            FrequencySavedData.get(server).activateBuffer(binding, tier);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (binding != null) tag.put("Frequency", FrequencyNbt.encode(binding));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        binding = tag.contains("Frequency") ? FrequencyNbt.decode(tag.getCompound("Frequency")) : null;
    }
}

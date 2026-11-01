package dev.frequencyworks.block;

import dev.frequencyworks.blockentity.TransferBufferBlockEntity;
import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.item.TransferBufferItem;
import dev.frequencyworks.world.FrequencySavedData;
import dev.frequencyworks.security.BufferSecurityPolicy;
import java.util.function.BiConsumer;
import net.minecraft.world.level.Explosion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class TransferBufferBlock extends Block implements EntityBlock {
    private final BufferTier tier;
    public TransferBufferBlock(Properties properties, BufferTier tier) {
        super(properties);
        this.tier = tier;
    }
    public BufferTier tier() { return tier; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransferBufferBlockEntity(pos, state);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(placer instanceof Player player) || level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof TransferBufferBlockEntity buffer) {
            FrequencyId id = TransferBufferItem.binding(stack);
            if (id != null) buffer.restoreFromItem(id, tier, player.getUUID());
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                dev.frequencyworks.network.ConfigNetworking.openBuffer(serverPlayer, pos);
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof TransferBufferBlockEntity buffer
                    && !BufferSecurityPolicy.mayPlayerAccess(buffer.binding(), player.getUUID())) {
                player.displayClientMessage(Component.literal("You do not have permission to access this Transfer Buffer."), true);
                return InteractionResult.FAIL;
            }
            dev.frequencyworks.network.ConfigNetworking.openBuffer(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative() && level.getBlockEntity(pos) instanceof TransferBufferBlockEntity buffer
                && BufferSecurityPolicy.mayPlayerBreak(buffer.binding(), player.getUUID())) {
            ItemStack item = new ItemStack(this);
            if (buffer.binding() != null) TransferBufferItem.setBinding(item, buffer.binding());
            popResource(level, pos, item);
        }
        // No vanilla loot-table drop for Buffers: prevent a second unbound item.
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                            BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (BufferSecurityPolicy.survivesExplosion(tier)) return;
        // The block has no loot table: it MUST NOT also generate vanilla drops.
        // Public low-tier Buffer drops carry their bound frequency data.
        // Private low-tier Buffer drops nothing (the frequency inventory remains saved).
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TransferBufferBlockEntity buffer) {
            if (BufferSecurityPolicy.dropsOnExplosion(tier, buffer.binding())) {
                ItemStack result = new ItemStack(this);
                if (buffer.binding() != null) TransferBufferItem.setBinding(result, buffer.binding());
                dropConsumer.accept(result, pos);
            }
        }
        // Missing block-entity data: drop nothing rather than risk exposing a
        // private binding whose ownership cannot be established.
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }
}

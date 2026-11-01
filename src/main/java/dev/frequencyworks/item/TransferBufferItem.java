package dev.frequencyworks.item;

import dev.frequencyworks.core.BufferTier;
import dev.frequencyworks.core.FrequencyId;
import dev.frequencyworks.world.FrequencyNbt;
import dev.frequencyworks.world.FrequencySavedData;
import dev.frequencyworks.security.BufferSecurityPolicy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Bound frequency is stored ONLY on Buffer items, never on Port items. */
public final class TransferBufferItem extends BlockItem {
    private final BufferTier tier;
    public TransferBufferItem(Block block, Properties properties, BufferTier tier) {
        super(block, properties.stacksTo(1));
        this.tier = tier;
    }
    public BufferTier tier() { return tier; }

    public static FrequencyId binding(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        CompoundTag tag = custom.copyTag();
        if (!tag.contains("FrequencyWorksBinding")) return null;
        try { return FrequencyNbt.decode(tag.getCompound("FrequencyWorksBinding")); }
        catch (IllegalArgumentException ex) { return null; }
    }

    public static void setBinding(ItemStack stack, FrequencyId id) {
        CompoundTag tag = new CompoundTag();
        tag.put("FrequencyWorksBinding", FrequencyNbt.encode(id));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override public net.minecraft.world.InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null && !BufferSecurityPolicy.mayPlayerKeepItem(binding(context.getItemInHand()), player.getUUID())) {
            if (!context.getLevel().isClientSide())
                player.displayClientMessage(Component.literal("This private Transfer Buffer belongs to another player."), true);
            return net.minecraft.world.InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        if (BufferSecurityPolicy.mayPlayerKeepItem(binding(stack), player.getUUID())) return;
        // A non-owner may receive an item through an unusual inventory operation.
        // Eject only the illicit stack rather than deleting it. Ground pickup is denied.
        if (slot >= 0 && slot < player.getInventory().getContainerSize()
                && player.getInventory().getItem(slot) == stack) {
            ItemStack detached = stack.copy();
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.drop(detached, false);
            player.displayClientMessage(Component.literal("You cannot carry another player's private Transfer Buffer."), true);
        }
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BufferSecurityPolicy.mayPlayerAccess(binding(stack), player.getUUID())) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            dev.frequencyworks.network.ConfigNetworking.openHeldBuffer(serverPlayer, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}

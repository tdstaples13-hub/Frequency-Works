package dev.frequencyworks.security;

import dev.frequencyworks.block.TransferBufferBlock;
import dev.frequencyworks.blockentity.TransferBufferBlockEntity;
import dev.frequencyworks.item.TransferBufferItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Server-first safety for private buffers. Deliberately does not intercept
 * automation IItemHandlers; public/private restrictions apply to players. */
public final class BufferSecurityEvents {
    private BufferSecurityEvents() {}

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getState().getBlock() instanceof TransferBufferBlock)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof TransferBufferBlockEntity buffer)) return;
        buffer.ensureCurrent();
        if (!BufferSecurityPolicy.mayPlayerBreak(buffer.binding(), event.getPlayer().getUUID())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("This Transfer Buffer belongs to another player."), true);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        // Additional safeguard if another mod uses very high blast power.
        event.getAffectedBlocks().removeIf(pos ->
            event.getLevel().getBlockState(pos).getBlock() instanceof TransferBufferBlock buffer
            && BufferSecurityPolicy.survivesExplosion(buffer.tier()));
    }

    public static void onGroundPickup(ItemEntityPickupEvent.Pre event) {
        ItemStack item = event.getItemEntity().getItem();
        if (item.getItem() instanceof TransferBufferItem
            && !BufferSecurityPolicy.mayPlayerKeepItem(TransferBufferItem.binding(item), event.getPlayer().getUUID())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    public static void onSlotStack(ItemStackedOnOtherEvent event) {
        if (!(event.getPlayer().level() instanceof net.minecraft.server.level.ServerLevel)) return;
        ItemStack carried = event.getCarriedItem();
        ItemStack slot = event.getStackedOnItem();
        if ((carried.getItem() instanceof TransferBufferItem
             && !BufferSecurityPolicy.mayPlayerKeepItem(TransferBufferItem.binding(carried), event.getPlayer().getUUID()))
            || (slot.getItem() instanceof TransferBufferItem
             && !BufferSecurityPolicy.mayPlayerKeepItem(TransferBufferItem.binding(slot), event.getPlayer().getUUID()))) {
            event.setCanceled(true);
        }
    }
}

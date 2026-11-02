package dev.frequencyworks.item;

import dev.frequencyworks.network.ConfigNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Deliberately no recipe and no creative-tab entry. /give is intended for admins only.
 * Even an item obtained by a survival player has NO debug privileges. */
public final class FrequencyDebuggerItem extends Item {
    public FrequencyDebuggerItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
            ConfigNetworking.openHeldDebugger(serverPlayer,hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide());
    }
}

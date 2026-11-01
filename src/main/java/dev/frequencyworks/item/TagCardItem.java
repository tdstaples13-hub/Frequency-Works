package dev.frequencyworks.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A tag selector, not an executable Sequence Tablet. Its data survives in item form. */
public final class TagCardItem extends Item {
    public TagCardItem(Properties properties) { super(properties.stacksTo(1)); }

    public static String tag(ItemStack stack) {
        if (!(stack.getItem() instanceof TagCardItem)) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString("FrequencyWorksItemTag");
    }
    public static boolean setTag(ItemStack stack, String raw) {
        if (!(stack.getItem() instanceof TagCardItem)) return false;
        String normalized = raw.trim().replaceFirst("^#", "").toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 128 || ResourceLocation.tryParse(normalized) == null) return false;
        CompoundTag data = new CompoundTag();
        data.putString("FrequencyWorksItemTag", normalized);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer server)
            dev.frequencyworks.network.ConfigNetworking.openHeldTag(server, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}

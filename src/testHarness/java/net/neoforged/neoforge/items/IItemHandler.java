package net.neoforged.neoforge.items;
import net.minecraft.world.item.ItemStack;
/** TEST-ONLY Java stub: intentionally NOT distributed in the mod JAR. */
public interface IItemHandler {
    int getSlots();
    ItemStack extractItem(int slot, int amount, boolean simulate);
    ItemStack insertItem(int slot, ItemStack item, boolean simulate);
}

package dev.frequencyworks.menu;

import dev.frequencyworks.blockentity.TransferPortBlockEntity;
import dev.frequencyworks.registration.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Real vanilla player inventory beneath two *ghost* filter banks.
 * Filter rules never become physical item slots or consume the cursor stack.
 */
public final class PortMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 208;
    private final BlockPos portPos;
    private final int filterCapacity;
    private CompoundTag state = new CompoundTag();

    public PortMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readBlockPos(), data.readVarInt());
    }

    public PortMenu(int containerId, Inventory inventory, BlockPos pos, int capacity) {
        super(ModContent.PORT_MENU.get(), containerId);
        this.portPos = pos;
        this.filterCapacity = Math.max(3, Math.min(15, capacity));
        int x = inventoryLeft();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        x + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, INVENTORY_Y + 58));
        }
    }

    public int columns() { return filterCapacity / 3; }
    public int filterCapacity() { return filterCapacity; }
    public int panelWidth() { return Math.max(196, columns() * 36 + 32); }
    public int inventoryLeft() { return (panelWidth() - 162) / 2; }
    public boolean at(BlockPos target) { return portPos.equals(target); }
    public BlockPos portPos() { return portPos; }
    public CompoundTag state() { return state; }
    public void setState(CompoundTag incoming) { state = incoming == null ? new CompoundTag() : incoming.copy(); }

    @Override public boolean stillValid(Player player) {
        if (!player.level().hasChunkAt(portPos)) return false;
        if (player.distanceToSqr(portPos.getX() + .5, portPos.getY() + .5, portPos.getZ() + .5) > 64) return false;
        return player.level().getBlockEntity(portPos) instanceof TransferPortBlockEntity;
    }

    /** Standard inventory/hotbar shift-click movement; ghost filters are not Slots. */
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotId < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}

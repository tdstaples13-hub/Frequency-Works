package dev.frequencyworks.menu;

import dev.frequencyworks.item.SequenceTabletItem;
import dev.frequencyworks.registration.ModContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tablet editor: real player slots, nine virtual instruction ghosts on screen. */
public final class TabletMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y=126;
    private final Inventory inventory;
    private final int hand;
    private CompoundTag state=new CompoundTag();
    public TabletMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf){this(id,inv,buf.readVarInt());}
    public TabletMenu(int id,Inventory inv,int hand){
        super(ModContent.TABLET_MENU.get(),id);
        this.inventory=inv;
        this.hand=hand;
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)
            addSlot(new Slot(inv,9+r*9+c,8+c*18,INVENTORY_Y+r*18));
        for(int c=0;c<9;c++)
            addSlot(new Slot(inv,c,8+c*18,INVENTORY_Y+58));
    }
    public int hand(){return hand;}
    public CompoundTag state(){return state;}
    public void setState(CompoundTag value){state=value==null?new CompoundTag():value.copy();}
    @Override public boolean stillValid(Player player){
        ItemStack held=player.getItemInHand(hand==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        return held.getItem() instanceof SequenceTabletItem;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){
        if(slot<0||slot>=slots.size())return ItemStack.EMPTY;
        Slot source=slots.get(slot);
        if(!source.hasItem())return ItemStack.EMPTY;
        ItemStack stack=source.getItem(),copy=stack.copy();
        if(slot<27){
            if(!moveItemStackTo(stack,27,36,false))return ItemStack.EMPTY;
        }else if(!moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();
        return copy;
    }
}
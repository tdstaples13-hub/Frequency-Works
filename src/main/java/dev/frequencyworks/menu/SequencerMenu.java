package dev.frequencyworks.menu;

import dev.frequencyworks.blockentity.SequencerBlockEntity;
import dev.frequencyworks.item.SequenceTabletItem;
import dev.frequencyworks.registration.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Standard nine-slot chest menu, with nine programmable Tablet slots only. */
public final class SequencerMenu extends AbstractContainerMenu {
    private final Container tablets;
    private final BlockPos pos;
    private final SequencerBlockEntity sequencer;
    private final ContainerData counters;

    public SequencerMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf){
        this(id,inv,buf.readBlockPos(),null);
    }
    public SequencerMenu(int id,Inventory inv,BlockPos pos,SequencerBlockEntity be){
        super(ModContent.SEQUENCER_MENU.get(),id);
        this.pos=pos;
        this.sequencer=be;
        this.tablets=be==null?new SimpleContainer(9):be;
        for(int col=0;col<9;col++){
            addSlot(new Slot(tablets,col,8+col*18,18){
                @Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof SequenceTabletItem;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col+row*9+9,8+col*18,49+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,107));
        counters=be==null?new SimpleContainerData(4):new ContainerData(){
            @Override public int get(int index){return be.runtime(index);}
            @Override public void set(int index,int value){}
            @Override public int getCount(){return 4;}
        };
        addDataSlots(counters);
    }
    public int tabletIndex(){return counters.get(0);}
    public int stepIndex(){return counters.get(1);}
    public int remaining(){return counters.get(2);}
    public boolean stopped(){return counters.get(3)!=0;}
    @Override public boolean stillValid(Player player){
        return sequencer==null || sequencer.stillValid(player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<9){
            if(!moveItemStackTo(stack,9,45,true))return ItemStack.EMPTY;
        }else if(stack.getItem() instanceof SequenceTabletItem){
            if(!moveItemStackTo(stack,0,9,false))return ItemStack.EMPTY;
        }else if(index<36){
            if(!moveItemStackTo(stack,36,45,false))return ItemStack.EMPTY;
        }else if(!moveItemStackTo(stack,9,36,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        return original;
    }
}
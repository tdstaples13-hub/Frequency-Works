package dev.frequencyworks.world;

import dev.frequencyworks.block.TransferBufferBlock;
import dev.frequencyworks.blockentity.TransferBufferBlockEntity;
import dev.frequencyworks.world.FrequencySavedData.StoredFrequency;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Normal NeoForge inventory interface for a *placed* Buffer.
 * The item handler is a projection of the authoritative shared frequency:
 * never an oversized ItemStack, never a second inventory.
 *
 * A lower-tier access point cannot automate a higher-tier frequency.
 */
public final class BufferItemHandler implements IItemHandler {
    private final TransferBufferBlockEntity buffer;
    public BufferItemHandler(TransferBufferBlockEntity buffer){this.buffer=buffer;}
    private StoredFrequency storage(){
        if(!(buffer.getLevel() instanceof ServerLevel server))return null;
        buffer.ensureCurrent();
        if(buffer.binding()==null || !(buffer.getBlockState().getBlock() instanceof TransferBufferBlock block))return null;
        var data=FrequencySavedData.get(server);
        StoredFrequency store=data.find(buffer.binding()).orElse(null);
        return store!=null && store.tier().ordinal()<=block.tier().ordinal()?store:null;
    }
    private int slotCapacity(){
        if(buffer.getBlockState().getBlock() instanceof TransferBufferBlock block)return block.tier().slots();
        return 0;
    }
    @Override public int getSlots(){return slotCapacity();}
    private StoredFrequency.Entry entry(StoredFrequency store,int slot){
        if(store==null||slot<0||slot>=getSlots())return null;
        var entries=store.sortedSnapshot();
        return slot<entries.size()?entries.get(slot):null;
    }
    @Override public ItemStack getStackInSlot(int slot){
        StoredFrequency store=storage();
        var e=entry(store,slot);
        if(e==null)return ItemStack.EMPTY;
        ItemStack stack=e.prototype();
        return stack.copyWithCount((int)Math.min(e.quantity(),stack.getMaxStackSize()));
    }
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
        if(stack.isEmpty())return ItemStack.EMPTY;
        StoredFrequency store=storage();
        if(store==null||slot<0||slot>=getSlots())return stack;
        var current=entry(store,slot);
        if(current!=null && !ItemStack.isSameItemSameComponents(current.prototype(),stack))
            return stack;
        // Storage may fill another partial stack of the same type; the public
        // slot view is sorted and not an independent mutable inventory.
        int accepted=(int)Math.min((long)stack.getCount(),store.capacityFor(stack));
        if(accepted<=0)return stack;
        if(!simulate){
            accepted=store.insert(stack.copyWithCount(accepted));
            if(accepted>0)FrequencySavedData.get((ServerLevel)buffer.getLevel()).changed();
        }
        return accepted>=stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-accepted);
    }
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
        StoredFrequency store=storage();
        var e=entry(store,slot);
        if(e==null||amount<=0)return ItemStack.EMPTY;
        ItemStack stack=e.prototype();
        int count=(int)Math.min(Math.min(e.quantity(),amount),stack.getMaxStackSize());
        if(count<=0)return ItemStack.EMPTY;
        if(!simulate){
            count=store.withdrawEntry(e.id(),count);
            if(count>0)FrequencySavedData.get((ServerLevel)buffer.getLevel()).changed();
        }
        return stack.copyWithCount(count);
    }
    @Override public int getSlotLimit(int slot){
        return slot<0||slot>=getSlots()?0:(int)Math.min(Integer.MAX_VALUE,
                ((TransferBufferBlock)buffer.getBlockState().getBlock()).tier().perSlot());
    }
    @Override public boolean isItemValid(int slot,ItemStack stack){
        StoredFrequency store=storage();
        if(store==null||slot<0||slot>=getSlots()||stack.isEmpty())return false;
        var e=entry(store,slot);
        return e==null || ItemStack.isSameItemSameComponents(e.prototype(),stack);
    }
}
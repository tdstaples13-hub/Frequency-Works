package dev.frequencyworks.tests;
import dev.frequencyworks.world.PortTransferService;
import dev.frequencyworks.world.FrequencySavedData.StoredFrequency;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Exercises the REAL PortTransferService source against simplified handler contracts. */
public final class PortTransferChecks {
    public static void main(String[] args) {
        StoredFrequency freq = new StoredFrequency();
        Inventory chest = new Inventory("iron",192,64);
        // Two independent Ports see the same iron. They don't reserve separate item IDs.
        assert PortTransferService.pullFromMachine(chest,freq,64,0)==64;
        assert PortTransferService.pullFromMachine(chest,freq,64,0)==64;
        assert freq.count("iron")==128;
        assert chest.quantity==64;
        Inventory dest = new Inventory("iron",0,64);
        assert PortTransferService.pushToMachine(dest,freq,64,0)==64;
        assert dest.quantity==64 && freq.count("iron")==64;
        // Destination full leaves all frequency items untouched.
        assert PortTransferService.pushToMachine(dest,freq,64,0)==0;
        assert freq.count("iron")==64;
        // Moving a partial amount must leave remainder at its source.
        Inventory partial = new Inventory("gold",24,32);
        assert PortTransferService.pullFromMachine(partial,freq,64,0)==24;
        assert partial.quantity==0;
        Inventory restricted = new Inventory("gold",31,32);
        assert PortTransferService.pushToMachine(restricted,freq,64,0)==0; // iron first (sorted by amount), blocked
        assert freq.count("iron")==64 && freq.count("gold")==24;
        // Hopper-style first-fit: the first slot receives items until full.
        // A chest must not round-robin its items just because multiple Port
        // transfer attempts occur across successive server ticks.
        StoredFrequency sorted = new StoredFrequency();
        assert sorted.insert(new ItemStack("cobblestone",64,64))==64;
        assert sorted.insert(new ItemStack("cobblestone",64,64))==64;
        assert sorted.insert(new ItemStack("cobblestone",64,64))==64;
        InventorySlots hopper = new InventorySlots(27,"cobblestone",64);
        assert PortTransferService.pushToMachine(hopper,sorted,32,0)==32;
        assert hopper.count(0)==32 && hopper.count(1)==0;
        assert PortTransferService.pushToMachine(hopper,sorted,32,0)==32;
        assert hopper.count(0)==64 && hopper.count(1)==0;
        assert PortTransferService.pushToMachine(hopper,sorted,64,0)==64;
        assert hopper.count(0)==64 && hopper.count(1)==64;
        // Slots 0..17 already full: search must reach slot 18.
        for(int i=2;i<18;i++)hopper.fill(i,64);
        assert PortTransferService.pushToMachine(hopper,sorted,32,0)==32;
        assert hopper.count(18)==32 && hopper.count(19)==0;

        // Imports also follow slot order instead of rotating occupied cells.
        StoredFrequency imported = new StoredFrequency();
        InventorySlots input = new InventorySlots(27,"iron",64);
        input.fill(0,10);input.fill(1,30);
        assert PortTransferService.pullFromMachine(input,imported,5,0)==5;
        assert input.count(0)==5 && input.count(1)==30;
        assert PortTransferService.pullFromMachine(input,imported,5,0)==5;
        assert input.count(0)==0 && input.count(1)==30;
        assert PortTransferService.pullFromMachine(input,imported,5,0)==5;
        assert input.count(0)==0 && input.count(1)==25;

        System.out.println("PortTransferService tests passed (shared frequency, capacity, hopper first-fit slot order, no loss).");
    }
    static final class Inventory implements IItemHandler {
        final String type;
        final int maxSlot;
        int quantity;
        Inventory(String type,int count,int maxSlot) { this.type=type; quantity=count; this.maxSlot=maxSlot; }
        @Override public int getSlots() { return 1; }
        @Override public ItemStack extractItem(int slot,int amount, boolean sim) {
            int take = Math.min(Math.max(0,amount),quantity);
            if (!sim) quantity-=take;
            return new ItemStack(type,take,64);
        }
        @Override public ItemStack insertItem(int slot,ItemStack stack, boolean sim) {
            if (!stack.id().equals(type)) return stack.copy();
            int moved=Math.min(stack.getCount(),maxSlot-quantity);
            if (!sim) quantity+=moved;
            return stack.copyWithCount(stack.getCount()-moved);
        }
    }
    static final class InventorySlots implements IItemHandler {
        final int[] quantities;
        final String id;
        final int limit;
        InventorySlots(int slots,String id,int limit){
            this.quantities=new int[slots];this.id=id;this.limit=limit;
        }
        int count(int slot){return quantities[slot];}
        void fill(int slot,int value){quantities[slot]=value;}
        @Override public int getSlots(){return quantities.length;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
            int taken=Math.min(Math.max(0,amount),quantities[slot]);
            if(!simulate)quantities[slot]-=taken;
            return new ItemStack(id,taken,limit);
        }
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
            if(!id.equals(stack.id()))return stack.copy();
            int accepted=Math.min(stack.getCount(),Math.max(0,limit-quantities[slot]));
            if(!simulate)quantities[slot]+=accepted;
            return stack.copyWithCount(stack.getCount()-accepted);
        }
    }
}

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
        System.out.println("PortTransferService tests passed (shared frequency, capacity, no loss in normal handlers).");
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
}

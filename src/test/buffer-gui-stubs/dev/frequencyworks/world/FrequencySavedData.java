package dev.frequencyworks.world;
import java.util.*;
import net.minecraft.world.item.ItemStack;
public class FrequencySavedData {
 public static class StoredFrequency {
    public static class Entry {
        private final long id; private final ItemStack item; private long quantity;
        Entry(long id, ItemStack item, long quantity){this.id=id;this.item=item.copyWithCount(1);this.quantity=quantity;}
        public ItemStack prototype(){return item.copyWithCount(1);}
        public long quantity(){return quantity;}
    }
    private final Map<Long,Entry> entries=new HashMap<>(); private long next=1;
    public long seed(ItemStack item, long quantity){long id=next++;entries.put(id,new Entry(id,item,quantity));return id;}
    public Optional<Entry> findEntry(long id){return Optional.ofNullable(entries.get(id));}
    public int withdraw(ItemStack item,int amount){
        for(Entry e:new ArrayList<>(entries.values())) if(ItemStack.isSameItemSameComponents(item,e.item)) {
            int taken=(int)Math.min(e.quantity,amount);e.quantity-=taken;
            if(e.quantity==0)entries.remove(e.id);
            return taken;
        } return 0;
    }
    public int insert(ItemStack stack){
        if(stack.isEmpty())return 0;
        for(Entry e:entries.values()) if(ItemStack.isSameItemSameComponents(stack,e.item)){
            int take=(int)Math.min(stack.getCount(),4096-e.quantity);e.quantity+=take;return take;
        }
        seed(stack,stack.getCount());return stack.getCount();
    }
 }
}

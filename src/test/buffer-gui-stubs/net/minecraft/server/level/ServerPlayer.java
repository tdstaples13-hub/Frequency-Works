package net.minecraft.server.level;
import net.minecraft.world.item.ItemStack;
public class ServerPlayer {
    private final Inventory inventory=new Inventory();
    public final ContainerMenu containerMenu=new ContainerMenu();
    public Inventory getInventory(){return inventory;}
    public static class ContainerMenu {public void broadcastChanges() {}}
    public static class Inventory {
        private final ItemStack[] items=new ItemStack[36];
        public ItemStack getItem(int slot){ItemStack v=items[slot];return v==null?new ItemStack("",0,64):v;}
        public void setItem(int slot,ItemStack item){items[slot]=item;}
        public void setChanged() {}
    }
}

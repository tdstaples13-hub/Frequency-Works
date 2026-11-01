package net.minecraft.world.item;
public class ItemStack {
    private final String id; private int amount; private final int maxStack;
    public ItemStack(String id, int amount, int maxStack) { this.id=id; this.amount=amount; this.maxStack=maxStack; }
    public boolean isEmpty() { return amount<=0; }
    public int getCount(){return amount;}
    public int getMaxStackSize(){return maxStack;}
    public void shrink(int by){amount-=by;}
    public void grow(int by){amount+=by;}
    public ItemStack copyWithCount(int amount){return new ItemStack(id,amount,maxStack);}
    public static boolean isSameItemSameComponents(ItemStack a,ItemStack b){return a.id.equals(b.id);}
}

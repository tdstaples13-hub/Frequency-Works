package net.minecraft.world.item;
/** TEST-ONLY Java stub: intentionally NOT distributed in the mod JAR. */
public final class ItemStack {
    private final String id;
    private final int max;
    private final int count;
    public ItemStack(String id, int count, int max) { this.id = id; this.count = count; this.max = max; }
    public String id() { return id; }
    public boolean isEmpty() { return count <= 0; }
    public int getCount() { return count; }
    public int getMaxStackSize() { return max; }
    public ItemStack copy() { return new ItemStack(id, count, max); }
    public ItemStack copyWithCount(int n) { return new ItemStack(id,n,max); }
    @Override public String toString() { return id+"×"+count; }
}

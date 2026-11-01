package dev.frequencyworks.core;

public enum BufferTier {
    TETHERED(27, 4096L),
    GILDED(36, 16384L),
    BLAZING(45, 65536L),
    ENDWOVEN(54, 262144L),
    DRAGONBOUND(81, 2147483647L);

    private final int slots;
    private final long perSlot;
    BufferTier(int slots, long perSlot) { this.slots = slots; this.perSlot = perSlot; }
    public int slots() { return slots; }
    public long perSlot() { return perSlot; }
}

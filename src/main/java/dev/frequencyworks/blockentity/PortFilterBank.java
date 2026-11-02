package dev.frequencyworks.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** One whole-port filter bank. Strings are item IDs or #item-tag IDs.
 * Parsing happens ONLY when a player changes the settings, not on every transfer.
 */
public final class PortFilterBank implements Predicate<ItemStack> {
    private boolean whitelist = true;
    // Keep legacy filters saved for rollback, but never evaluate entries above
    // the tier's new three-row visible limit.
    private int activeCapacity = 20;
    private final List<String> rules = new ArrayList<>();
    private final List<Predicate<ItemStack>> compiled = new ArrayList<>();

    public boolean whitelist() { return whitelist; }
    public int size() { return rules.size(); }
    public String get(int index) { return index >= 0 && index < rules.size() ? rules.get(index) : ""; }
    public List<String> rules() { return List.copyOf(rules); }

    public void whitelist(boolean value) { whitelist = value; }

    /** An empty bank never restricts transfer, including an empty whitelist. */
    @Override public boolean test(ItemStack stack) {
        if (stack.isEmpty() || compiled.isEmpty()) return !stack.isEmpty();
        boolean matched = false;
        for (var predicate : compiled) { if (predicate.test(stack)) { matched = true; break; } }
        return whitelist == matched;
    }

    private static Predicate<ItemStack> compile(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        boolean isTag = s.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(isTag ? s.substring(1) : s);
        if (id == null || (!isTag && !BuiltInRegistries.ITEM.containsKey(id)))
            throw new IllegalArgumentException("Unknown item or malformed tag: " + raw);
        if (isTag) {
            TagKey<net.minecraft.world.item.Item> key = TagKey.create(Registries.ITEM, id);
            return stack -> stack.is(key);
        }
        return stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    public boolean put(int slot, String newRule, int capacity) {
        if (slot < 0 || slot >= capacity || capacity > 20) return false;
        String normalized = newRule.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 128) return false;
        if (!normalized.isEmpty()) {
            try { compile(normalized); } catch (IllegalArgumentException ex) { return false; }
        }
        activeCapacity = capacity;
        while (rules.size() <= slot) rules.add("");
        rules.set(slot, normalized);
        rebuild();
        return true;
    }

    private void rebuild() {
        compiled.clear();
        for (int i = 0; i < Math.min(activeCapacity, rules.size()); i++) {
            String rule = rules.get(i);
            if (!rule.isBlank()) compiled.add(compile(rule));
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Whitelist", whitelist);
        ListTag values = new ListTag();
        for (String rule : rules) values.add(StringTag.valueOf(rule));
        tag.put("Rules", values);
        return tag;
    }

    public void load(CompoundTag tag, int capacity) {
        whitelist = !tag.contains("Whitelist") || tag.getBoolean("Whitelist");
        activeCapacity = capacity;
        rules.clear();
        ListTag list = tag.getList("Rules", Tag.TAG_STRING);
        // Preserve old saved 20-slot Tier V entries, but hide/disable overflow.
        for (int i = 0; i < Math.min(20, list.size()); i++) {
            String rule = list.getString(i);
            try { if (!rule.isBlank()) compile(rule); rules.add(rule); }
            catch (IllegalArgumentException ex) { rules.add(""); }
        }
        rebuild();
    }
}

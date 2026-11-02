package dev.frequencyworks.sequencer;

import dev.frequencyworks.item.TagCardItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Server-owned tablet program stored in the tablet's own item components.
 * Every tablet has nine ordered steps and individually linked sided targets.
 * Only the SequencerBlockEntity stores runtime progress.
 */
public final class SequencerProgram {
    private SequencerProgram() {}
    public static final String KEY = "FrequencyWorksSequencer";
    public static final int STEPS = 9;
    public static final int LINK_LIMIT = 54;

    public record Link(String dimension, BlockPos pos, Direction face, int mode) {
        public boolean permits(boolean receive) {
            return mode == 3 || (receive ? mode == 2 : mode == 1);
        }
        public String title() { return pos.getX()+","+pos.getY()+","+pos.getZ()+" "+face.getName(); }
    }

    public record Step(int link, boolean receive, int amount, boolean blacklist,
                       ItemStack item, String tag, String card, int gate) {
        public boolean configured() { return !item.isEmpty() || !tag.isEmpty() || !card.isEmpty(); }
        public boolean redstone() { return "redstone".equals(card); }
        public boolean interaction() { return "interaction".equals(card); }
    }

    public static CompoundTag program(ItemStack tablet) {
        CustomData data = tablet.get(DataComponents.CUSTOM_DATA);
        if (data == null) return new CompoundTag();
        return data.copyTag().getCompound(KEY).copy();
    }
    public static void update(ItemStack tablet, UnaryOperator<CompoundTag> change) {
        CustomData existing = tablet.get(DataComponents.CUSTOM_DATA);
        CompoundTag custom = existing == null ? new CompoundTag() : existing.copyTag();
        CompoundTag program = custom.getCompound(KEY).copy();
        custom.put(KEY, change.apply(program));
        tablet.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    private static CompoundTag saveLink(Link link) {
        CompoundTag c = new CompoundTag();
        c.putString("Dimension", link.dimension());
        c.putLong("Pos", link.pos().asLong());
        c.putInt("Face", link.face().ordinal());
        c.putInt("Mode", link.mode());
        return c;
    }
    private static Link loadLink(CompoundTag c) {
        int side = c.getInt("Face"), mode = c.getInt("Mode");
        if (side < 0 || side >= Direction.values().length || mode < 1 || mode > 3) return null;
        return new Link(c.getString("Dimension"), BlockPos.of(c.getLong("Pos")), Direction.values()[side], mode);
    }
    public static List<Link> links(ItemStack tablet) {
        ListTag list = program(tablet).getList("Links", Tag.TAG_COMPOUND);
        List<Link> out = new ArrayList<>();
        for (int i=0; i<list.size() && i<LINK_LIMIT; i++) {
            Link link = loadLink(list.getCompound(i));
            if (link != null) out.add(link);
        }
        return List.copyOf(out);
    }
    public static void cycleLink(ItemStack tablet, String dimension, BlockPos pos, Direction face) {
        update(tablet, p -> {
            ListTag list = p.getList("Links", Tag.TAG_COMPOUND);
            int at = -1;
            for (int i=0; i<list.size(); i++) {
                Link l = loadLink(list.getCompound(i));
                if (l!=null && l.dimension().equals(dimension) && l.pos().equals(pos) && l.face()==face) {
                    at = i; break;
                }
            }
            if (at < 0) {
                if (list.size() >= LINK_LIMIT) return p;
                list.add(saveLink(new Link(dimension,pos,face,1)));
            } else {
                Link old=loadLink(list.getCompound(at));
                if (old.mode() < 3) list.set(at, saveLink(new Link(dimension,pos,face,old.mode()+1)));
                else {
                    list.remove(at);
                    ListTag steps=p.getList("Steps",Tag.TAG_COMPOUND);
                    for (int i=0; i<steps.size(); i++) {
                        CompoundTag step=steps.getCompound(i).copy();
                        int index=step.contains("Link")?step.getInt("Link"):-1;
                        if (index==at) step.putInt("Link",-1);
                        else if (index>at) step.putInt("Link",index-1);
                        steps.set(i,step);
                    }
                    p.put("Steps",steps);
                }
            }
            p.put("Links",list);
            return p;
        });
    }

    public static Step step(ItemStack tablet, int index, HolderLookup.Provider registries) {
        if (index<0 || index>=STEPS) return blank();
        ListTag list=program(tablet).getList("Steps",Tag.TAG_COMPOUND);
        if (index>=list.size()) return blank();
        CompoundTag c=list.getCompound(index);
        ItemStack item=c.contains("Item",Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries,c.getCompound("Item")) : ItemStack.EMPTY;
        if (!item.isEmpty()) item=item.copyWithCount(1);
        return new Step(c.contains("Link")?c.getInt("Link"):-1,
                c.getBoolean("Receive"),Math.max(1,Math.min(4096,c.getInt("Amount"))),
                c.getBoolean("Blacklist"),item,c.getString("Tag"),c.getString("Card"),
                c.getInt("Gate"));
    }
    public static Step blank() {
        return new Step(-1,false,1,false,ItemStack.EMPTY,"","",0);
    }

    private static CompoundTag encode(Step step, HolderLookup.Provider registries) {
        CompoundTag c=new CompoundTag();
        c.putInt("Link",step.link());
        c.putBoolean("Receive",step.receive());
        c.putInt("Amount",Math.max(1,Math.min(4096,step.amount())));
        c.putBoolean("Blacklist",step.blacklist());
        if (!step.item().isEmpty()) c.put("Item",step.item().copyWithCount(1).save(registries));
        if (!step.tag().isEmpty()) c.putString("Tag",step.tag());
        if (!step.card().isEmpty()) c.putString("Card",step.card());
        c.putInt("Gate",step.gate());
        return c;
    }
    public static void setStep(ItemStack tablet,int index,Step next,HolderLookup.Provider registries) {
        if (index<0 || index>=STEPS) return;
        update(tablet,p-> {
            ListTag list=p.getList("Steps",Tag.TAG_COMPOUND);
            while (list.size()<=index) list.add(new CompoundTag());
            list.set(index,encode(next,registries));
            p.put("Steps",list);
            return p;
        });
    }
    public static Step withItem(Step before,ItemStack cursor) {
        if (cursor.getItem() instanceof TagCardItem) {
            String tag=TagCardItem.tag(cursor);
            return new Step(before.link(),before.receive(),before.amount(),before.blacklist(),
                    ItemStack.EMPTY,tag,"",before.gate());
        }
        String card=cursor.getItem()==dev.frequencyworks.registration.ModContent.REDSTONE_CARD.get()?"redstone":
                cursor.getItem()==dev.frequencyworks.registration.ModContent.INTERACTION_CARD.get()?"interaction":"";
        return new Step(before.link(),before.receive(),before.amount(),before.blacklist(),
                card.isEmpty()?cursor.copyWithCount(1):ItemStack.EMPTY,"",card,before.gate());
    }
    public static Step clearItem(Step before) {
        return new Step(before.link(),before.receive(),before.amount(),before.blacklist(),
                ItemStack.EMPTY,"","",before.gate());
    }
    /** Cycle valid face/direction pairs. A Both face contributes two choices,
     * one Host-to-target and another target-to-Host. None is a separate choice.
     */
    public static Step nextFaceOption(ItemStack tablet,Step current) {
        List<Link> links=links(tablet);
        List<int[]> options=new ArrayList<>();
        options.add(new int[]{-1,0});
        for(int i=0;i<links.size();i++){
            Link link=links.get(i);
            if(link.permits(false))options.add(new int[]{i,0});
            if(link.permits(true))options.add(new int[]{i,1});
        }
        int index=0;
        for(int i=0;i<options.size();i++){
            int[] option=options.get(i);
            if(option[0]==current.link() && (option[1]==1)==current.receive()){
                index=i;break;
            }
        }
        int[] next=options.get((index+1)%options.size());
        return withReceive(withLink(current,next[0]),next[1]==1);
    }
    public static Step withLink(Step s,int link) {
        return new Step(link,s.receive(),s.amount(),s.blacklist(),s.item(),s.tag(),s.card(),s.gate());
    }
    public static Step withReceive(Step s,boolean receive) {
        return new Step(s.link(),receive,s.amount(),s.blacklist(),s.item(),s.tag(),s.card(),s.gate());
    }
    public static Step withAmount(Step s,int amount) {
        return new Step(s.link(),s.receive(),amount,s.blacklist(),s.item(),s.tag(),s.card(),s.gate());
    }
    public static Step withBlacklist(Step s,boolean value) {
        return new Step(s.link(),s.receive(),s.amount(),value,s.item(),s.tag(),s.card(),s.gate());
    }
    public static Step withGate(Step s,int gate) {
        return new Step(s.link(),s.receive(),s.amount(),s.blacklist(),s.item(),s.tag(),s.card(),gate);
    }
    public static int failurePolicy(ItemStack tablet) {
        return Math.floorMod(program(tablet).getInt("OnFailure"),3);
    }
    public static void cycleFailurePolicy(ItemStack tablet) {
        update(tablet,p->{p.putInt("OnFailure",(Math.floorMod(p.getInt("OnFailure"),3)+1)%3);return p;});
    }
}
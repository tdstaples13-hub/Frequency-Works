package dev.frequencyworks.client;

import dev.frequencyworks.network.ConfigNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** First functional, text-based GUI. Item icons/ghost drag-and-drop can replace
 * the text filter editor in a later visual milestone without changing server rules.
 */
public final class ConfigScreen extends Screen {
    private final ConfigNetworking.OpenConfig packet;
    private final CompoundTag data;
    private int page;
    private boolean incoming = true;
    private int selectedSlot;
    private int depositSlot;
    private boolean privateScope = true;
    private EditBox frequencyBox;
    private EditBox ruleBox;
    private int left;
    private int top;
    private static final int WIDTH = 332;
    private int panelHeight;
    private int bufferRows() { return Math.max(1, Math.min(8, (panelHeight - 160) / 18)); }

    public ConfigScreen(ConfigNetworking.OpenConfig packet) { this(packet,0,true,0,0); }
    public ConfigScreen(ConfigNetworking.OpenConfig packet, int page, boolean incoming, int selectedSlot, int depositSlot) {
        super(Component.literal(packet.kind().equals("buffer") ? "Transfer Buffer" : packet.kind().equals("port") ? "Transfer Port" : "Tag Card"));
        this.packet = packet;
        this.data = packet.state() == null ? new CompoundTag() : packet.state();
        this.page = page;
        this.incoming = incoming;
        this.selectedSlot = selectedSlot;
        this.depositSlot = depositSlot;
        this.privateScope = data.getBoolean("Assigned") ? data.getBoolean("Private")
                : !data.getString("Pending").endsWith(":public");
    }
    public boolean sameTarget(ConfigNetworking.OpenConfig other) {
        return packet.kind().equals(other.kind()) && packet.pos().equals(other.pos()) && packet.hand() == other.hand();
    }
    public int page() { return page; }
    public boolean bank() { return incoming; }
    public int selectedSlot() { return selectedSlot; }
    public int depositSlot() { return depositSlot; }

    private void send(String action, String value, int slot) {
        PacketDistributor.sendToServer(new ConfigNetworking.Action(packet.pos(), packet.hand(), action, value, slot));
    }
    private Button button(int x,int y,int w,String text,Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(x,y,w,20).build());
    }
    private EditBox field(int x,int y,int w,int max,String hint,String initial) {
        EditBox box = new EditBox(font,x,y,w,18,Component.literal(hint));
        box.setMaxLength(max);
        box.setValue(initial);
        addRenderableWidget(box);
        return box;
    }
    @Override protected void init() {
        left = (width-WIDTH)/2;
        panelHeight = Math.min(310, Math.max(230, height-12));
        top = (height-panelHeight)/2;
        if (packet.kind().equals("tag")) {
            ruleBox = field(left+12, top+44, 216, 128, "Item tag", data.getString("Tag"));
            button(left+236, top+43, 82, "Save Tag", () -> send("tag_set", ruleBox.getValue(), 0));
        } else if (!data.getBoolean("Assigned")) {
            frequencyBox = field(left+12,top+41,78,3,"Frequency",
                data.getString("Pending").length()>=3 ? data.getString("Pending").substring(0,3) : "");
            button(left+100,top+40,100,privateScope?"Private":"Public", () -> {
                privateScope=!privateScope;
                rebuildWidgets();
            });
            if (packet.kind().equals("buffer")) {
                button(left+212,top+40,106,"Assign", () -> send("bind",selectedFrequency(),0));
                if (data.getBoolean("Confirm")) button(left+12,top+76,155,"Confirm sharing",() -> send("confirm",selectedFrequency(),0));
            } else {
                button(left+212,top+40,106,"Connect", () -> send("bind",selectedFrequency(),0));
            }
        } else if (packet.kind().equals("buffer")) {
            initBuffer();
        } else {
            initPort();
        }
        button(left+WIDTH-62,top+panelHeight-25,50,"Done", this::onClose);
    }
    private String selectedFrequency() {
        return (frequencyBox == null ? "" : frequencyBox.getValue()) + ":" + (privateScope?"private":"public");
    }
    protected void rebuildWidgets() {
        String previous = frequencyBox == null?"":frequencyBox.getValue();
        clearWidgets();
        init();
        if (frequencyBox != null) frequencyBox.setValue(previous);
    }
    private void initBuffer() {
        if (!data.getString("Error").isEmpty()) return;
        String[] sorts = {"QUANTITY_ASC", "QUANTITY_DESC", "ALPHABETICAL", "MOD_ALPHABETICAL"};
        String mode = data.getString("Sort");
        button(left+12,top+53,306,"Sorting: " + readableSort(mode), () -> {
            int next = 0;
            for (int i=0;i<sorts.length;i++) if (sorts[i].equals(mode)) next=(i+1)%sorts.length;
            send("sort", sorts[next], 0);
        });
        ListTag items = data.getList("Items", Tag.TAG_COMPOUND);
        int rows = bufferRows();
        int pages = Math.max(1, (items.size()+rows-1)/rows);
        page = Math.max(0,Math.min(page,pages-1));
        button(left+234,top+78,33,"<",() -> {page=Math.max(0,page-1); rebuildWidgets();});
        button(left+271,top+78,33,">",() -> {page=Math.min(pages-1,page+1); rebuildWidgets();});
        // Each row has server-validated withdrawal buttons. A permanent entry ID
        // avoids withdrawing a different item if sorting changes between packets.
        for (int row=0; row<rows; row++) {
            int index = page*rows+row;
            if (index >= items.size()) break;
            long entryId = items.getCompound(index).getLong("EntryId");
            int y=top+102+row*18;
            button(left+233,y,37,"1",() -> send("withdraw",Long.toString(entryId),1));
            button(left+273,y,45,"64",() -> send("withdraw",Long.toString(entryId),64));
        }
        // Select which player inventory slot is the source for deposits.
        button(left+12,top+panelHeight-52,26,"<",() -> {depositSlot=(depositSlot+35)%36; rebuildWidgets();});
        button(left+40,top+panelHeight-52,26,">",() -> {depositSlot=(depositSlot+1)%36; rebuildWidgets();});
        button(left+177,top+panelHeight-52,62,"Store 1",() -> send("deposit","one",depositSlot));
        button(left+244,top+panelHeight-52,74,"Store Stack",() -> send("deposit","stack",depositSlot));
    }
    private static String readableSort(String raw) {
        return switch(raw) {
            case "QUANTITY_ASC" -> "Quantity: Smallest first";
            case "QUANTITY_DESC" -> "Quantity: Largest first";
            case "ALPHABETICAL" -> "Alphabetical";
            case "MOD_ALPHABETICAL" -> "By Mod: Alphabetical";
            default -> raw;
        };
    }
    private void initPort() {
        if (!data.getBoolean("CanConfigure")) return;
        button(left+12,top+44,147,data.getBoolean("Stack")?"Full-stack mode":"Individual mode", () -> send("stack",Boolean.toString(!data.getBoolean("Stack")),0));
        int[] modes = data.getIntArray("Faces");
        String[] names = {"Down","Up","North","South","West","East"};
        String[] values = {"NONE","INPUT","OUTPUT","BOTH"};
        for (int i=0;i<6;i++) {
            final int face = i;
            int index = i < modes.length ? modes[i]:0;
            button(left+12+(i%3)*104,top+74+(i/3)*23,100,names[i]+": "+values[index],
                () -> send("face",values[(index+1)%4],face));
        }
        button(left+12,top+126,144,incoming?"Bank: INPUT":"Bank: OUTPUT",() -> {incoming=!incoming; selectedSlot=0; rebuildWidgets();});
        CompoundTag filter = data.getCompound(incoming?"InputFilter":"OutputFilter");
        boolean whitelist = !filter.contains("Whitelist") || filter.getBoolean("Whitelist");
        button(left+162,top+126,156,whitelist?"Whitelist":"Blacklist", () -> send("filter_mode",(incoming?"input":"output")+":"+(whitelist?"black":"white"),0));
        ListTag rules = filter.getList("Rules",Tag.TAG_STRING);
        int capacity = data.getInt("Slots");
        int base = Math.min((selectedSlot/5)*5,Math.max(0,capacity-1));
        for (int i=0;i<5 && base+i<capacity;i++) {
            int slot = base+i;
            String rule = slot<rules.size()?rules.getString(slot):"";
            button(left+12+i*62,top+151,60,(slot+1)+": "+(rule.isEmpty()?"---":"Set"),() -> {selectedSlot=slot;rebuildWidgets();});
        }
        String initial = selectedSlot < rules.size() ? rules.getString(selectedSlot) : "";
        ruleBox = field(left+12,top+176,211,128,"Item ID or #tag", initial);
        button(left+229,top+175,44,"Set", () -> send("rule",(incoming?"input":"output")+"|"+ruleBox.getValue(),selectedSlot));
        button(left+277,top+175,41,"Clear", () -> send("rule",(incoming?"input":"output")+"|",selectedSlot));
        button(left+12,top+200,184,"Use held Tag Card", () -> send("apply_card", incoming?"input":"output",selectedSlot));
        if (capacity>5) {
            button(left+243,top+200,35,"<", () -> {selectedSlot=Math.max(0,selectedSlot-5);rebuildWidgets();});
            button(left+282,top+200,35,">", () -> {selectedSlot=Math.min(capacity-1,selectedSlot+5);rebuildWidgets();});
        }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g,mx,my,partial);
        g.fill(left,top,left+WIDTH,top+panelHeight,0xEF1F2430);
        g.drawCenteredString(font,title,left+WIDTH/2,top+10,0xFFFFFF);
        if (!data.getString("Warning").isEmpty()) {
            int line = data.getBoolean("Assigned") ? top+panelHeight-10 : top+140;
            g.drawString(font,shorten(data.getString("Warning"),50),left+12,line,0xFFCC66);
        }
        if (!data.getString("Error").isEmpty()) g.drawString(font,shorten(data.getString("Error"),55),left+12,top+75,0xFF8888);
        if (packet.kind().equals("tag")) {
            g.drawString(font,"Enter a tag, e.g. #minecraft:logs",left+12,top+26,0xCCCCCC);
            g.drawString(font,"Use this card in a Port filter (hold it in your offhand).",left+12,top+88,0xAABBDD);
        } else if (!data.getBoolean("Assigned")) {
            g.drawString(font,packet.kind().equals("buffer")?"Choose a permanent 3-digit frequency":"Connect to an existing frequency",left+12,top+26,0xCCCCCC);
            g.drawString(font,"Public shares items; Private belongs to your UUID",left+12,top+112,0xAABBDD);
        } else {
            g.drawString(font,"Frequency " + data.getString("Frequency") + (data.getBoolean("Private")?" (Private)":" (Public)"),left+12,top+29,0xFFFFFF);
            if (packet.kind().equals("buffer") && data.getString("Error").isEmpty()) {
                g.drawString(font,"Tier " + data.getInt("EffectiveTier") + " | " + data.getInt("Occupied") + "/" + data.getInt("Slots") + " types | " + data.getLong("TotalItems") + " items",left+12,top+43,0xCCCCCC);
                ListTag items = data.getList("Items",Tag.TAG_COMPOUND);
                int rows = bufferRows();
                int start = page*rows;
                g.drawString(font,"Page " + (page+1) + " / " + Math.max(1,(items.size()+rows-1)/rows) + " — withdraw below",left+12,top+84,0xAABBCC);
                for (int i=0; i<rows && start+i<items.size(); i++) {
                    var item = items.getCompound(start+i);
                    g.drawString(font,shorten(item.getString("Name"),27),left+12,top+107+i*18,0xEEEEEE);
                    g.drawString(font,Long.toString(item.getLong("Count")),left+163,top+107+i*18,0xAADDAA);
                }
                ListTag own = data.getList("PlayerItems",Tag.TAG_COMPOUND);
                if (depositSlot >= 0 && depositSlot < own.size()) {
                    CompoundTag held = own.getCompound(depositSlot);
                    g.drawString(font,"Slot "+depositSlot+": "+shorten(held.getString("Name"),17)+" x"+held.getInt("Count"),left+74,top+panelHeight-68,0xDDDDDD);
                }
                g.drawString(font,"Choose a player inventory slot; then store 1 or a stack.",left+12,top+panelHeight-29,0x99AABB);
            } else if (packet.kind().equals("port")) {
                g.drawString(font,"Input: machine to frequency | Output: frequency to machine",left+12,top+63,0xCCDDCC);
                g.drawString(font,"Filter slot "+(selectedSlot+1)+" — item ID or #tag",left+12,top+223,0xDDDDDD);
            }
        }
        super.render(g,mx,my,partial);
    }
    private static String shorten(String value,int limit) {return value.length()>limit?value.substring(0,limit-3)+"...":value;}
    @Override public boolean isPauseScreen() { return false; }
}

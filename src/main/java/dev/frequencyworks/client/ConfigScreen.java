package dev.frequencyworks.client;

import dev.frequencyworks.network.ConfigNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Configuration screen for initial frequency binding, Tag Cards, and debug.
 * Configured Ports use a separate, compact Pipez-style tabbed controls panel.
 * Minecraft's own rendering layer handles the widgets; the backdrop is drawn
 * before the widgets instead of accidentally blurring the UI in super.render().
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
        super(Component.literal(packet.kind().equals("buffer") ? "Transfer Buffer" : packet.kind().equals("port") ? "Transfer Port" : packet.kind().equals("debug") ? "Frequency Debugger" : "Tag Card"));
        this.packet = packet;
        this.data = packet.state() == null ? new CompoundTag() : packet.state();
        this.page = page;
        this.incoming = incoming;
        this.selectedSlot = selectedSlot;
        this.depositSlot = depositSlot;
        this.privateScope = packet.kind().equals("debug")
                ? !data.getString("Pending").endsWith(":public")
                : (data.getBoolean("Assigned") ? data.getBoolean("Private")
                : !data.getString("Pending").endsWith(":public"));
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
        panelHeight = packet.kind().equals("port") && data.getBoolean("Assigned")
            ? Math.min(260, Math.max(232, height - 12))
            : Math.min(310, Math.max(230, height-12));
        top = (height-panelHeight)/2;
        if (packet.kind().equals("debug")) {
            frequencyBox = field(left+12,top+41,78,3,"Frequency",
                data.getString("Pending").length()>=3 ? data.getString("Pending").substring(0,3) : "");
            button(left+100,top+40,100,privateScope?"Private":"Public", () -> {
                privateScope=!privateScope; rebuildWidgets();
            });
            button(left+212,top+40,106,"Delete...",() -> send("debug_delete",selectedFrequency(),0));
            if(data.getBoolean("Confirm"))
                button(left+12,top+100,225,"CONFIRM PERMANENT DELETION",() -> send("debug_confirm",selectedFrequency(),0));
        } else if (packet.kind().equals("tag")) {
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
        } else if (packet.kind().equals("port")) {
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
    private static String readableSort(String raw) {
        return switch(raw) {
            case "QUANTITY_ASC" -> "Quantity: Smallest first";
            case "QUANTITY_DESC" -> "Quantity: Largest first";
            case "ALPHABETICAL" -> "Alphabetical";
            case "MOD_ALPHABETICAL" -> "By Mod: Alphabetical";
            default -> raw;
        };
    }
    private static final String[] FACE_NAMES = {"Down", "Up", "North", "South", "West", "East"};
    private static final String[] FACE_STATES = {"Off", "Import", "Export", "Both"};
    private static final String[] FACE_VALUES = {"NONE", "INPUT", "OUTPUT", "BOTH"};
    private static final ChatFormatting[] FACE_COLORS = {
        ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.GOLD, ChatFormatting.LIGHT_PURPLE
    };

    private void switchPortTab(int tab) {
        page = tab;
        incoming = tab != 2;
        selectedSlot = 0;
        rebuildWidgets();
    }

    private void initPort() {
        // Tab navigation is local client state, not a server configuration change.
        for (int tab=0; tab<3; tab++) {
            final int destination = tab;
            String[] names={"Routing", "Import filter", "Export filter"};
            addRenderableWidget(Button.builder(
                Component.literal((page==tab?"[":"")+names[tab]+(page==tab?"]":"")),
                b -> switchPortTab(destination))
                .bounds(left+12+tab*103, top+48, 100, 20).build());
        }
        if (!data.getBoolean("CanConfigure")) return;
        if (page == 0) {
            int[] modes = data.getIntArray("Faces");
            for (int i=0; i<6; i++) {
                int face = i;
                int index = i<modes.length && modes[i]>=0 && modes[i]<FACE_STATES.length?modes[i]:0;
                Component caption = Component.literal(FACE_NAMES[i]+": "+FACE_STATES[index])
                    .withStyle(FACE_COLORS[index]);
                addRenderableWidget(Button.builder(caption,
                    b -> send("face", FACE_VALUES[(index+1)%FACE_VALUES.length], face))
                    .bounds(left+12+(i%3)*103, top+87+(i/3)*24, 100,20).build());
            }
            button(left+12,top+146,210,
                data.getBoolean("Stack")?"Transfer rate: Full stacks":"Transfer rate: Individual items",
                () -> send("stack",Boolean.toString(!data.getBoolean("Stack")),0));
        } else {
            incoming = page==1;
            CompoundTag bank = data.getCompound(incoming?"InputFilter":"OutputFilter");
            boolean whitelist = !bank.contains("Whitelist") || bank.getBoolean("Whitelist");
            button(left+12,top+78,154,whitelist?"Whitelist (allow)":"Blacklist (deny)",
                () -> send("filter_mode",(incoming?"input":"output")+":"+(whitelist?"black":"white"),0));
            ListTag rules = bank.getList("Rules",Tag.TAG_STRING);
            int capacity = Math.max(1, data.getInt("Slots"));
            selectedSlot = Math.max(0,Math.min(selectedSlot,capacity-1));
            int base = (selectedSlot / 5) * 5;
            if (capacity>5) {
                button(left+216,top+78,32,"<",()->{selectedSlot=Math.max(0,selectedSlot-5);rebuildWidgets();});
                button(left+252,top+78,32,">",()->{selectedSlot=Math.min(capacity-1,selectedSlot+5);rebuildWidgets();});
            }
            for (int i=0; i<5 && base+i<capacity; i++) {
                final int slot = base+i;
                String rule=slot<rules.size()?rules.getString(slot):"";
                String label = (selectedSlot==slot?"[":"")+(slot+1)+(selectedSlot==slot?"]":"");
                button(left+12+i*62,top+110,58,label,()->{selectedSlot=slot;rebuildWidgets();});
            }
            String initial= selectedSlot<rules.size()?rules.getString(selectedSlot):"";
            ruleBox=field(left+12,top+141,206,128,"Item ID or #tag",initial);
            button(left+224,top+140,46,"Set",()->send("rule",(incoming?"input":"output")+"|"+ruleBox.getValue(),selectedSlot));
            button(left+274,top+140,44,"Clear",()->send("rule",(incoming?"input":"output")+"|",selectedSlot));
            button(left+12,top+172,198,"Use held Tag Card",()->send("apply_card",incoming?"input":"output",selectedSlot));
        }
    }

    /** Minecraft's default blurred world background is not a foreground overlay. */
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // We draw our own opaque readable panel in render() before the controls.
    }

    private void drawPortPreviews(GuiGraphics g, int mx, int my) {
        if (!packet.kind().equals("port") || page == 0 || !data.getBoolean("Assigned")) return;
        CompoundTag bank=data.getCompound(page==1?"InputFilter":"OutputFilter");
        ListTag rules=bank.getList("Rules",Tag.TAG_STRING);
        int cap=data.getInt("Slots");
        int base=(selectedSlot/5)*5;
        for(int i=0;i<5 && base+i<cap;i++) {
            int slot=base+i;
            String rule=slot<rules.size()?rules.getString(slot):"";
            int x=left+12+i*62, y=top+110;
            if (rule.startsWith("#")) {
                g.drawString(font, "#", x+40,y+6,0x64C7ED,true);
            } else if (!rule.isBlank()) {
                ResourceLocation itemId=ResourceLocation.tryParse(rule);
                if(itemId!=null && BuiltInRegistries.ITEM.containsKey(itemId))
                    g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(itemId)),x+38,y+2);
            }
            if (!rule.isBlank() && mx>=x && mx<x+58 && my>=y && my<y+20)
                g.renderTooltip(font,Component.literal(rule),mx,my);
        }
    }

    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        // Render the opaque panel FIRST, then Minecraft's ordinary buttons and
        // text fields. Never blur them by drawing the default screen background last.
        g.fill(0,0,width,height,0x93000000);
        g.fill(left,top,left+WIDTH,top+panelHeight,0xFF1C2430);
        g.fill(left+3,top+3,left+WIDTH-3,top+panelHeight-3,0xFF303B49);
        g.drawCenteredString(font,title,left+WIDTH/2,top+9,0xFFFFFF);
        if (!data.getString("Error").isEmpty()) g.drawString(font,shorten(data.getString("Error"),55),left+12,top+76,0xFF8888);
        if (packet.kind().equals("debug")) {
            g.drawString(font,"CREATIVE OP DEBUGGER: permanently clears inventory",left+12,top+26,0xFF9999);
            g.drawString(font,"Deleted bindings reset when old Buffers are next accessed.",left+12,top+77,0xE3D6B0);
            if(data.getBoolean("Confirm")) g.drawString(font,"Irreversible. All items in this frequency will be lost.",left+12,top+91,0xFFAAAA);
        } else if (packet.kind().equals("tag")) {
            g.drawString(font,"Enter a tag, e.g. #minecraft:logs",left+12,top+26,0xCCCCCC);
            g.drawString(font,"Use this card in a Port filter (hold it in your offhand).",left+12,top+88,0xAABBDD);
        } else if (!data.getBoolean("Assigned")) {
            g.drawString(font,packet.kind().equals("buffer")?"Choose a permanent 3-digit frequency":"Connect to an existing frequency",left+12,top+26,0xCCCCCC);
            g.drawString(font,"Public shares items; Private belongs to your UUID",left+12,top+112,0xAABBDD);
        } else if (packet.kind().equals("port")) {
            g.drawString(font,"Frequency "+data.getString("Frequency")+(data.getBoolean("Private")?" (Private)":" (Public)"),left+12,top+29,0xE9EEF4);
            if (!data.getBoolean("CanConfigure")) {
                g.drawString(font,"Only the Port's owner may configure it.",left+12,top+85,0xFFCC88);
            } else if (page==0) {
                g.drawString(font,"Configure each side of the connected inventory:",left+12,top+73,0xDFE5ED);
                g.drawString(font,"IMPORT: inventory -> frequency",left+12,top+180,0x85E9A5);
                g.drawString(font,"EXPORT: frequency -> inventory",left+12,top+192,0xFFD185);
                g.drawString(font,"BOTH: import wins; shared items never loop back",left+12,top+204,0xD9B4FF);
            } else {
                g.drawString(font,(page==1?"Import":"Export")+" filter - slot "+(selectedSlot+1)+" / "+data.getInt("Slots"),left+12,top+99,0xDFE5ED);
                g.drawString(font,"Click a numbered filter slot; enter item ID or #tag.",left+12,top+197,0xCCD7E4);
                g.drawString(font,"Empty filters allow all items.",left+12,top+209,0xB8C2CA);
            }
        }
        if (!data.getString("Warning").isEmpty()) {
            int line=packet.kind().equals("port")?top+panelHeight-38:packet.kind().equals("debug")?top+153:data.getBoolean("Assigned")?top+panelHeight-36:top+140;
            g.drawString(font,shorten(data.getString("Warning"),50),left+12,line,0xFFCC66);
        }
        super.render(g,mx,my,partial);
        drawPortPreviews(g,mx,my);
    }
    private static String shorten(String value,int limit) {return value.length()>limit?value.substring(0,limit-3)+"...":value;}
    @Override public boolean isPauseScreen() { return false; }
}

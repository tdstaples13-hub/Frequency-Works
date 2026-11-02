package dev.frequencyworks.client;

import dev.frequencyworks.menu.TabletMenu;
import dev.frequencyworks.network.SequencerNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Compact Sequence Tablet editor.
 *
 * Top:    nine independent face/direction cycle controls.
 * Middle: nine ghost matcher/card instruction slots (not inventories).
 * Bottom: nine independent whitelist/blacklist toggles.
 *
 * Square colors in the original design diagram only identified control kinds.
 * Use restrained vanilla-style gray widgets, not those literal diagram colors.
 */
public final class TabletScreen extends AbstractContainerScreen<TabletMenu> {
    private static final ResourceLocation CHEST =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int FIRST_X=8, SPACING=18;
    private static final int OPTION_Y=24, GHOST_Y=46, FILTER_Y=68;
    private static final int PANEL_Y=112;
    private int selected=0;

    public TabletScreen(TabletMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        imageWidth=176;
        imageHeight=PANEL_Y+96;
        titleLabelY=6;
        inventoryLabelY=PANEL_Y+3;
    }

    /** The screen refreshes its small footer only when a server update arrives. */
    public void refreshState(){rebuildWidgets();}

    private void send(String action,int step,int value) {
        PacketDistributor.sendToServer(
                new SequencerNetworking.TabletAction(menu.hand(),action,step,value));
    }
    private ListTag steps(){return menu.state().getList("Steps",Tag.TAG_COMPOUND);}
    private CompoundTag step(int slot){
        ListTag list=steps();
        return slot<list.size()?list.getCompound(slot):new CompoundTag();
    }
    private static int amount(CompoundTag s){
        return Math.max(1,s.getInt("Amount"));
    }
    private static boolean ordinaryItem(CompoundTag step){
        // A configured Tag Card stores a Tag, and control cards store Card,
        // never Item. Only actual item matching has quantity arrows.
        return step.contains("Item",Tag.TAG_COMPOUND);
    }
    private static boolean interaction(CompoundTag step){
        return "interaction".equals(step.getString("Card"));
    }
    private static String interactionAction(CompoundTag step){
        return switch(Math.floorMod(step.getInt("Gate"),4)){
            case 0->"Right-click";
            case 1->"Sneak-right-click";
            case 2->"Punch";
            default->"Mine";
        };
    }
    private static String actionSymbol(CompoundTag step){
        return switch(Math.floorMod(step.getInt("Gate"),4)){
            case 0->"R";
            case 1->"S";
            case 2->"P";
            default->"M";
        };
    }
    private static boolean occupied(CompoundTag s){
        return ordinaryItem(s)||!s.getString("Tag").isEmpty()||!s.getString("Card").isEmpty();
    }
    private String faceText(CompoundTag s){
        int index=s.contains("Link")?s.getInt("Link"):-1;
        ListTag links=menu.state().getList("Links",Tag.TAG_COMPOUND);
        if(index<0||index>=links.size())return "No linked face; click to cycle";
        CompoundTag link=links.getCompound(index);
        var pos=net.minecraft.core.BlockPos.of(link.getLong("Pos"));
        int ord=Math.floorMod(link.getInt("Face"),6);
        var direction=net.minecraft.core.Direction.values()[ord];
        return (s.getBoolean("Receive")?"Receive: target to Host":"Send: Host to target")
            +" | "+link.getString("Dimension")+" "+pos.getX()+","+pos.getY()+","+pos.getZ()
            +" "+direction.getName();
    }

    private String interactionTargetText(CompoundTag s){
        int index=s.contains("Link")?s.getInt("Link"):-1;
        ListTag links=menu.state().getList("Links",Tag.TAG_COMPOUND);
        if(index<0||index>=links.size())return "Target: none; click to cycle linked faces";
        CompoundTag link=links.getCompound(index);
        var pos=net.minecraft.core.BlockPos.of(link.getLong("Pos"));
        var face=net.minecraft.core.Direction.values()[Math.floorMod(link.getInt("Face"),6)];
        return "Target: "+link.getString("Dimension")+" "+
                pos.getX()+","+pos.getY()+","+pos.getZ()+" "+face.getName();
    }

    private Button footer(int x,int w,String label,String tip,Runnable action){
        Button btn=addRenderableWidget(Button.builder(Component.literal(label),b->action.run())
                .bounds(leftPos+x,topPos+92,w,18).build());
        btn.setTooltip(Tooltip.create(Component.literal(tip)));
        return btn;
    }
    private static String gateLabel(int mode){
        return switch(mode){
            case 0->"Powered";
            case 1->"Unpowered";
            case 2->"Rising";
            case 3->"Falling";
            default->"Signal >= "+(mode-3);
        };
    }
    @Override protected void init(){
        super.init();
        int failure=Math.floorMod(menu.state().getInt("OnFailure"),3);
        String[] modes={"Continue","Skip","Stop"};
        CompoundTag current=step(selected);
        boolean redstone="redstone".equals(current.getString("Card"));
        int width=redstone?81:160;
        footer(8,width,"Failure: "+modes[failure],
                "On failure: Continue retries, Skip only before partial transfers, Stop requires manual restart",
                ()->send("failure",selected,0));
        if(redstone){
            int gate=Math.floorMod(current.getInt("Gate"),19);
            footer(92,76,"RS: "+(gate>=4?">="+(gate-3):gateLabel(gate)),
                    "Step "+(selected+1)+" Redstone gate: "+gateLabel(gate)+" (click to cycle)",
                    ()->send("gate",selected,0));
        }
    }

    /** The A5G bug was an undrawn panel. Always paint it before the normal
     * menu/widget rendering. Never rely on the older renderBg-only approach.
     */
    private void drawPanel(GuiGraphics g){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF767676);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+PANEL_Y-2,0xFFC6C6C6);
        // The player has actual vanilla slots, not ghost copies.
        g.blit(CHEST,leftPos,topPos+PANEL_Y,0,126,176,96);
        for(int i=0;i<9;i++){
            int x=leftPos+FIRST_X+i*SPACING;
            int y=topPos+OPTION_Y;
            // All three rows share Minecraft's neutral inventory palette.
            // The first and third rows are controls; the middle row is a ghost
            // slot, with a light selection rim rather than a colored fill.
            square(g,x,y,0xFF454545,0xFFB6B6B6);
            y=topPos+GHOST_Y;
            square(g,x,y,i==selected?0xFFE5E5E5:0xFF454545,0xFF888888);
            y=topPos+FILTER_Y;
            square(g,x,y,0xFF454545,0xFFB6B6B6);
        }
    }
    private static void square(GuiGraphics g,int x,int y,int rim,int interior){
        g.fill(x,y,x+18,y+18,rim);
        g.fill(x+1,y+1,x+17,y+17,interior);
    }
    // No default blurred backdrop. Always draw a darkened world first, then
    // a fully opaque panel and the genuine vanilla player inventory texture.
    @Override public void renderBackground(GuiGraphics graphics,int mx,int my,float partial){
        graphics.fill(0,0,width,height,0x88000000);
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mx,int my){
        // Deliberately empty. drawPanel() runs before super.render().
    }

    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,6,0x353535,false);
        g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0x404040,false);
    }

    private void drawItem(GuiGraphics g,CompoundTag s,int x,int y){
        if(ordinaryItem(s) && minecraft!=null && minecraft.level!=null){
            ItemStack item=ItemStack.parseOptional(minecraft.level.registryAccess(),s.getCompound("Item"));
            if(!item.isEmpty())g.renderItem(item,x+1,y+1);
            // Short, vertical quantity arrows occupy the right wall only for
            // ordinary item ghosts. Cards and #tags have no arrows whatsoever.
            g.fill(x+13,y+1,x+17,y+17,0xBA252525);
            boolean bold=hasShiftDown();
            g.drawString(font,"^",x+13,y+0,bold?0xFFFFFF:0xE8E8E8,true);
            g.drawString(font,"v",x+13,y+8,bold?0xFFFFFF:0xE8E8E8,true);
            if(bold){
                g.drawString(font,"^",x+14,y+0,0xFFFFFF,true);
                g.drawString(font,"v",x+14,y+8,0xFFFFFF,true);
            }
            int qty=amount(s);
            if(qty>1){
                g.pose().pushPose();
                g.pose().translate(x+1.0F,y+11.0F,200.0F);
                g.pose().scale(0.53F,0.53F,1.0F);
                g.drawString(font,Integer.toString(qty),0,0,0xFFFFFF,true);
                g.pose().popPose();
            }
        }else if(!s.getString("Tag").isEmpty()){
            g.drawCenteredString(font,"#",x+9,y+5,0xD2F0FF);
        }else if("redstone".equals(s.getString("Card"))){
            g.renderItem(new ItemStack(dev.frequencyworks.registration.ModContent.REDSTONE_CARD.get()),x+1,y+1);
        }else if("interaction".equals(s.getString("Card"))){
            g.renderItem(new ItemStack(dev.frequencyworks.registration.ModContent.INTERACTION_CARD.get()),x+1,y+1);
        }
    }

    private String ghostTooltip(int i,CompoundTag s){
        String kind=ordinaryItem(s)?"Item matcher":
                !s.getString("Tag").isEmpty()?"Tag #"+s.getString("Tag"):
                !s.getString("Card").isEmpty()?s.getString("Card")+" Card":"Empty";
        return "Step "+(i+1)+" | "+kind+
                (ordinaryItem(s)?" | Quantity "+amount(s)+" | arrows +/-"+
                        (hasShiftDown()?"10":"1"):"")+
                " | Left click to "+(occupied(s)?"remove":"assign item/card")+
                "; right click to select";
    }

    private void drawRowContent(GuiGraphics g){
        ListTag links=menu.state().getList("Links",Tag.TAG_COMPOUND);
        for(int i=0;i<9;i++){
            CompoundTag s=step(i);
            int x=leftPos+FIRST_X+i*SPACING;
            int index=s.contains("Link")?s.getInt("Link"):-1;
            String icon=interaction(s)?actionSymbol(s):
                    index>=0 && index<links.size()
                        ?s.getBoolean("Receive")?"<":">":"-";
            g.drawCenteredString(font,icon,x+9,topPos+OPTION_Y+5,0xFF303030);
            drawItem(g,s,x,topPos+GHOST_Y);
            String lower=interaction(s)?
                    index>=0 && index<links.size()?
                        net.minecraft.core.Direction.values()[
                            Math.floorMod(links.getCompound(index).getInt("Face"),6)
                        ].getName().substring(0,1).toUpperCase(java.util.Locale.ROOT):"-":
                    s.getBoolean("Blacklist")?"B":"W";
            g.drawCenteredString(font,lower,
                    x+9,topPos+FILTER_Y+5,0xFF303030);
        }
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0||button==1){
            double relative=mx-leftPos-FIRST_X;
            if(relative>=0&&relative<9*SPACING){
                int column=(int)(relative/SPACING);
                int cellX=leftPos+FIRST_X+column*SPACING;
                if(mx<cellX+18){
                    double y=my-topPos;
                    if(y>=OPTION_Y && y<OPTION_Y+18){
                        selected=column;
                        if(button==0)send("option",column,0);
                        rebuildWidgets();
                        return true;
                    }
                    if(y>=GHOST_Y && y<GHOST_Y+18){
                        selected=column;
                        CompoundTag s=step(column);
                        if(button==0){
                            if(ordinaryItem(s) && mx>=cellX+13){
                                int delta=y-GHOST_Y<9?1:-1;
                                send("amount",column,delta*(hasShiftDown()?10:1));
                            }else if(occupied(s)){
                                send("clear",column,0);
                            }else if(!menu.getCarried().isEmpty()){
                                send("set",column,0);
                            }
                        }
                        // Right-click only selects; it never destroys a filter.
                        rebuildWidgets();
                        return true;
                    }
                    if(y>=FILTER_Y && y<FILTER_Y+18){
                        selected=column;
                        if(button==0)send(interaction(step(column))?"interaction_target":"blacklist",column,0);
                        rebuildWidgets();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx,my,button);
    }

    private void hoveredDescription(GuiGraphics g,int mx,int my){
        double rx=mx-leftPos-FIRST_X;
        if(rx<0||rx>=9*SPACING)return;
        int col=(int)(rx/SPACING);
        if(mx>=leftPos+FIRST_X+col*SPACING+18)return;
        double y=my-topPos;
        CompoundTag s=step(col);
        String description;
        if(y>=OPTION_Y && y<OPTION_Y+18){
            description=interaction(s)?
                    "Step "+(col+1)+" action: "+interactionAction(s)+" | click to cycle":
                    "Step "+(col+1)+": "+faceText(s)+" | click to cycle";
        }else if(y>=GHOST_Y && y<GHOST_Y+18){
            description=ghostTooltip(col,s);
        }else if(y>=FILTER_Y && y<FILTER_Y+18){
            description=interaction(s)?
                    "Step "+(col+1)+": "+interactionTargetText(s):
                    "Step "+(col+1)+": "+(s.getBoolean("Blacklist")?"Blacklist: exclude matching items":"Whitelist: require matching items");
        }else return;
        g.renderTooltip(font,Component.literal(description),mx,my);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);
        drawPanel(g);
        super.render(g,mx,my,partial);
        drawRowContent(g);
        renderTooltip(g,mx,my);
        hoveredDescription(g,mx,my);
    }
}

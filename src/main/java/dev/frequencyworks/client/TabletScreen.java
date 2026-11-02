package dev.frequencyworks.client;

import dev.frequencyworks.menu.TabletMenu;
import dev.frequencyworks.network.SequencerNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Nine ghost instruction cells, selectable linked face, count, filter and
 * failure policy, with untouched vanilla player inventory underneath.
 */
public final class TabletScreen extends AbstractContainerScreen<TabletMenu> {
    private static final ResourceLocation CHEST=
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int GHOST_Y=37;
    private static final int GHOST_X=25;
    private int selected;
    public TabletScreen(TabletMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=212;imageHeight=292;
        inventoryLabelY=195;titleLabelY=8;
    }
    public void refreshState(){rebuildWidgets();}
    private void send(String action,int value){
        PacketDistributor.sendToServer(new SequencerNetworking.TabletAction(menu.hand(),action,selected,value));
    }
    private CompoundTag current(){
        ListTag steps=menu.state().getList("Steps",Tag.TAG_COMPOUND);
        return selected<steps.size()?steps.getCompound(selected):new CompoundTag();
    }
    private CompoundTag stepAt(int index){
        ListTag steps=menu.state().getList("Steps",Tag.TAG_COMPOUND);
        return index<steps.size()?steps.getCompound(index):new CompoundTag();
    }
    private int amount(){int c=current().getInt("Amount");return c<=0?1:c;}
    private int failure(){return Math.floorMod(menu.state().getInt("OnFailure"),3);}
    private String linkDescription(){
        int index=current().contains("Link")?current().getInt("Link"):-1;
        ListTag links=menu.state().getList("Links",Tag.TAG_COMPOUND);
        if(index<0||index>=links.size())return "Target: NONE - shift/right-click face to link";
        CompoundTag l=links.getCompound(index);
        net.minecraft.core.BlockPos pos=net.minecraft.core.BlockPos.of(l.getLong("Pos"));
        net.minecraft.core.Direction side=net.minecraft.core.Direction.values()[
                Math.floorMod(l.getInt("Face"),6)];
        return "Target: "+pos.getX()+","+pos.getY()+","+pos.getZ()+" "+side.getName();
    }
    private Button add(int x,int y,int w,String title,Runnable onClick,String tip){
        Button btn=addRenderableWidget(Button.builder(Component.literal(title),b->onClick.run())
                .bounds(leftPos+x,topPos+y,w,18).build());
        if(!tip.isEmpty())btn.setTooltip(Tooltip.create(Component.literal(tip)));
        return btn;
    }
    @Override protected void init(){
        super.init();
        add(8,76,196,linkDescription(),()->send("link",0),
                "Cycles the face links recorded by shift-right-clicking a target block");
        add(8,100,96,current().getBoolean("Receive")?"Receive ←":"Send →",
                ()->send("direction",0),"Send Host to target; Receive target to Host");
        add(108,100,96,current().getBoolean("Blacklist")?"Blacklist":"Whitelist",
                ()->send("blacklist",0),"Match listed item/tag or exclude it");
        add(8,124,37,"-16",()->send("amount",-16),"Reduce requested item count");
        add(47,124,37,"-1",()->send("amount",-1),"Reduce requested item count");
        add(86,124,40,Integer.toString(amount()),()->{},"Requested count (1 to 4096)").active=false;
        add(128,124,37,"+1",()->send("amount",1),"Increase requested item count");
        add(167,124,37,"+16",()->send("amount",16),"Increase requested item count");
        if("redstone".equals(current().getString("Card"))) {
            String[] gates={"Powered","Unpowered","Rising edge","Falling edge"};
            int mode=Math.floorMod(current().getInt("Gate"),19);
            String title=mode<4?gates[mode]:"Signal at least "+(mode-3);
            add(8,147,196,"Redstone: "+title,
                    ()->send("gate",0),"Gate checks signal on the linked target face");
        } else {
            add(8,147,196,"Clear selected step",()->send("clear",0),
                    "Erase the item or card from this instruction");
        }
        String[] modes={"Pause / Continue","Skip on failure","Stop on failure"};
        add(8,171,196,"On Failure: "+modes[failure()],()->send("failure",0),
                "Normal waits for missing items or full inventories never skip");
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        // renderPanel draws explicitly before the vanilla widgets and slot items.
    }
    private void renderPanel(GuiGraphics g){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF777777);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+190,0xFFC6C6C6);
        // Exactly the vanilla inventory texture, aligned with real slots.
        g.blit(CHEST,leftPos+17,topPos+189,0,126,176,96);
        for(int i=0;i<9;i++){
            int x=leftPos+GHOST_X+i*18,y=topPos+GHOST_Y;
            g.fill(x,y,x+18,y+18,i==selected?0xFFBE9B30:0xFF444444);
            g.fill(x+1,y+1,x+17,y+17,0xFF8B8B8B);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,8,0x353535,false);
        g.drawString(font,"Select a ghost step, then carry an item or card",8,23,0x4A4A4A,false);
        g.drawString(font,"Step "+(selected+1)+" / 9",8,61,0x353535,false);
        g.drawString(font,playerInventoryTitle,25,195,0x414141,false);
    }
    private void drawGhosts(GuiGraphics g,int mx,int my){
        ListTag steps=menu.state().getList("Steps",Tag.TAG_COMPOUND);
        for(int i=0;i<9;i++){
            CompoundTag step=stepAt(i);
            int x=leftPos+GHOST_X+i*18,y=topPos+GHOST_Y;
            if(step.contains("Item",Tag.TAG_COMPOUND) && minecraft!=null && minecraft.level!=null){
                ItemStack item=ItemStack.parseOptional(minecraft.level.registryAccess(),step.getCompound("Item"));
                if(!item.isEmpty())g.renderItem(item,x+1,y+1);
            } else if(!step.getString("Tag").isEmpty()){
                g.drawCenteredString(font,"#",x+9,y+5,0x4BAAD4);
            } else if("redstone".equals(step.getString("Card"))){
                g.renderItem(new ItemStack(dev.frequencyworks.registration.ModContent.REDSTONE_CARD.get()),x+1,y+1);
            } else if("interaction".equals(step.getString("Card"))){
                g.renderItem(new ItemStack(dev.frequencyworks.registration.ModContent.INTERACTION_CARD.get()),x+1,y+1);
            }
            if(mx>=x && mx<x+18 && my>=y && my<y+18){
                String title=step.getString("Tag");
                if(title.isEmpty())title=step.getString("Card");
                if(title.isEmpty())title="Step "+(i+1);
                g.renderTooltip(font,Component.literal(title+" | left: select/ghost | right: clear"),mx,my);
            }
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if((button==0||button==1) && x>=leftPos+GHOST_X && x<leftPos+GHOST_X+162
                && y>=topPos+GHOST_Y && y<topPos+GHOST_Y+18){
            selected=Math.max(0,Math.min(8,(int)(x-leftPos-GHOST_X)/18));
            if(button==1)send("clear",0);
            else if(!menu.getCarried().isEmpty())send("set",0);
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);
        renderPanel(g);
        super.render(g,mx,my,partial);
        drawGhosts(g,mx,my);
        renderTooltip(g,mx,my);
    }
}
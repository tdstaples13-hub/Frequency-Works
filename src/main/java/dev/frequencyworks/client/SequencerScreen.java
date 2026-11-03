package dev.frequencyworks.client;

import dev.frequencyworks.menu.SequencerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Exactly 1x9 real Tablet slots above standard Minecraft inventory.
 * Always draw the chest panel explicitly before normal menu rendering.
 */
public final class SequencerScreen extends AbstractContainerScreen<SequencerMenu> {
    private static final ResourceLocation CHEST=
        ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private Button resetButton;
    private long clientConfirmUntil;
    public SequencerScreen(SequencerMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=176;imageHeight=131;
        inventoryLabelY=38;titleLabelY=6;
    }
    @Override protected void init(){
        super.init();
        resetButton=addRenderableWidget(Button.builder(Component.literal("R"),button->{
            if(minecraft==null || minecraft.gameMode==null)return;
            long now=minecraft.level==null?0:minecraft.level.getGameTime();
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
            if(menu.partial()){
                clientConfirmUntil=now+100;
                resetButton.setMessage(Component.literal("!"));
                resetButton.setTooltip(Tooltip.create(Component.literal(
                    "Confirm reset: already transferred items will not be returned. Click again within 5 seconds.")));
            } else {
                clientConfirmUntil=0;
            }
        }).bounds(leftPos+158,topPos+1,17,16).build());
        resetButton.setTooltip(Tooltip.create(Component.literal(
            "Restart from Tablet 1, Step 1. Partial transfers require confirmation.")));
    }
    @Override public void containerTick(){
        super.containerTick();
        if(clientConfirmUntil>0 && minecraft!=null && minecraft.level!=null
                && minecraft.level.getGameTime()>clientConfirmUntil){
            clientConfirmUntil=0;
            resetButton.setMessage(Component.literal("R"));
            resetButton.setTooltip(Tooltip.create(Component.literal(
                "Restart from Tablet 1, Step 1. Partial transfers require confirmation.")));
        }
    }
    private void drawPanel(GuiGraphics g){
        g.blit(CHEST,leftPos,topPos,0,0,176,35);
        g.blit(CHEST,leftPos,topPos+35,0,126,176,96);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0x88000000);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        // All panels are explicitly drawn before super.render(), not here.
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,6,0x404040,false);
        g.drawString(font,playerInventoryTitle,8,38,0x404040,false);
        String state=menu.stopped()?"Stopped":menu.waiting()?"Waiting":"Running";
        int color=menu.stopped()?0xAA2828:menu.waiting()?0x966021:0x31723A;
        g.drawString(font,state,103,6,color,false);
        String location="T"+(menu.tabletIndex()+1)+":S"+(menu.stepIndex()+1);
        if(menu.remaining()>0)location+=" "+menu.remaining()+" left";
        g.drawString(font,location,92,38,0x494949,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);
        drawPanel(g);
        super.render(g,mx,my,partial);
        renderTooltip(g,mx,my);
    }
}

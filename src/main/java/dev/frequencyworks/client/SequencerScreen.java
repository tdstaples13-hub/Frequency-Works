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
    private Button resume;
    public SequencerScreen(SequencerMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=194;imageHeight=131;
        inventoryLabelY=38;titleLabelY=6;
    }
    @Override protected void init(){
        super.init();
        resume=addRenderableWidget(Button.builder(Component.literal(">"),b->{
            if(minecraft!=null && minecraft.gameMode!=null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        }).bounds(leftPos+178,topPos+7,14,18).build());
        resume.setTooltip(Tooltip.create(Component.literal("Resume after Stop-on-failure")));
    }
    private void drawPanel(GuiGraphics g){
        g.blit(CHEST,leftPos,topPos,0,0,176,35);
        g.blit(CHEST,leftPos,topPos+35,0,126,176,96);
        g.fill(leftPos+176,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF777777);
        g.fill(leftPos+177,topPos+1,leftPos+imageWidth-1,topPos+imageHeight-1,0xFFC6C6C6);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        // All panels are explicitly drawn before super.render(), not here.
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,6,0x404040,false);
        g.drawString(font,playerInventoryTitle,8,38,0x404040,false);
        String step=menu.stopped()?"STOPPED":"T"+(menu.tabletIndex()+1)+":S"+(menu.stepIndex()+1);
        g.drawString(font,step,109,6,menu.stopped()?0xAE2828:0x31723A,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);
        drawPanel(g);
        super.render(g,mx,my,partial);
        renderTooltip(g,mx,my);
    }
}

package dev.frequencyworks.client;

import dev.frequencyworks.menu.SequencerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Uses the same real inventory panel and slot positions as a vanilla chest. */
public final class SequencerScreen extends AbstractContainerScreen<SequencerMenu> {
    private static final ResourceLocation CHEST=
        ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private Button resume;
    public SequencerScreen(SequencerMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=194; imageHeight=132;
        inventoryLabelY=38;titleLabelY=6;
    }
    @Override protected void init(){
        super.init();
        resume=addRenderableWidget(Button.builder(Component.literal("▶"),b->{
            if(minecraft!=null && minecraft.gameMode!=null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        }).bounds(leftPos+178,topPos+7,14,18).build());
        resume.setTooltip(Tooltip.create(Component.literal("Resume a Sequencer stopped by a Tablet failure")));
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.blit(CHEST,leftPos,topPos,0,0,176,35);
        g.blit(CHEST,leftPos,topPos+35,0,126,176,96);
        g.fill(leftPos+176,topPos,leftPos+194,topPos+imageHeight,0xFF8A8A8A);
        g.fill(leftPos+177,topPos+1,leftPos+193,topPos+imageHeight-1,0xFFC6C6C6);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,6,0x404040,false);
        g.drawString(font,playerInventoryTitle,8,38,0x404040,false);
        String state=menu.stopped()?"STOPPED":"T"+(menu.tabletIndex()+1)+":S"+(menu.stepIndex()+1);
        g.drawString(font,state,107,6,menu.stopped()?0xA43626:0x387346,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);
        super.render(g,mx,my,partial);
        renderTooltip(g,mx,my);
    }
}
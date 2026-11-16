/* Copyright (c) 2014, Lukas Tenbrink. Target-version screen by Sollace. */
package ivorius.psychedelicraft.client.screen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.screen.DryingTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class DryingTableScreen extends AbstractContainerScreen<DryingTableMenu> {
    private static final ResourceLocation TEXTURE=Psychedelicraft.id("textures/gui/drying_table.png");
    public DryingTableScreen(DryingTableMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void init(){super.init();titleLabelX=26;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        renderBackground(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderTooltip(graphics,mouseX,mouseY);
        if(menu.getCarried().isEmpty()&&(hoveredSlot==null||!hoveredSlot.hasItem())
                &&mouseX>leftPos+imageWidth-30&&mouseX<leftPos+imageWidth&&mouseY>topPos&&mouseY<topPos+30){
            graphics.renderTooltip(font,Component.translatable("block.psychedelicraft.drying_table.daylight",(int)(menu.getHeatRatio()*100)),mouseX,mouseY);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY){
        graphics.setColor(1,1,1,1);graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(menu.getProgress()>0)graphics.blit(TEXTURE,leftPos+88,topPos+34,176,59,25,16);
        int progress=(int)(menu.getProgress()*24);graphics.blit(TEXTURE,leftPos+88,topPos+34,176,42,progress+1,16);
        int heat=(int)(menu.getHeatRatio()*20);graphics.blit(TEXTURE,leftPos+148,topPos+26-heat,176,41-heat,20,heat);
    }
}

/* Copyright (c) 2014, Lukas Tenbrink. Native screen layout by Sollace. */
package ivorius.psychedelicraft.client.screen;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.client.render.TankFluidAppearance;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.screen.FluidMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;
public final class FluidMachineScreen extends AbstractContainerScreen<FluidMachineMenu>{
    public FluidMachineScreen(FluidMachineMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    private FluidMachineBlockEntity machine(){return minecraft.level!=null&&minecraft.level.getBlockEntity(menu.getPosition()) instanceof FluidMachineBlockEntity entity?entity:null;}
    private ResourceLocation texture(){var entity=machine();return Psychedelicraft.id("textures/gui/"+(entity instanceof MashTubBlockEntity?"wooden_vat":entity instanceof DistilleryBlockEntity?"distillery":entity instanceof BarrelBlockEntity?"barrel":"flask")+".png");}
    @Override protected void init(){super.init();titleLabelX=83-font.width(title)/2;titleLabelY=10;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        var texture=texture();graphics.setColor(1,1,1,1);graphics.blit(texture,leftPos+30,topPos+20,0,imageHeight,110,50);
        var machine=machine();var fluid=machine==null?FluidStack.EMPTY:machine.getTank().getFluid();
        if(!fluid.isEmpty()&&menu.getCapacity()>0){
            var appearance=TankFluidAppearance.of(fluid,menu.getPosition());int color=appearance.color();float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
            int height=Mth.ceil(27*Mth.clamp(menu.getFluidAmount()/(float)menu.getCapacity(),0,1));
            graphics.enableScissor(leftPos+48,topPos+59-height,leftPos+112,topPos+59);
            for(int x=0;x<64;x+=16)for(int y=0;y<height;y+=16)graphics.blit(leftPos+48+x,topPos+59-height+y,0,16,16,appearance.sprite(),r,g,b,1);
            graphics.disableScissor();
        }
        graphics.blit(texture,leftPos,topPos,0,0,imageWidth,imageHeight);float progress=menu.getProgress();
        if(machine instanceof DistilleryBlockEntity||progress>0&&progress<1){int y=machine instanceof DistilleryBlockEntity||machine instanceof MashTubBlockEntity?14:20;int x=machine instanceof MashTubBlockEntity?140:110;
            graphics.blit(texture,leftPos+x,topPos+y,233,22,23,22);int unfilled=(int)(22*(1-progress));graphics.blit(texture,leftPos+x,topPos+y+unfilled,233,unfilled,23,23-unfilled);
        }
        float input=menu.getVesselProgress(0),output=menu.getVesselProgress(1);
        if(!menu.getSlot(0).getItem().isEmpty()&&input>0&&input<1){int width=(int)(45*input);graphics.blit(texture,leftPos+20+width,topPos+40,176+width,0,45-width,16);}
        if(output>0&&output<1)graphics.blit(texture,leftPos+68,topPos+60,176,17,(int)(53*output),20);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);
        if(menu.getCarried().isEmpty()&&mouseX>=leftPos+65&&mouseX<leftPos+105&&mouseY>=topPos+33&&mouseY<topPos+63)graphics.renderTooltip(font,tankTooltip(),Optional.empty(),mouseX,mouseY);
    }
    public List<Component> tankTooltip(){
        List<Component> result=new ArrayList<>();var machine=machine();FluidStack fluid=machine==null?FluidStack.EMPTY:machine.getTank().getFluid();boolean brew=!fluid.isEmpty()&&PSFluids.brew(fluid.getFluid())!=null;
        result.add(fluid.isEmpty()?Component.translatable("fluid.psychedelicraft.empty"):PSFluids.displayName(fluid));
        if(!fluid.isEmpty()){result.add(Component.translatable("psychedelicraft.container.levels",menu.getFluidAmount(),menu.getCapacity()).withStyle(ChatFormatting.GRAY));if(brew)BrewProfile.appendAttributes(fluid,result);}
        BrewProcess stage=menu.getStage();if(menu.isStewing()){result.add(Component.translatable("fluid.status",Component.translatable("fluid.status.stew")));result.add(Component.translatable("time.until.stew",countdown(menu.getRemainingTicks())));}
        else if(machine instanceof FluidProcessingBlockEntity&&(brew||stage!=BrewProcess.IDLE))result.add(Component.translatable("fluid.status",Component.translatable("fluid.status."+stage.key())));
        if(!menu.isStewing()&&stage!=BrewProcess.IDLE)result.add(Component.translatable("time.until."+stage.key(),countdown(menu.getRemainingTicks())));
        if(brew){boolean fermented=BrewProfile.stage(fluid,BrewProfile.FERMENTATION,2)>0;
            if(machine instanceof DistilleryBlockEntity){result.add(requirement(fermented,"psychedelicraft.processing.fermented","psychedelicraft.distillery.requires_fermentation"));result.add(requirement(BrewProfile.stage(fluid,BrewProfile.MATURATION,16)==0,"psychedelicraft.distillery.unaged","psychedelicraft.distillery.requires_unaged"));}
            else if(machine instanceof BarrelBlockEntity)result.add(requirement(fermented,"psychedelicraft.processing.fermented","psychedelicraft.barrel.requires_fermentation"));
        }
        if(machine instanceof DistilleryBlockEntity&&menu.getOutlet()!=DistilleryBlockEntity.Outlet.READY.ordinal()){
            var values=DistilleryBlockEntity.Outlet.values();var outlet=menu.getOutlet()>=0&&menu.getOutlet()<values.length?values[menu.getOutlet()]:values[0];result.add(Component.translatable(outlet.key()).withStyle(ChatFormatting.RED,ChatFormatting.ITALIC));
        }return result;
    }
    private static Component requirement(boolean met,String satisfied,String needed){return Component.translatable(met?satisfied:needed).withStyle(met?ChatFormatting.GRAY:ChatFormatting.RED);}
    private static String countdown(int ticks){int seconds=ticks/20,hours=seconds/3600,minutes=seconds/60%60;return hours>0?String.format(Locale.ROOT,"%02d:%02d:%02d",hours,minutes,seconds%60):String.format(Locale.ROOT,"%02d:%02d",minutes,seconds%60);}
}

/* Copyright (c) 2014, Lukas Tenbrink. Native renderer by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.client.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import java.util.Random;
public final class MashTubRenderer implements BlockEntityRenderer<MashTubBlockEntity>{
    public MashTubRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(MashTubBlockEntity vat,float partial,PoseStack pose,MultiBufferSource vertices,int light,int overlay){
        var client=Minecraft.getInstance();var fluid=vat.getTank().getFluid();float height=vat.getFluidSurface();
        if(!fluid.isEmpty())new FluidBoxRenderer(pose,vertices.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),TankFluidAppearance.of(fluid,vat.getBlockPos()),light,overlay).draw(-.5F,0,-.5F,2,height,2,Direction.UP);
        else if(vat.getSolidOutput().getItem() instanceof BlockItem block){var sprite=client.getBlockRenderer().getBlockModelShaper().getParticleIcon(block.getBlock().defaultBlockState());new FluidBoxRenderer(pose,vertices.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS)),new TankFluidAppearance(sprite,0xffffffff),light,overlay).draw(-.5F,0,-.5F,2,.5F,2,Direction.UP);}
        var supplied=vat.getSuppliedIngredients();long seed=vat.getBlockPos().asLong()+1;Random random=new Random(seed);float ticks=vat.getLevel().getGameTime()+partial;
        for(int i=0;i<supplied.size();i++){
            float x=.5F+(random.nextFloat()-.5F)*1.5F,z=.5F+(random.nextFloat()-.5F)*1.5F,rotation=random.nextFloat()*360;
            pose.pushPose();pose.translate(x,Math.max(.1F,height)+.025F,z);pose.mulPose(Axis.YP.rotationDegrees(rotation));pose.scale(.3F,.3F,.3F);
            pose.translate(0,Mth.sin((ticks+i*5)/8F)*.2F,-.2F);pose.mulPose(Axis.ZP.rotationDegrees(6*Mth.cos((ticks+i*5)/8F)));pose.mulPose(Axis.YP.rotationDegrees((ticks+i)%360));
            client.getItemRenderer().renderStatic(supplied.get(i),ItemDisplayContext.FIXED,light,overlay,pose,vertices,vat.getLevel(),(int)seed+i);pose.popPose();
        }
    }
}

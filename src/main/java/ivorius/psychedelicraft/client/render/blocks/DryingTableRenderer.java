/* Copyright (c) 2014, Lukas Tenbrink. Target-version renderer by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

public final class DryingTableRenderer implements BlockEntityRenderer<DryingTableBlockEntity> {
    public DryingTableRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(DryingTableBlockEntity table,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        Random random=new Random(table.getBlockPos().asLong()+1);
        pose.pushPose();
        try{
            pose.translate(0,.75,0);
            for(int slot=0;slot<table.getContainerSize();slot++){
                ItemStack stack=table.getItem(slot);if(stack.isEmpty())continue;
                boolean output=slot==0;
                float x=output?.5F:.35F+random.nextFloat()*.3F,z=output?.5F:.35F+random.nextFloat()*.3F;
                float rotation=random.nextFloat()*360;
                pose.pushPose();
                try{
                    pose.translate(x,slot/500F,z);pose.mulPose(Axis.YP.rotationDegrees(rotation));
                    float scale=output?.75F:.5F;pose.scale(scale,scale,scale);pose.translate(0,0,-.2);
                    pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(50));
                    Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,table.getLevel(),(int)table.getBlockPos().asLong());
                }finally{pose.popPose();}
            }
        }finally{pose.popPose();}
    }
}

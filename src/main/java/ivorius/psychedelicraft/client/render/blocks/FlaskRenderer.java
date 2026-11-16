/* Copyright (c) 2014, Lukas Tenbrink. Native renderer by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.client.render.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
public final class FlaskRenderer<T extends FluidMachineBlockEntity> implements BlockEntityRenderer<T>{
    public FlaskRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(T entity,float partial,PoseStack matrices,MultiBufferSource vertices,int light,int overlay){
        matrices.pushPose();
        matrices.translate(0.5F, 0, 0.5F);

        float scale = 1/8F - 0.001F;
        matrices.scale(scale, scale, scale);


        var tank = entity.getTank();
        var fluid = tank.getFluid();

        if (!fluid.isEmpty()) {
            float fillPercentage = Mth.clamp((float) tank.getFluidAmount() / (float) tank.getCapacity(), 0, 1);

            var appearance=TankFluidAppearance.of(fluid,entity.getBlockPos());
            FluidBoxRenderer fluidRenderer=new FluidBoxRenderer(matrices,vertices.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),appearance,light,overlay);

            float firstLevelHeight = Math.min(fillPercentage * 2, 2);

            // lower
            fluidRenderer.draw(-1, 0, -2, 2, firstLevelHeight, 1, Direction.NORTH, Direction.UP);
            fluidRenderer.draw(-1, 0,  1, 2, firstLevelHeight, 1, Direction.SOUTH, Direction.UP);

            fluidRenderer.draw( 1, 0, -1, 1, firstLevelHeight, 2, Direction.EAST, Direction.UP);
            fluidRenderer.draw(-2, 0, -1, 1, firstLevelHeight, 2, Direction.WEST, Direction.UP);

            fillPercentage = Math.max(fillPercentage - 0.5F, 0);
            float secondLevelHeight = Math.min(fillPercentage * 2, 2);
            if (secondLevelHeight > 0) {
                if (!(entity instanceof DistilleryBlockEntity)) {
                    matrices.translate(0, -1, 0);
                    fluidRenderer=new FluidBoxRenderer(matrices,vertices.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),appearance,light,overlay);
                }

                fluidRenderer.draw(-1, 4.5F, -1.5F, 2, secondLevelHeight, 0.5F, Direction.NORTH, Direction.UP);
                fluidRenderer.draw(-1, 4.5F,  1, 2, secondLevelHeight, 0.5F, Direction.SOUTH, Direction.UP);

                fluidRenderer.draw( 1, 4.5F, -1, 0.5F, secondLevelHeight, 2, Direction.EAST, Direction.UP);
                fluidRenderer.draw(-1.5F, 4.5F, -1, 0.5F, secondLevelHeight, 2, Direction.WEST, Direction.UP);
            }
        }

        matrices.popPose();
    }
}

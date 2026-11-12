/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import java.util.Random;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.PeyoteBlock;
import ivorius.psychedelicraft.block.entity.PeyoteBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class PeyoteRenderer extends TileEntitySpecialRenderer<PeyoteBlockEntity> {
    private final PeyoteModel[] models = {
            PeyoteModel.stage0(), PeyoteModel.stage1(), PeyoteModel.stage2(), PeyoteModel.stage3()
    };
    private final ResourceLocation[] textures = new ResourceLocation[models.length];

    public PeyoteRenderer() {
        for (int age = 0; age < textures.length; age++) {
            textures[age] = Psychedelicraft.id("textures/entity/peyote/peyote_stage" + age + ".png");
        }
    }

    @Override
    public void render(PeyoteBlockEntity tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        World world = tile.getWorld();
        BlockPos pos = tile.getPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != PSBlocks.PEYOTE) {
            return;
        }
        int age = state.getValue(PeyoteBlock.AGE);
        Vec3d offset = state.getOffset(world, pos);
        float turn = new Random(MathHelper.getPositionRandom(pos)).nextInt(4) * 180F;
        bindTexture(textures[age]);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + 0.5 + offset.x, y + 1.5 + offset.y, z + 0.5 + offset.z);
            GlStateManager.rotate(turn, 0, 1, 0);
            GlStateManager.rotate(180, 1, 0, 0);
            models[age].render();
        } finally {
            GlStateManager.popMatrix();
        }
    }
}

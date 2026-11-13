package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nullable;

public final class WaterloggedRenderer {
    private WaterloggedRenderer() {
    }

    @Nullable
    public static Boolean render(BlockFluidRenderer renderer, IBlockState state, BlockPos pos,
                                 IBlockAccess world, BufferBuilder buffer) {
        if (MinecraftForgeClient.getRenderLayer() != BlockRenderLayer.TRANSLUCENT || !Waterlogging.supports(state)) {
            return null;
        }
        return Waterlogging.isWet(world, pos, state)
                && renderer.renderFluid(world, Blocks.WATER.getDefaultState(), pos, buffer);
    }
}

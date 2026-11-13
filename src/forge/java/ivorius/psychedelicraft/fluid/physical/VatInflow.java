package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public final class VatInflow {
    private VatInflow() {}

    public static boolean canAcceptVanilla(BlockDynamicLiquid liquid, World world, BlockPos pos, IBlockState state) {
        if (!(state.getBlock() instanceof MashTubBlock)) return false;
        return fill(world, pos, vanillaBucket(liquid), false);
    }

    public static boolean fillIfVanillaVat(BlockDynamicLiquid liquid, World world, BlockPos pos, IBlockState state) {
        if (!(state.getBlock() instanceof MashTubBlock)) return false;
        fill(world, pos, vanillaBucket(liquid), true);
        return true;
    }

    private static FluidStack vanillaBucket(BlockDynamicLiquid liquid) {
        Fluid fluid = liquid.getDefaultState().getMaterial() == Material.WATER ? FluidRegistry.WATER : FluidRegistry.LAVA;
        return new FluidStack(fluid, Fluid.BUCKET_VOLUME);
    }

    static boolean canAccept(World world, BlockPos pos, IBlockState state, FluidStack bucket) {
        return state.getBlock() instanceof MashTubBlock && fill(world, pos, bucket, false);
    }

    // A vat is never displaced by a spread attempt, even if its tank refuses the liquid.
    static boolean fillIfVat(World world, BlockPos pos, IBlockState state, FluidStack bucket) {
        if (!(state.getBlock() instanceof MashTubBlock)) return false;
        fill(world, pos, bucket, true);
        return true;
    }

    private static boolean fill(World world, BlockPos pos, FluidStack bucket, boolean doFill) {
        if (!world.isBlockLoaded(pos) || doFill && world.isRemote) return false;
        MashTubBlockEntity vat = MashTubBlock.getVat(world, pos);
        return vat != null && vat.getTank().fillExactly(bucket, doFill);
    }
}

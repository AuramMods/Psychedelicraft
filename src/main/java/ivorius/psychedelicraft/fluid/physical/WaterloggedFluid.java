package ivorius.psychedelicraft.fluid.physical;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public abstract class WaterloggedFluid implements IFluidHandler {
    private final TileEntity owner;
    private final IFluidTankProperties tank = new IFluidTankProperties() {
        @Override public FluidStack getContents() { return isWet() ? source() : null; }
        @Override public int getCapacity() { return Fluid.BUCKET_VOLUME; }
        @Override public boolean canFill() { return !isWet(); }
        @Override public boolean canDrain() { return isWet(); }
        @Override public boolean canFillFluidType(FluidStack fluid) { return isPlainWater(fluid); }
        @Override public boolean canDrainFluidType(FluidStack fluid) { return isPlainWater(fluid); }
    };

    protected WaterloggedFluid(TileEntity owner) {
        this.owner = owner;
    }

    public abstract boolean isWet();

    protected abstract void store(boolean wet);

    @Override
    public int fill(FluidStack resource, boolean execute) {
        World world = owner.getWorld();
        if (isWet() || !isPlainWater(resource) || resource.amount < Fluid.BUCKET_VOLUME
                || !isPresent(world) || world.provider.doesWaterVaporize()) return 0;
        if (execute && !world.isRemote) setWet(world, true);
        return Fluid.BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean execute) {
        return isPlainWater(resource) ? drain(resource.amount, execute) : null;
    }

    @Override
    public FluidStack drain(int amount, boolean execute) {
        World world = owner.getWorld();
        if (!isWet() || amount < Fluid.BUCKET_VOLUME || !isPresent(world)) return null;
        if (execute && !world.isRemote) setWet(world, false);
        return source();
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[]{tank};
    }

    private boolean isPresent(@Nullable World world) {
        BlockPos pos = owner.getPos();
        return world != null && !owner.isInvalid() && world.getTileEntity(pos) == owner
                && Waterlogging.supports(world.getBlockState(pos));
    }

    private void setWet(World world, boolean wet) {
        BlockPos pos = owner.getPos();
        store(wet);
        owner.markDirty();
        if (!world.captureBlockSnapshots) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
            world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
        }
        if (wet) Waterlogging.scheduleFlow(world, pos);
    }

    private static boolean isPlainWater(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == FluidRegistry.WATER
                && (stack.tag == null || stack.tag.hasNoTags());
    }

    private static FluidStack source() {
        return new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME);
    }
}

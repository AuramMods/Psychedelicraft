package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public class WaterloggedBlockEntity extends TileEntity implements IFluidHandler {
    private boolean waterlogged;
    private final IFluidTankProperties properties = new IFluidTankProperties() {
        @Override
        public FluidStack getContents() {
            return waterlogged ? source() : null;
        }

        @Override
        public int getCapacity() {
            return Fluid.BUCKET_VOLUME;
        }

        @Override
        public boolean canFill() {
            return !waterlogged;
        }

        @Override
        public boolean canDrain() {
            return waterlogged;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return isPlainWater(fluid);
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluid) {
            return isPlainWater(fluid);
        }
    };

    public final boolean isWaterlogged() {
        return waterlogged;
    }

    @Override
    public int fill(FluidStack resource, boolean execute) {
        if (waterlogged || !isPlainWater(resource) || resource.amount < Fluid.BUCKET_VOLUME
                || !isPresent() || world.provider.doesWaterVaporize()) return 0;
        if (execute && !world.isRemote) setWaterlogged(true);
        return Fluid.BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean execute) {
        return isPlainWater(resource) ? drain(resource.amount, execute) : null;
    }

    @Override
    public FluidStack drain(int amount, boolean execute) {
        if (!waterlogged || amount < Fluid.BUCKET_VOLUME || !isPresent()) return null;
        if (execute && !world.isRemote) setWaterlogged(false);
        return source();
    }

    private boolean isPresent() {
        return !isInvalid() && world != null && world.getTileEntity(pos) == this
                && Waterlogging.supports(world.getBlockState(pos));
    }

    private static boolean isPlainWater(@Nullable FluidStack stack) {
        return stack != null && stack.getFluid() == FluidRegistry.WATER
                && (stack.tag == null || stack.tag.hasNoTags());
    }

    private static FluidStack source() {
        return new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME);
    }

    private void setWaterlogged(boolean wet) {
        waterlogged = wet;
        onWaterloggedChanged(wet);
        markDirty();
        if (!world.captureBlockSnapshots) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
            world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
        }
        if (wet) Waterlogging.scheduleFlow(world, pos);
    }

    protected void onWaterloggedChanged(boolean wet) {
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[]{properties};
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this) : super.getCapability(capability, side);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState previous, IBlockState next) {
        return previous.getBlock() != next.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("waterlogged", waterlogged);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        waterlogged = tag.getBoolean("waterlogged");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
        world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
    }
}

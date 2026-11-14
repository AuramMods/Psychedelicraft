package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.physical.WaterloggedFluid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public class WaterloggedBlockEntity extends TileEntity implements IFluidHandler {
    private boolean waterlogged;
    private final WaterloggedFluid fluid = new WaterloggedFluid(this) {
        @Override
        public boolean isWet() {
            return waterlogged;
        }

        @Override
        protected void store(boolean wet) {
            waterlogged = wet;
            onWaterloggedChanged(wet);
        }
    };

    public final boolean isWaterlogged() {
        return waterlogged;
    }

    @Override
    public int fill(FluidStack resource, boolean execute) {
        return fluid.fill(resource, execute);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean execute) {
        return fluid.drain(resource, execute);
    }

    @Override
    public FluidStack drain(int amount, boolean execute) {
        return fluid.drain(amount, execute);
    }

    protected void onWaterloggedChanged(boolean wet) {
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return fluid.getTankProperties();
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

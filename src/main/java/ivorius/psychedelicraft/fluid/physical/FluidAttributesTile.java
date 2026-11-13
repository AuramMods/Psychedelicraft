package ivorius.psychedelicraft.fluid.physical;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class FluidAttributesTile extends TileEntity {
    private NBTTagCompound attributes = new NBTTagCompound();

    NBTTagCompound getAttributes() {
        return attributes.copy();
    }

    void setAttributes(NBTTagCompound tag) {
        if (attributes.equals(tag)) return;
        attributes = tag.copy();
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 2);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock() || !newState.getBlock().hasTileEntity(newState);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("attributes", attributes.copy());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        attributes = tag.getCompoundTag("attributes").copy();
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
    public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
        world.markBlockRangeForRenderUpdate(pos, pos);
    }
}

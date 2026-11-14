package ivorius.psychedelicraft.block.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BeeNestBlockEntity extends TileEntity {
    private EnumFacing facing = EnumFacing.NORTH;

    public BeeNestBlockEntity() {
    }

    public BeeNestBlockEntity(EnumFacing facing) {
        this.facing = facing;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public void setFacing(EnumFacing facing) {
        if (this.facing == facing) return;
        this.facing = facing;
        if (world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        } else {
            world.markChunkDirty(pos, this);
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        EnumFacing saved = EnumFacing.byName(tag.getString("Facing"));
        facing = saved != null && saved.getAxis().isHorizontal() ? saved : EnumFacing.NORTH;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("Facing", facing.getName());
        return tag;
    }

    @Override
    public void rotate(Rotation rotation) {
        facing = rotation.rotate(facing);
    }

    @Override
    public void mirror(Mirror mirror) {
        facing = mirror.mirror(facing);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
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
        EnumFacing previous = facing;
        handleUpdateTag(packet.getNbtCompound());
        if (facing != previous) world.markBlockRangeForRenderUpdate(pos, pos);
    }
}

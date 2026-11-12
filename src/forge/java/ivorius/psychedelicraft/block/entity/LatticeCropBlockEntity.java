package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.LatticeCropBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class LatticeCropBlockEntity extends TileEntity {
    private int age;
    private boolean persistent;

    public int getAge() {
        return age;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setGrowth(int age, boolean persistent) {
        int validAge = MathHelper.clamp(age, 0, LatticeCropBlock.MAX_AGE);
        if (this.age != validAge || this.persistent != persistent) {
            this.age = validAge;
            this.persistent = persistent;
            markDirty();
            if (world != null && !world.isRemote) {
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 2);
            }
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("age", age);
        tag.setBoolean("persistent", persistent);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        age = MathHelper.clamp(tag.getInteger("age"), 0, LatticeCropBlock.MAX_AGE);
        persistent = tag.getBoolean("persistent");
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
        world.markBlockRangeForRenderUpdate(pos, pos);
    }
}

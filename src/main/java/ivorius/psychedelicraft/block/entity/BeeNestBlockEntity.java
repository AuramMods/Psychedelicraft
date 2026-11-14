package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.BeeNestBlock;
import ivorius.psychedelicraft.entity.BeeEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class BeeNestBlockEntity extends TileEntity implements ITickable {
    private static final int CAPACITY = 3;
    private static final int REST_TICKS = 600;
    private static final int NECTAR_TICKS = 2400;
    private final List<Occupant> occupants = new ArrayList<>(CAPACITY);
    private EnumFacing facing = EnumFacing.NORTH;
    private boolean movingWithOccupants;

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
        occupants.clear();
        NBTTagList bees = tag.getTagList("Bees", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < Math.min(CAPACITY, bees.tagCount()); i++) {
            NBTTagCompound bee = bees.getCompoundTagAt(i);
            if (bee.hasKey("EntityData", Constants.NBT.TAG_COMPOUND)) {
                occupants.add(new Occupant(bee.getCompoundTag("EntityData").copy(), Math.max(0, bee.getInteger("TicksInHive"))));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("Facing", facing.getName());
        if (!occupants.isEmpty()) tag.setTag("Bees", writeOccupants());
        return tag;
    }

    public NBTTagList writeOccupants() {
        NBTTagList list = new NBTTagList();
        for (Occupant occupant : occupants) {
            NBTTagCompound stored = new NBTTagCompound();
            stored.setTag("EntityData", occupant.data.copy());
            stored.setInteger("TicksInHive", occupant.ticks);
            list.appendTag(stored);
        }
        return list;
    }

    public int getOccupantCount() {
        return occupants.size();
    }

    public boolean isFull() {
        return occupants.size() >= CAPACITY;
    }

    public void seed(Random random) {
        if (!occupants.isEmpty()) return;
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) occupants.add(new Occupant(new NBTTagCompound(), random.nextInt(599)));
        world.markChunkDirty(pos, this);
    }

    public boolean tryEnter(BeeEntity bee) {
        if (world.isRemote || isFull() || !bee.isEntityAlive() || bee.world != world || bee.isRiding() || bee.isBeingRidden()) return false;
        NBTTagCompound data = bee.writeToNBT(new NBTTagCompound());
        data.removeTag("HivePos");
        data.removeTag("UUIDMost");
        data.removeTag("UUIDLeast");
        occupants.add(new Occupant(data, 0));
        bee.setDead();
        world.markChunkDirty(pos, this);
        return true;
    }

    public void setMovingWithOccupants(boolean movingWithOccupants) {
        this.movingWithOccupants = movingWithOccupants;
    }

    public void releaseOnBreak() {
        if (movingWithOccupants) return;
        occupants.removeIf(occupant -> release(occupant, true, false));
    }

    @Override
    public void update() {
        if (world.isRemote || occupants.isEmpty()) return;
        boolean day = world.isDaytime() && !world.isRaining();
        Iterator<Occupant> bees = occupants.iterator();
        while (bees.hasNext()) {
            Occupant bee = bees.next();
            boolean nectar = bee.data.getBoolean("HasNectar");
            int minimum = nectar ? NECTAR_TICKS : REST_TICKS;
            if (bee.ticks > minimum && day && release(bee, false, nectar)) {
                bees.remove();
            } else if (bee.ticks < Integer.MAX_VALUE) {
                bee.ticks++;
            }
        }
        world.markChunkDirty(pos, this);
    }

    private boolean release(Occupant occupant, boolean emergency, boolean delivered) {
        BlockPos exit = pos.offset(facing);
        boolean blocked = !world.isBlockLoaded(exit) || world.getBlockState(exit).getCollisionBoundingBox(world, exit) != null;
        if (blocked && !emergency) return false;
        BeeEntity bee = new BeeEntity(world);
        if (!occupant.data.hasNoTags()) bee.readFromNBT(occupant.data.copy());
        bee.motionX = bee.motionY = bee.motionZ = 0D;
        double offset = blocked ? 0D : 0.55D + bee.width * 0.5D;
        bee.setLocationAndAngles(pos.getX() + 0.5D + offset * facing.getFrontOffsetX(),
                pos.getY() + 0.5D - bee.height * 0.5D, pos.getZ() + 0.5D + offset * facing.getFrontOffsetZ(),
                facing.getHorizontalAngle(), 0F);
        bee.leaveNest(pos, delivered, occupant.ticks);
        if (!world.spawnEntity(bee)) return false;
        if (delivered) {
            IBlockState state = world.getBlockState(pos);
            int level = state.getValue(BeeNestBlock.HONEY_LEVEL);
            if (level < BeeNestBlock.FULL_HONEY) {
                int increase = world.rand.nextInt(100) == 0 ? 2 : 1;
                world.setBlockState(pos, state.withProperty(BeeNestBlock.HONEY_LEVEL, Math.min(BeeNestBlock.FULL_HONEY, level + increase)), 3);
            }
        }
        return true;
    }

    private static final class Occupant {
        private final NBTTagCompound data;
        private int ticks;

        private Occupant(NBTTagCompound data, int ticks) {
            this.data = data;
            this.ticks = ticks;
        }
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
        NBTTagCompound tag = super.getUpdateTag();
        tag.setString("Facing", facing.getName());
        return tag;
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

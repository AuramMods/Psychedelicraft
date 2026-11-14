/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.entity.RealityRiftEntity;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RiftJarBlockEntity extends TileEntity implements ITickable {
    private float amount;
    private boolean opening;
    private boolean sucking = true;
    private float lid;
    private float handle;
    private float previousLid;
    private float previousHandle;
    private float lastSentAmount;
    private int visualAge;
    private final Map<UUID, Connection> connections = new HashMap<>();

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = Float.isFinite(amount) ? Math.max(0, amount) : 0;
        publish();
    }

    public boolean toggleLid() {
        opening = !opening;
        publish();
        return opening;
    }

    public void toggleMode() {
        sucking = !sucking;
        publish();
    }

    public float getLid(float partialTicks) {
        return previousLid + (lid - previousLid) * partialTicks;
    }

    public float getHandle(float partialTicks) {
        return previousHandle + (handle - previousHandle) * partialTicks;
    }

    public int getVisualAge() {
        return visualAge;
    }

    public Collection<Connection> getConnections() {
        return connections.values();
    }

    @Override
    public void update() {
        previousLid = lid;
        previousHandle = handle;
        lid = opening ? Math.min(1, lid + 0.02F) : Math.max(0, lid - 0.02F);
        handle = sucking ? Math.max(0, handle - 0.04F) : Math.min(1, handle + 0.04F);
        visualAge++;

        if (world.isRemote) {
            tickConnections();
            return;
        }

        float before = amount;
        if (lid > 0) {
            if (sucking) collect();
            else if (amount > 0) discharge();
        }
        if (amount > 1) {
            overload();
            return;
        }

        if (before != amount || previousLid != lid || previousHandle != handle) markDirty();
        if (lastSentAmount != amount && (amount == 0 || world.getTotalWorldTime() % 5 == 0)) publish();
    }

    private void collect() {
        List<RealityRiftEntity> rifts = nearbyRifts();
        if (rifts.isEmpty()) return;
        float rate = 0.001F * lid / rifts.size();
        for (RealityRiftEntity rift : rifts) amount += rift.takeFromRift(rate);
    }

    private void discharge() {
        float drained = Math.min(0.0004F * lid * amount + 0.0004F, amount);
        AxisAlignedBB area = new AxisAlignedBB(pos.getX() - 5, pos.getY() - 5, pos.getZ() - 2,
                pos.getX() + 6, pos.getY() + 6, pos.getZ() + 6);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, area, EntitySelectors.CAN_AI_TARGET)) {
            DrugProperties drugs = DrugCapability.get(player);
            if (drugs != null) {
                double effect = (5 - player.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) * 0.2F * drained;
                drugs.addToDrug(DrugType.ZERO, effect * 5);
                drugs.addToDrug(DrugType.POWER, effect * 35);
            }
        }
        amount -= drained;
    }

    private void overload() {
        List<RealityRiftEntity> rifts = nearbyRifts();
        if (rifts.isEmpty()) {
            RealityRiftEntity escaped = new RealityRiftEntity(world);
            escaped.setPosition(pos.getX() + 1, pos.getY() + 3.5, pos.getZ() + 1);
            escaped.setRiftSize(amount);
            world.spawnEntity(escaped);
        } else {
            RealityRiftEntity first = rifts.get(0);
            first.setRiftSize(first.getRiftSize() + amount);
        }
        world.setBlockToAir(pos);
        world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, false, true);
    }

    private List<RealityRiftEntity> nearbyRifts() {
        return world.getEntitiesWithinAABB(RealityRiftEntity.class,
                new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2,
                        pos.getX() + 3, pos.getY() + 10, pos.getZ() + 3), EntitySelectors.IS_ALIVE);
    }

    private void tickConnections() {
        if (sucking && lid > 0) {
            for (RealityRiftEntity rift : nearbyRifts()) {
                Connection connection = connections.computeIfAbsent(rift.getUniqueID(),
                        key -> new Connection(new Vec3d(rift.posX, rift.posY + rift.getEyeHeight(), rift.posZ)));
                connection.strength = Math.min(1, connection.strength + 0.02F * lid);
            }
        }
        connections.values().removeIf(connection -> (connection.strength -= 0.01F) <= 0);
    }

    private void publish() {
        if (world != null && !world.isRemote) {
            markDirty();
            lastSentAmount = amount;
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState previous, IBlockState next) {
        return previous.getBlock() != next.getBlock();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos).grow(3, 10, 3);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("currentRiftFraction", amount);
        tag.setBoolean("isOpening", opening);
        tag.setFloat("fractionOpen", lid);
        tag.setBoolean("suckingRifts", sucking);
        tag.setFloat("fractionHandleUp", handle);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        float stored = tag.getFloat("currentRiftFraction");
        amount = Float.isFinite(stored) ? Math.max(0, stored) : 0;
        opening = tag.getBoolean("isOpening");
        previousLid = lid = tag.getFloat("fractionOpen");
        sucking = !tag.hasKey("suckingRifts") || tag.getBoolean("suckingRifts");
        previousHandle = handle = tag.getFloat("fractionHandleUp");
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
    }

    public static final class Connection {
        private final Vec3d position;
        private float strength;

        private Connection(Vec3d position) {
            this.position = position;
        }

        public Vec3d getPosition() {
            return position;
        }

        public float getStrength() {
            return strength;
        }
    }
}

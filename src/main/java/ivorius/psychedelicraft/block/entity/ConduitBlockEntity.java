package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.client.particle.ConduitParticles;
import ivorius.psychedelicraft.entity.ConduitPower;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConduitBlockEntity extends WaterloggedBlockEntity implements ITickable {
    private final List<BlockPos> frame = new ArrayList<>();
    private int ticks;
    private int activeTicks;
    private long nextAmbient;
    private UUID targetId;
    private EntityLivingBase target;

    public boolean isActive() { return frame.size() >= 16; }
    public boolean isHunting() { return frame.size() == 42; }
    public int ticks() { return ticks; }
    public float rotation(float partialTicks) { return (activeTicks + partialTicks) * -0.0375F; }

    @Override
    public void update() {
        ticks++;
        long now = world.getTotalWorldTime();
        if (ticks == 1 || now % 40 == 0) {
            boolean wasActive = isActive();
            refreshFrame();
            if (!world.isRemote) {
                if (wasActive != isActive()) world.playSound(null, pos,
                        isActive() ? PSSounds.CONDUIT_ACTIVATE : PSSounds.CONDUIT_DEACTIVATE, SoundCategory.BLOCKS, 1F, 1F);
                if (isActive()) applyPower();
                updateTarget();
            }
        }
        if (world.isRemote) {
            if (targetId != null && (target == null || !targetId.equals(target.getUniqueID()))) target = findTarget();
            if (targetId == null) target = null;
            ConduitParticles.animate(world, pos, frame, target, ticks);
        } else if (isActive()) {
            if (now % 80 == 0) world.playSound(null, pos, PSSounds.CONDUIT_AMBIENT, SoundCategory.BLOCKS, 1F, 1F);
            if (now > nextAmbient) {
                nextAmbient = now + 60 + world.rand.nextInt(40);
                world.playSound(null, pos, PSSounds.CONDUIT_AMBIENT_SHORT, SoundCategory.BLOCKS, 1F, 1F);
            }
        }
        if (isActive()) activeTicks++;
    }

    private void refreshFrame() {
        frame.clear();
        if (!world.isAreaLoaded(pos, 2)) return;
        for (BlockPos water : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
            if (!Waterlogging.isWater(world, water)) return;
        }
        for (BlockPos candidate : BlockPos.getAllInBoxMutable(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
            if (!isFrameOffset(candidate.getX() - pos.getX(), candidate.getY() - pos.getY(), candidate.getZ() - pos.getZ())) continue;
            Block block = world.getBlockState(candidate).getBlock();
            if (block == Blocks.PRISMARINE || block == Blocks.SEA_LANTERN) frame.add(candidate.toImmutable());
        }
    }

    static boolean isFrameOffset(int x, int y, int z) {
        return (x == 0 || y == 0 || z == 0) && Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2;
    }

    static int effectRange(int blocks) { return blocks / 7 * 16; }

    private void applyPower() {
        int range = effectRange(frame.size());
        for (EntityPlayer player : world.playerEntities) {
            if (pos.distanceSq(player.getPosition()) < range * range && player.isWet()) {
                player.addPotionEffect(new PotionEffect(ConduitPower.EFFECT, 260, 0, true, true));
            }
        }
    }

    private void updateTarget() {
        UUID old = targetId;
        if (!isHunting()) {
            target = null;
        } else if (target == null && targetId != null) {
            target = findTarget();
        } else if (target == null) {
            List<EntityLivingBase> choices = world.getEntitiesWithinAABB(EntityLivingBase.class, attackArea(),
                    mob -> mob instanceof IMob && mob.isWet() && mob.isEntityAlive() && pos.distanceSq(mob.getPosition()) < 64D);
            if (!choices.isEmpty()) target = choices.get(world.rand.nextInt(choices.size()));
        } else if (!target.isEntityAlive() || pos.distanceSq(target.getPosition()) >= 64D) {
            target = null;
        }
        targetId = target == null ? null : target.getUniqueID();
        if (target != null) {
            world.playSound(null, target.posX, target.posY, target.posZ, PSSounds.CONDUIT_ATTACK, SoundCategory.BLOCKS, 1F, 1F);
            target.attackEntityFrom(DamageSource.MAGIC, 4F);
        }
        if (!Objects.equals(old, targetId)) {
            markDirty();
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    private AxisAlignedBB attackArea() {
        return new AxisAlignedBB(pos).grow(8);
    }

    private EntityLivingBase findTarget() {
        List<EntityLivingBase> matches = world.getEntitiesWithinAABB(EntityLivingBase.class, attackArea(),
                entity -> targetId.equals(entity.getUniqueID()));
        return matches.size() == 1 ? matches.get(0) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (targetId != null) tag.setUniqueId("Target", targetId);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        targetId = tag.hasUniqueId("Target") ? tag.getUniqueId("Target") : null;
    }
}

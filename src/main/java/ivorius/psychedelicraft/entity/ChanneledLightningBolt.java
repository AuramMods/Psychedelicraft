package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.advancement.PSCriteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class ChanneledLightningBolt extends EntityLightningBolt {
    private final EntityPlayerMP shooter;
    private final List<Entity> victims = new ArrayList<>();
    private boolean striking;

    public ChanneledLightningBolt(World world, BlockPos strike, EntityPlayerMP shooter) {
        super(world, strike.getX() + 0.5, strike.getY(), strike.getZ() + 0.5, false);
        this.shooter = shooter;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (striking) {
            PSCriteria.channeledLightning(shooter, victims);
            victims.clear();
            striking = false;
        }
    }

    public static List<Entity> beginStrike(World world, Entity bolt, AxisAlignedBB range) {
        if (bolt instanceof ChanneledLightningBolt) ((ChanneledLightningBolt) bolt).striking = true;
        return world.getEntitiesWithinAABBExcludingEntity(bolt, range);
    }

    public static void strike(Entity victim, EntityLightningBolt bolt) {
        boolean record = bolt instanceof ChanneledLightningBolt && victim.isEntityAlive();
        victim.onStruckByLightning(bolt);
        if (record) ((ChanneledLightningBolt) bolt).victims.add(victim);
    }
}

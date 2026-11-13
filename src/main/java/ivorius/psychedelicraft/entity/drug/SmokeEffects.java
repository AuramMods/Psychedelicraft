/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.item.SmokeableItem;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

public final class SmokeEffects {
    private SmokeEffects() {
    }

    public static void playBreath(EntityPlayer player) {
        if (!player.world.isRemote) {
            player.world.playSound(null, player.posX, player.posY, player.posZ, PSSounds.BREATH, SoundCategory.PLAYERS, 0.02F, 1.5F);
        }
    }

    static void exhale(EntityPlayer player, int color) {
        Random random = player.getRNG();
        if (random.nextBoolean()) puff(player, color, 1);
        if (random.nextInt(5) == 0) puff(player, color, 2.5F);
    }

    private static void puff(EntityPlayer player, int color, float scale) {
        Vec3d look = player.getLookVec();
        Vec3d origin = player.getPositionEyes(1).addVector(look.x * 0.1, look.y * 0.1 - 0.1, look.z * 0.1);
        double speed = player.getRNG().nextFloat() * 0.05 + 0.1;
        Vec3d velocity = look.scale(speed).addVector(player.motionX, player.motionY + 0.03, player.motionZ);
        Channel.spawnSmoke(player.world, origin, velocity, color, scale, 1);
    }

    public static void fuelConsumed(TileEntityFurnace furnace) {
        World world = furnace.getWorld();
        if (!(furnace.getStackInSlot(1).getItem() instanceof SmokeableItem) || world.isRemote) return;
        SmokeableItem item = (SmokeableItem) furnace.getStackInSlot(1).getItem();
        BlockPos pos = furnace.getPos();
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(3),
                player -> !player.isSpectator())) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) item.applyInfluences(properties);
        }
        Random random = world.rand;
        for (int puff = 0; puff < 30; puff++) {
            double centerX = pos.getX() + 0.5 + triangularOffset(random);
            double centerZ = pos.getZ() + 0.5 + triangularOffset(random);
            // With a positive count, the donor's packet offsets spread the spawn position.
            Vec3d origin = new Vec3d(centerX + random.nextGaussian() * (float) triangularOffset(random),
                    pos.getY() + 1 + random.nextGaussian() * (float) (0.8 + triangularOffset(random)),
                    centerZ + random.nextGaussian() * (float) triangularOffset(random));
            Vec3d velocity = new Vec3d(random.nextGaussian() * 0.001F, random.nextGaussian() * 0.001F,
                    random.nextGaussian() * 0.001F);
            Channel.spawnSmoke(world, origin, velocity, item.smokeColor(), 1, 1);
        }
    }

    private static double triangularOffset(Random random) {
        return 0.3 * (random.nextDouble() - random.nextDouble());
    }
}

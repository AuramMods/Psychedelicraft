/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.SmokeableItem;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class SmokeEffects {
    private static final SoundEvent BREATH = new SoundEvent(Psychedelicraft.id("entity.player.breath"))
            .setRegistryName(Psychedelicraft.id("entity.player.breath"));

    private SmokeEffects() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(BREATH);
    }

    public static void playBreath(EntityPlayer player) {
        if (!player.world.isRemote) {
            player.world.playSound(null, player.posX, player.posY, player.posZ, BREATH, SoundCategory.PLAYERS, 0.02F, 1.5F);
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
        Channel.spawnSmoke(world, new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5),
                new Vec3d(0, 0.18, 0), item.smokeColor(), 1, 30);
    }
}

/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.INetHandler;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
public final class SlurryBubble extends Particle {
    private static void spawn(World world, Vec3d center, int count, double spread, int color) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.gameSettings.particleSetting == 2) return;
        Random random = world.rand;
        int amount = client.gameSettings.particleSetting == 1 ? (count + 1) / 2 : count;
        for (int i = 0; i < amount; i++) {
            double x = center.x + (random.nextDouble() - random.nextDouble()) * spread;
            double z = center.z + (random.nextDouble() - random.nextDouble()) * spread;
            client.effectRenderer.addEffect(new SlurryBubble(world, new Vec3d(x, center.y, z), Vec3d.ZERO, color));
        }
    }

    public static void receive(INetHandler sender, int dimension, Vec3d origin, int count, double spread, int color) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() == sender && client.world != null && client.world.provider.getDimension() == dimension) {
            spawnMash(client.world, origin, count, spread, color, false);
        }
    }

    public static void spawnMash(World world, Vec3d center, int count, int color) {
        spawnMash(world, center, count, 0, color, true);
    }

    private static void spawnMash(World world, Vec3d center, int count, double spread, int color, boolean local) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.gameSettings.particleSetting == 2) return;
        Random random = world.rand;
        double radius = 0.5 + spread;
        Vec3d motion = new Vec3d(triangular(random, 0.125), 0.1 + triangular(random, 0.125), triangular(random, 0.125));
        Vec3d velocity = local ? motion.scale(0.2F) : Vec3d.ZERO;
        for (int i = 0; i < count; i++) {
            if (client.gameSettings.particleSetting == 1 && random.nextInt(3) == 0) continue;
            Vec3d origin = center.addVector(triangular(random, radius), triangular(random, radius), triangular(random, radius));
            // The donor's counted server packets use the supplied motion as additional position spread.
            if (!local) origin = origin.addVector(random.nextGaussian() * (float) motion.x,
                    random.nextGaussian() * (float) motion.y, random.nextGaussian() * (float) motion.z);
            client.effectRenderer.addEffect(new SlurryBubble(world, origin, velocity, color));
        }
    }

    private static double triangular(Random random, double radius) {
        return (random.nextDouble() - random.nextDouble()) * radius;
    }

    public static void ambient(World world, Vec3d center, int color, double spread, Random random) {
        spawn(world, center, 5, spread, color);
        world.playSound(center.x, center.y, center.z, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS,
                0.5F + random.nextFloat(), 0.3F + random.nextFloat(), true);
    }

    private SlurryBubble(World world, Vec3d origin, Vec3d velocity, int color) {
        super(world, origin.x, origin.y, origin.z);
        setParticleTextureIndex(32);
        setSize(0.02F, 0.02F);
        particleScale *= rand.nextFloat() * 0.6F + 0.2F;
        particleMaxAge = (int) (8 / (rand.nextDouble() * 0.8 + 0.2));
        motionX = velocity.x + (rand.nextDouble() - 0.5) * 0.04;
        motionY = velocity.y + (rand.nextDouble() - 0.5) * 0.04;
        motionZ = velocity.z + (rand.nextDouble() - 0.5) * 0.04;
        setRBGColorF((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setExpired();
            return;
        }
        move(motionX, motionY + 0.002, motionZ);
        motionX *= 0.85;
        motionY = (motionY + 0.002) * 0.85;
        motionZ *= 0.85;
    }
}

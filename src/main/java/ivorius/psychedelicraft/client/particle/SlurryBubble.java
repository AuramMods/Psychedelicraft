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
    public static void spawn(World world, Vec3d center, int count, double spread, int color) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.gameSettings.particleSetting == 2) return;
        Random random = world.rand;
        int amount = client.gameSettings.particleSetting == 1 ? (count + 1) / 2 : count;
        for (int i = 0; i < amount; i++) {
            double x = center.x + (random.nextDouble() - random.nextDouble()) * spread;
            double z = center.z + (random.nextDouble() - random.nextDouble()) * spread;
            client.effectRenderer.addEffect(new SlurryBubble(world, x, center.y, z, color));
        }
    }

    public static void receive(INetHandler sender, int dimension, Vec3d origin, int count, double spread, int color) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() == sender && client.world != null && client.world.provider.getDimension() == dimension) {
            spawn(client.world, origin, count, spread, color);
        }
    }

    public static void ambient(World world, Vec3d center, int color, double spread, Random random) {
        spawn(world, center, 5, spread, color);
        world.playSound(center.x, center.y, center.z, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS,
                0.5F + random.nextFloat(), 0.3F + random.nextFloat(), true);
    }

    private SlurryBubble(World world, double x, double y, double z, int color) {
        super(world, x, y, z);
        setParticleTextureIndex(32);
        setSize(0.02F, 0.02F);
        particleScale *= rand.nextFloat() * 0.6F + 0.2F;
        particleMaxAge = (int) (8 / (rand.nextDouble() * 0.8 + 0.2));
        motionX = (rand.nextDouble() - 0.5) * 0.04;
        motionY = (rand.nextDouble() - 0.5) * 0.04;
        motionZ = (rand.nextDouble() - 0.5) * 0.04;
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

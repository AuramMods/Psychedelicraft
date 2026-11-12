/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SlurryBubble extends Particle {
    public static void spawn(World world, double x, double y, double z, int color) {
        if (Minecraft.getMinecraft().gameSettings.particleSetting != 2) {
            Minecraft.getMinecraft().effectRenderer.addEffect(new SlurryBubble(world, x, y, z, color));
        }
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

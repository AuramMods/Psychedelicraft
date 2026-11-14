/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.particle;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEnchantmentTable;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RealityRiftParticles {
    private RealityRiftParticles() {}

    public static void spawn(World world, double x, double y, double z, float height, Random random) {
        Minecraft client = Minecraft.getMinecraft();
        double halfHeight = height * 0.5F;
        Vec3d center = new Vec3d(x, y + halfHeight, z);
        for (int count = random.nextInt(3); count > 0; count--) {
            Vec3d position = center.add(offset(random));
            if (visible(client, world, position)) {
                client.effectRenderer.addEffect(new SmokeOrDust(world, position, true));
            }
        }
        if (random.nextInt(2) != 0) {
            Vec3d position = center.add(offset(random));
            if (visible(client, world, position)) {
                client.effectRenderer.addEffect(new SmokeOrDust(world, position, false));
            }
        }
        Vec3d position = center.addVector(0, 1, 0);
        Vec3d velocity = offset(random).addVector(0, halfHeight, 0);
        if (visible(client, world, position)) {
            client.effectRenderer.addEffect(new Enchantment(world, position, velocity));
        }
    }

    private static Vec3d offset(Random random) {
        float spread = random.nextFloat() * random.nextFloat();
        return new Vec3d((random.nextFloat() * 8 - 4) * spread,
                (random.nextFloat() * 8 - 4) * spread, (random.nextFloat() * 8 - 4) * spread);
    }

    private static boolean visible(Minecraft client, World world, Vec3d position) {
        Entity camera = client.getRenderViewEntity();
        if (client.world != world || camera == null || client.effectRenderer == null) return false;
        int setting = client.gameSettings.particleSetting;
        return setting != 2 && (setting != 1 || world.rand.nextInt(3) != 0)
                && camera.getDistanceSq(position.x, position.y, position.z) <= 1024;
    }

    private static final class SmokeOrDust extends Particle {
        private final float baseScale;
        private boolean stoppedByCollision;

        private SmokeOrDust(World world, Vec3d position, boolean smoke) {
            super(world, position.x, position.y, position.z, smoke ? 0 : -10, smoke ? 0 : -10, smoke ? 0 : -10);
            motionX *= 0.1F;
            motionY *= 0.1F;
            motionZ *= 0.1F;
            particleGravity = smoke ? -0.1F : 0F;

            if (smoke) {
                float color = world.rand.nextFloat() * 0.3F;
                setRBGColorF(color, color, color);
                particleMaxAge = (int) (8D / (world.rand.nextFloat() * 0.8D + 0.2D) * 2.5F);
            } else {
                float shade = rand.nextFloat() * 0.4F + 0.6F;
                setRBGColorF(shade * (rand.nextFloat() * 0.2F + 0.8F),
                        0.5F * shade * (rand.nextFloat() * 0.2F + 0.8F),
                        0.5F * shade * (rand.nextFloat() * 0.2F + 0.8F));
                particleMaxAge = (int) (8D / (rand.nextDouble() * 0.8D + 0.2D));
            }
            baseScale = particleScale * (0.75F * (smoke ? 2.5F : 1));
            setParticleTextureIndex(7);
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

            motionY -= 0.04D * particleGravity;
            if (!stoppedByCollision) {
                move(motionX, motionY, motionZ);
                stoppedByCollision = Math.abs(motionY) >= 1.0E-5F && Math.abs(posY - prevPosY) < 1.0E-5F;
            }
            if (posY == prevPosY) {
                motionX *= 1.1;
                motionZ *= 1.1;
            }
            motionX *= 0.96F;
            motionY *= 0.96F;
            motionZ *= 0.96F;
            if (onGround) {
                motionX *= 0.7F;
                motionZ *= 0.7F;
            }
            setParticleTextureIndex(7 - particleAge * 7 / particleMaxAge);
        }

        @Override
        public void renderParticle(BufferBuilder buffer, Entity view, float partialTicks, float rotationX,
                                   float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
            particleScale = baseScale * MathHelper.clamp((particleAge + partialTicks) / particleMaxAge * 32F, 0F, 1F);
            super.renderParticle(buffer, view, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        }
    }

    private static final class Enchantment extends ParticleEnchantmentTable {
        private final Vec3d origin;

        private Enchantment(World world, Vec3d origin, Vec3d velocity) {
            super(world, origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
            this.origin = origin;
            canCollide = false;
            setPosition(origin.x + velocity.x, origin.y + velocity.y, origin.z + velocity.z);
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

            float progress = (float) particleAge / particleMaxAge;
            float remaining = 1 - progress;
            float fall = 1 - remaining;
            fall *= fall;
            fall *= fall;
            setPosition(origin.x + motionX * remaining, origin.y + motionY * remaining - fall * 1.2F,
                    origin.z + motionZ * remaining);
        }
    }
}

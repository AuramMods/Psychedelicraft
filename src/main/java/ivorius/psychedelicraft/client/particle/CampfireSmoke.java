package ivorius.psychedelicraft.client.particle;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
public final class CampfireSmoke extends Particle {
    private static final ResourceLocation[] SPRITES = {
            Psychedelicraft.id("particle/campfire_smoke_0"), Psychedelicraft.id("particle/campfire_smoke_1"),
            Psychedelicraft.id("particle/campfire_smoke_2"), Psychedelicraft.id("particle/campfire_smoke_3")
    };

    public static void stitch(TextureMap atlas) {
        for (ResourceLocation sprite : SPRITES) atlas.registerSprite(sprite);
    }

    public static void spawn(World world, BlockPos pos, boolean signal, int count, boolean dowsed) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.gameSettings.particleSetting == 2 || client.getRenderViewEntity() == null
                || client.getRenderViewEntity().getDistanceSqToCenter(pos) > 512D * 512D) return;
        Random random = world.rand;
        for (int i = 0; i < count; i++) {
            if (client.gameSettings.particleSetting == 1 && random.nextInt(3) == 0) continue;
            double x = pos.getX() + 0.5D + random.nextDouble() * (random.nextBoolean() ? 1D : -1D) / 3D;
            double y = pos.getY() + random.nextDouble() + random.nextDouble();
            double z = pos.getZ() + 0.5D + random.nextDouble() * (random.nextBoolean() ? 1D : -1D) / 3D;
            client.effectRenderer.addEffect(new CampfireSmoke(world, x, y, z, signal));
            if (dowsed) world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, pos.getY() + 0.4D, z, 0, 0.005D, 0);
        }
    }

    private CampfireSmoke(World world, double x, double y, double z, boolean signal) {
        super(world, x, y, z);
        particleScale *= 3F;
        setSize(0.25F, 0.25F);
        particleMaxAge = (signal ? 280 : 80) + rand.nextInt(50);
        motionY = 0.07D + rand.nextFloat() / 500D;
        setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(SPRITES[rand.nextInt(SPRITES.length)].toString()));
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge || particleAlpha <= 0F) {
            setExpired();
            return;
        }
        motionX += rand.nextFloat() * (rand.nextBoolean() ? 1D : -1D) / 5000D;
        motionZ += rand.nextFloat() * (rand.nextBoolean() ? 1D : -1D) / 5000D;
        motionY -= 0.000003D;
        move(motionX, motionY, motionZ);
        if (particleAge >= particleMaxAge - 60 && particleAlpha > 0.01F) particleAlpha -= 0.015F;
    }
}

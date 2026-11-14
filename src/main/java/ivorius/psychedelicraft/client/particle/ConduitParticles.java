package ivorius.psychedelicraft.client.particle;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

@SideOnly(Side.CLIENT)
public final class ConduitParticles extends Particle {
    private static final ResourceLocation SPRITE = Psychedelicraft.id("particle/nautilus");
    private final double originX;
    private final double originY;
    private final double originZ;

    public static void stitch(TextureMap atlas) {
        atlas.registerSprite(SPRITE);
    }

    public static void animate(World world, BlockPos pos, List<BlockPos> frame, EntityLivingBase target, int ticks) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.gameSettings.particleSetting == 2 || client.getRenderViewEntity() == null
                || client.getRenderViewEntity().getDistanceSqToCenter(pos) > 1024D) return;
        Random random = world.rand;
        double bob = MathHelper.sin((ticks + 35) * .1F) / 2F + .5F;
        for (BlockPos part : frame) {
            if (random.nextInt(50) == 0) {
                client.effectRenderer.addEffect(new ConduitParticles(world, pos.getX() + .5, pos.getY() + 1.5 + (bob * bob + bob) * .3,
                        pos.getZ() + .5, part.getX() - pos.getX() - .5 + random.nextFloat(),
                        part.getY() - pos.getY() - 2 + random.nextFloat(), part.getZ() - pos.getZ() - .5 + random.nextFloat()));
            }
        }
        if (target != null) {
            double spread = 3 + target.width;
            client.effectRenderer.addEffect(new ConduitParticles(world, target.posX, target.posY + target.getEyeHeight(), target.posZ,
                    (random.nextFloat() - .5) * spread, random.nextFloat() * target.height - 1, (random.nextFloat() - .5) * spread));
        }
    }

    private ConduitParticles(World world, double x, double y, double z, double dx, double dy, double dz) {
        super(world, x + dx, y + dy, z + dz);
        originX = x;
        originY = y;
        originZ = z;
        motionX = dx;
        motionY = dy;
        motionZ = dz;
        particleMaxAge = 30 + rand.nextInt(10);
        particleScale = .1F * (.2F + rand.nextFloat() * .5F);
        float shade = .4F + rand.nextFloat() * .6F;
        setRBGColorF(shade * .9F, shade * .9F, shade);
        setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(SPRITE.toString()));
    }

    @Override public int getFXLayer() { return 1; }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setExpired();
            return;
        }
        float age = (float) particleAge / particleMaxAge;
        float remaining = 1F - age;
        setPosition(originX + motionX * remaining, originY + motionY * remaining - Math.pow(age, 4) * 1.2,
                originZ + motionZ * remaining);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        int light = super.getBrightnessForRender(partialTick);
        int sky = (light >> 16) & 255;
        return (light & 255) | Math.min(240, sky + (int) (240 * Math.pow((float) particleAge / particleMaxAge, 4))) << 16;
    }
}

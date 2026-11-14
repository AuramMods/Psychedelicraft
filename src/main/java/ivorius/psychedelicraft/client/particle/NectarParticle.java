package ivorius.psychedelicraft.client.particle;

import ivorius.psychedelicraft.entity.BeeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NectarParticle extends Particle {
    public static void trySpawn(BeeEntity bee) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != bee.world || client.gameSettings.particleSetting == 2 || bee.getRNG().nextFloat() >= 0.05F) return;
        int count = 1 + bee.getRNG().nextInt(2);
        for (int i = 0; i < count; i++) {
            if (client.gameSettings.particleSetting == 1 && bee.world.rand.nextInt(3) == 0) continue;
            double x = bee.posX + (bee.world.rand.nextDouble() - 0.5D) * 0.6D;
            double z = bee.posZ + (bee.world.rand.nextDouble() - 0.5D) * 0.6D;
            client.effectRenderer.addEffect(new NectarParticle(bee.world, x, bee.posY + bee.height * 0.5D, z));
        }
    }

    private NectarParticle(World world, double x, double y, double z) {
        super(world, x, y, z);
        setSize(0.01F, 0.01F);
        setParticleTextureIndex(112);
        setRBGColorF(0.92F, 0.782F, 0.72F);
        particleMaxAge = (int) (16D / (rand.nextDouble() * 0.8D + 0.2D));
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
        motionY -= 0.007F;
        move(0D, motionY, 0D);
        if (onGround) setExpired();
        motionY *= 0.98D;
    }
}

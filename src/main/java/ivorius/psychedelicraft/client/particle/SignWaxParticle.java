package ivorius.psychedelicraft.client.particle;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleSimpleAnimated;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SignWaxParticle extends ParticleSimpleAnimated {
    public static void spawn(World world, BlockPos pos) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.gameSettings.particleSetting == 2) return;
        Random random = world.rand;
        for (EnumFacing face : EnumFacing.VALUES) {
            int count = 3 + random.nextInt(3);
            int dx = face.getFrontOffsetX();
            int dy = face.getFrontOffsetY();
            int dz = face.getFrontOffsetZ();
            for (int i = 0; i < count; i++) {
                if (client.gameSettings.particleSetting == 1 && random.nextInt(3) == 0) continue;
                double x = pos.getX() + 0.5 + (dx == 0 ? random.nextDouble() - 0.5 : dx * 0.55);
                double y = pos.getY() + 0.5 + (dy == 0 ? random.nextDouble() - 0.5 : dy * 0.55);
                double z = pos.getZ() + 0.5 + (dz == 0 ? random.nextDouble() - 0.5 : dz * 0.55);
                client.effectRenderer.addEffect(new SignWaxParticle(world, x, y, z, dx, dy, dz));
            }
        }
    }

    private SignWaxParticle(World world, double x, double y, double z, int dx, int dy, int dz) {
        super(world, x, y, z, 160, 8, 0);
        setRBGColorF(0.91F, 0.55F, 0.08F);
        setParticleTextureIndex(167);
        setBaseAirFriction(0.96F);
        particleScale *= 0.75F;
        particleMaxAge = 10 + rand.nextInt(30);
        canCollide = false;
        motionX = dx == 0 ? (rand.nextDouble() - 0.5) * 0.005 : 0;
        motionY = dy == 0 ? (rand.nextDouble() - 0.5) * 0.01 : 0;
        motionZ = dz == 0 ? (rand.nextDouble() - 0.5) * 0.005 : 0;
    }

    @Override
    public boolean shouldDisableDepth() {
        return false;
    }
}

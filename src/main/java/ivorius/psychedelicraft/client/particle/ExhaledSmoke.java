package ivorius.psychedelicraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.network.INetHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
public final class ExhaledSmoke extends ParticleSmokeNormal {
    private ExhaledSmoke(World world, Vec3d origin, Vec3d velocity, int color, float scale) {
        super(world, origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z, scale);
        setRBGColorF((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
    }

    public static void spawn(INetHandler sender, int dimension, Vec3d origin, Vec3d velocity, int color, float scale, int count) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() != sender || client.world == null || client.world.provider.getDimension() != dimension
                || client.gameSettings.particleSetting == 2) return;
        Random random = client.world.rand;
        if (count == 1 && client.gameSettings.particleSetting == 1 && random.nextInt(3) == 0) return;
        int amount = Math.min(count, client.gameSettings.particleSetting == 1 ? 10 : 30);
        for (int i = 0; i < amount; i++) {
            Vec3d from = count > 1 ? origin.addVector(random.nextGaussian() * 0.12, 0, random.nextGaussian() * 0.12) : origin;
            Vec3d motion = count > 1 ? velocity.addVector(random.nextGaussian() * 0.06,
                    random.nextGaussian() * 0.06, random.nextGaussian() * 0.06) : velocity;
            client.effectRenderer.addEffect(new ExhaledSmoke(client.world, from, motion, color, scale));
        }
    }
}

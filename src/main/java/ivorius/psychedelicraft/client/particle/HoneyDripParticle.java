package ivorius.psychedelicraft.client.particle;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class HoneyDripParticle extends Particle {
    private enum Stage { HANGING, FALLING, LANDED }

    private final Stage stage;

    public static void trySpawn(World world, BlockPos nest) {
        if (world.rand.nextFloat() < 0.3F) return;
        BlockPos below = nest.down();
        IBlockState state = world.getBlockState(below);
        if (state.getMaterial().isLiquid() || state.getBlock() instanceof IFluidBlock
                || Waterlogging.keepsWaterSource(world, below) || hasFullCollision(world, below, state)) return;
        Minecraft client = Minecraft.getMinecraft();
        double x = nest.getX() + world.rand.nextDouble();
        double z = nest.getZ() + world.rand.nextDouble();
        if (client.world != world || client.gameSettings.particleSetting == 2
                || (client.gameSettings.particleSetting == 1 && world.rand.nextInt(3) == 0)) return;
        client.effectRenderer.addEffect(new HoneyDripParticle(world, x, nest.getY() - 0.05D, z, Stage.HANGING));
    }

    private static boolean hasFullCollision(World world, BlockPos pos, IBlockState state) {
        // Selection bounds can fill a cell even when its collision shape leaves a gap, as with stairs.
        AxisAlignedBB cell = new AxisAlignedBB(pos);
        List<AxisAlignedBB> boxes = new ArrayList<>();
        state.addCollisionBoxToList(world, pos, cell, boxes, null, false);
        for (AxisAlignedBB box : boxes) {
            if (box.minX <= cell.minX && box.maxX >= cell.maxX && box.minY <= cell.minY && box.maxY >= cell.maxY
                    && box.minZ <= cell.minZ && box.maxZ >= cell.maxZ) return true;
        }
        return false;
    }

    private HoneyDripParticle(World world, double x, double y, double z, Stage stage) {
        super(world, x, y, z);
        this.stage = stage;
        setSize(0.01F, 0.01F);
        switch (stage) {
            case HANGING:
                setParticleTextureIndex(113);
                setRBGColorF(0.622F, 0.508F, 0.082F);
                particleGravity = 0.000012F;
                particleMaxAge = 100;
                break;
            case FALLING:
                setParticleTextureIndex(112);
                setRBGColorF(0.582F, 0.448F, 0.082F);
                particleGravity = 0.01F;
                particleMaxAge = dripLifetime(64);
                break;
            case LANDED:
                setParticleTextureIndex(114);
                setRBGColorF(0.522F, 0.408F, 0.082F);
                particleGravity = 0.06F;
                particleMaxAge = dripLifetime(128);
                break;
        }
    }

    private int dripLifetime(int ticks) {
        return (int) (ticks / (rand.nextDouble() * 0.8D + 0.2D));
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setExpired();
            if (stage == Stage.HANGING) advanceTo(Stage.FALLING);
            return;
        }
        motionY -= particleGravity;
        move(0, motionY, 0);
        if (stage == Stage.FALLING && onGround) {
            setExpired();
            advanceTo(Stage.LANDED);
            world.playSound(posX, posY, posZ, PSSounds.BEE_NEST_DRIP, SoundCategory.BLOCKS,
                    0.3F + rand.nextFloat() * 0.7F, 1.0F, false);
        }
        motionY *= stage == Stage.HANGING ? 0.0196D : 0.98D;
    }

    private void advanceTo(Stage next) {
        HoneyDripParticle drip = new HoneyDripParticle(world, posX, posY, posZ, next);
        if (next == Stage.FALLING) drip.motionY = motionY;
        Minecraft.getMinecraft().effectRenderer.addEffect(drip);
    }
}

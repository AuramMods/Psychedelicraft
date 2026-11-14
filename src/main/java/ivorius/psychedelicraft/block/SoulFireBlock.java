package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public final class SoulFireBlock extends Block {
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0, 0, 0, 1, 1D / 16D, 1);

    SoulFireBlock() {
        super(Material.FIRE, MapColor.LIGHT_BLUE);
        setHardness(0F);
        setLightLevel(10F / 15F);
        setLightOpacity(0);
        setSoundType(SoundType.CLOTH);
        disableStats();
    }

    public static IBlockState stateForIgnition(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() == Blocks.FIRE && hasSoulBase(world, pos)) return PSBlocks.SOUL_FIRE.getDefaultState();
        return state;
    }

    public static boolean isOpenFire(Block block) {
        return block == Blocks.FIRE || block instanceof SoulFireBlock;
    }

    public static int contactDamage(World world, AxisAlignedBB box) {
        // Match the bounds and loaded-chunk check used by the legacy fire scan.
        BlockPos start = new BlockPos(box.minX, box.minY, box.minZ);
        BlockPos end = new BlockPos(MathHelper.ceil(box.maxX) - 1, MathHelper.ceil(box.maxY) - 1,
                MathHelper.ceil(box.maxZ) - 1);
        if (!world.isAreaLoaded(start, end)) return 1;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(start, end)) {
            if (world.getBlockState(pos).getBlock() instanceof SoulFireBlock) return 2;
        }
        return 1;
    }

    private static boolean hasSoulBase(World world, BlockPos pos) {
        if (pos.getY() <= 0 || pos.getY() >= world.getHeight()) return false;
        Block below = world.getBlockState(pos.down()).getBlock();
        return below == Blocks.SOUL_SAND || below == PSBlocks.SOUL_SOIL;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return hasSoulBase(world, pos);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) return;
        int dimension = world.provider.getDimension();
        if ((dimension == 0 || dimension == -1) && Blocks.PORTAL.trySpawnPortal(world, pos)) return;
        if (!hasSoulBase(world, pos)) world.setBlockToAir(pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!world.isRemote && !hasSoulBase(world, pos)) world.setBlockToAir(pos);
    }

    @Override
    public boolean isBurning(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(24) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT,
                    SoundCategory.BLOCKS, 1F + random.nextFloat(), 0.3F + 0.7F * random.nextFloat(), false);
        }
        for (int particle = 0; particle < 3; particle++) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + random.nextDouble(),
                    pos.getY() + 0.5 + random.nextDouble() * 0.5, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public final class KelpBlock extends Block implements IGrowable, Waterloggable {
    public static final int MAX_AGE = 25;
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 15);
    private static final AxisAlignedBB TIP_SHAPE = new AxisAlignedBB(0, 0, 0, 1, 9D / 16D, 1);
    private final boolean older;

    KelpBlock(boolean older) {
        super(Material.PLANTS);
        this.older = older;
        setDefaultState(blockState.getBaseState().withProperty(STAGE, 0));
        setSoundType(SoundType.PLANT);
        setTickRandomly(true);
        setLightOpacity(3);
    }

    public static IBlockState tip(int age) {
        if (age < 16) return PSBlocks.KELP.getStateFromMeta(age);
        return PSBlocks.KELP_STALK.getStateFromMeta(age - 15);
    }

    public static IBlockState stem() {
        return PSBlocks.KELP_STALK.getDefaultState();
    }

    public static int age(IBlockState state) {
        int stage = state.getValue(STAGE);
        if (!((KelpBlock) state.getBlock()).older) return stage;
        return stage == 0 ? -1 : stage + 15;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(STAGE, Math.min(meta & 15, older ? MAX_AGE - 15 : 15));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE);
    }

    @Override
    public boolean alwaysWaterlogged() {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return false;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return Waterlogging.isWaterSource(world.getBlockState(pos)) && hasSupport(world, pos);
    }

    public static boolean hasSupport(World world, BlockPos pos) {
        BlockPos below = pos.down();
        IBlockState support = world.getBlockState(below);
        Block block = support.getBlock();
        return block instanceof KelpBlock || block == Blocks.SOUL_SAND
                || block != Blocks.MAGMA && support.isSideSolid(world, below, EnumFacing.UP);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            world.setBlockState(pos, world.getBlockState(pos.up()).getBlock() instanceof KelpBlock
                    ? stem() : tip(world.rand.nextInt(MAX_AGE)), 3);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!hasSupport(world, pos)) {
            world.destroyBlock(pos, true);
            return;
        }
        boolean hasKelpAbove = world.getBlockState(pos.up()).getBlock() instanceof KelpBlock;
        if (hasKelpAbove && age(state) >= 0) world.setBlockState(pos, stem(), 2);
        else if (!hasKelpAbove && age(state) < 0) world.setBlockState(pos, tip(random.nextInt(MAX_AGE)), 2);
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        int age = age(state);
        if (age >= 0 && age < MAX_AGE && random.nextDouble() < 0.14D) growTip(world, pos, age);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        return hasWaterAbove(world, findTip(world, pos, state));
    }

    private static BlockPos findTip(World world, BlockPos pos, IBlockState state) {
        while (age(state) < 0 && pos.getY() + 1 < world.getHeight()) {
            pos = pos.up();
            state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof KelpBlock)) return null;
        }
        return pos;
    }

    private static boolean hasWaterAbove(World world, BlockPos pos) {
        if (pos == null || pos.getY() + 1 >= world.getHeight() || !world.isBlockLoaded(pos.up())) return false;
        Block above = world.getBlockState(pos.up()).getBlock();
        return above == Blocks.WATER || above == Blocks.FLOWING_WATER;
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        BlockPos tip = findTip(world, pos, state);
        if (tip != null) growTip(world, tip, age(world.getBlockState(tip)));
    }

    private static void growTip(World world, BlockPos pos, int age) {
        if (hasWaterAbove(world, pos) && world.setBlockState(pos.up(), tip(Math.min(MAX_AGE, age + 1)), 3)) {
            world.setBlockState(pos, stem(), 3);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return PSItems.KELP;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(PSItems.KELP);
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return age(state) < 0 ? FULL_BLOCK_AABB : TIP_SHAPE;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
}

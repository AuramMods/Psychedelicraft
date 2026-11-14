package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Random;

import static net.minecraft.block.BlockDoublePlant.EnumBlockHalf.LOWER;
import static net.minecraft.block.BlockDoublePlant.EnumBlockHalf.UPPER;
import static net.minecraft.block.BlockDoublePlant.HALF;

final class PitcherCropBlock extends BlockBush implements IGrowable {
    private static final PropertyInteger AGE = PropertyInteger.create("age", 0, 4);
    private static final int MAX_AGE = 4;
    private static final int TWO_BLOCK_AGE = 3;
    private static final AxisAlignedBB BULB = shape(5, -1, 3);
    private static final AxisAlignedBB CROP = shape(3, -1, 5);
    private static final AxisAlignedBB LOWER_FULL = shape(3, -1, 16);
    private static final AxisAlignedBB[] LOWER_SHAPES = {BULB, shape(3, -1, 14), LOWER_FULL, LOWER_FULL, LOWER_FULL};
    private static final AxisAlignedBB UPPER_SHORT = shape(3, 0, 11);
    private static final AxisAlignedBB UPPER_FULL = shape(3, 0, 15);

    PitcherCropBlock() {
        setDefaultState(blockState.getBaseState().withProperty(HALF, LOWER).withProperty(AGE, 0));
        setCreativeTab(null);
        setSoundType(SoundType.PLANT);
        disableStats();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, HALF);
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Crop;
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        BlockDoublePlant.EnumBlockHalf half = (metadata & 8) == 0 ? LOWER : UPPER;
        return getDefaultState().withProperty(AGE, Math.min(metadata & 7, MAX_AGE)).withProperty(HALF, half);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE) | (state.getValue(HALF) == UPPER ? 8 : 0);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND && super.canPlaceBlockAt(world, pos);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        int age = state.getValue(AGE);
        if (state.getValue(HALF) == UPPER) {
            return age >= TWO_BLOCK_AGE && isHalf(world.getBlockState(pos.down()), LOWER, age);
        }
        return world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND && hasLight(world, pos)
                && (age < TWO_BLOCK_AGE || isHalf(world.getBlockState(pos.up()), UPPER, age));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isAreaLoaded(pos, 1)) return;
        checkAndDropBlock(world, pos, state);
        if (world.getBlockState(pos) == state && state.getValue(HALF) == LOWER && canGrow(world, pos, state, false)
                && ForgeHooks.onCropsGrowPre(world, pos, state, TorchflowerCropBlock.rollFarmlandGrowth(this, world, pos, random))) {
            grow(world, random, pos, state);
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        BlockPos root = state.getValue(HALF) == UPPER ? pos.down() : pos;
        int age = state.getValue(AGE);
        IBlockState lower = world.getBlockState(root);
        if (age >= MAX_AGE || !isHalf(lower, LOWER, age) || !canBlockStay(world, root, lower)) return false;
        if (age + 1 < TWO_BLOCK_AGE) return true;
        return root.getY() + 1 < world.getHeight() && (world.isAirBlock(root.up())
                || isHalf(world.getBlockState(root.up()), UPPER, age));
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        if (!canGrow(world, pos, state, false)) return;
        BlockPos root = state.getValue(HALF) == UPPER ? pos.down() : pos;
        int next = state.getValue(AGE) + 1;
        IBlockState lower = getDefaultState().withProperty(AGE, next);
        // Update the lower half before notifying neighbors about the upper half.
        world.setBlockState(root, lower, 2);
        if (next >= TWO_BLOCK_AGE) world.setBlockState(root.up(), lower.withProperty(HALF, UPPER), 3);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (player.capabilities.isCreativeMode && state.getValue(HALF) == UPPER
                && isHalf(world.getBlockState(pos.down()), LOWER, state.getValue(AGE))) {
            world.setBlockState(pos.down(), Blocks.AIR.getDefaultState(), 2);
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int age = state.getValue(AGE);
        if (state.getValue(HALF) == LOWER) return LOWER_SHAPES[age];
        return age == TWO_BLOCK_AGE ? UPPER_SHORT : UPPER_FULL;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state.getValue(HALF) == UPPER) return NULL_AABB;
        return state.getValue(AGE) == 0 ? BULB : CROP;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(PSItems.PITCHER_POD);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        if (state.getValue(HALF) == UPPER) return Items.AIR;
        return state.getValue(AGE) == MAX_AGE ? PSItems.PITCHER_PLANT : PSItems.PITCHER_POD;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Item item = getItemDropped(state, null, fortune);
        if (item != Items.AIR) drops.add(new ItemStack(item));
    }

    private boolean isHalf(IBlockState state, BlockDoublePlant.EnumBlockHalf half, int age) {
        return state.getBlock() == this && state.getValue(HALF) == half && state.getValue(AGE) == age;
    }

    private static boolean hasLight(World world, BlockPos pos) {
        return world.getLight(pos) >= 8 || world.canSeeSky(pos);
    }

    private static AxisAlignedBB shape(int inset, int bottom, int top) {
        return new AxisAlignedBB(inset / 16D, bottom / 16D, inset / 16D, 1 - inset / 16D, top / 16D, 1 - inset / 16D);
    }
}

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;

import java.util.Random;

final class TorchflowerCropBlock extends BlockCrops {
    private static final PropertyInteger AGE = PropertyInteger.create("age", 0, 1);
    private static final AxisAlignedBB[] SHAPES = {AgePlantBlock.shape(3, 6), AgePlantBlock.shape(3, 10)};

    @Override
    protected PropertyInteger getAgeProperty() {
        return AGE;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    public IBlockState withAge(int age) {
        return age >= getMaxAge() ? PSBlocks.TORCHFLOWER.getDefaultState() : super.withAge(age);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return withAge(metadata & 1);
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Crop;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND && super.canPlaceBlockAt(world, pos);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (random.nextInt(3) != 0) super.updateTick(world, pos, state, random);
    }

    static boolean rollFarmlandGrowth(BlockBush crop, World world, BlockPos pos, Random random) {
        return random.nextInt((int) (25F / getGrowthChance(crop, world, pos)) + 1) == 0;
    }

    @Override
    protected int getBonemealAgeIncrease(World world) {
        return 1;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPES[getAge(state)];
    }

    @Override
    protected Item getSeed() {
        return PSItems.TORCHFLOWER_SEEDS;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(getSeed()));
    }
}

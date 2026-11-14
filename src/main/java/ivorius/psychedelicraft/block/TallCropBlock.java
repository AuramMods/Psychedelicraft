/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.ForgeHooks;

public class TallCropBlock extends BlockBush implements IGrowable {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);
    public static final PropertyBool TOP = PropertyBool.create("top");
    private static final AxisAlignedBB OUTLINE = new AxisAlignedBB(0.125, 0, 0.125, 0.875, 1, 0.875);

    private final Crop crop;
    private final boolean natural;

    TallCropBlock(Crop crop, boolean natural) {
        this.crop = crop;
        this.natural = natural;
        IBlockState initial = blockState.getBaseState().withProperty(ageProperty(), 0);
        setDefaultState(hasTop() ? initial.withProperty(TOP, false) : initial);
        setSoundType(SoundType.PLANT);
        setCreativeTab(null);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ageProperty());
    }

    protected PropertyInteger ageProperty() {
        return AGE;
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        if (hasTop()) {
            return getDefaultState().withProperty(ageProperty(), metadata & 7).withProperty(TOP, (metadata & 8) != 0);
        }
        return getDefaultState().withProperty(ageProperty(), Math.min(crop.maxAge, metadata & 15));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ageProperty()) | (hasTop() && state.getValue(TOP) ? 8 : 0);
    }

    public boolean hasTop() {
        return crop.height == 2;
    }

    public int maxAge(IBlockState state) {
        return crop == Crop.COFFEA && state.getValue(TOP) ? 3 : crop.maxAge;
    }

    public boolean isMature(IBlockState state) {
        return state.getValue(ageProperty()) == maxAge(state);
    }

    public int maxHeight() {
        return crop.height;
    }

    public IBlockState stateForHeight(int height) {
        return hasTop() ? getDefaultState().withProperty(TOP, height > 0) : getDefaultState();
    }

    public IBlockState stateForHeight(int height, int age) {
        IBlockState state = stateForHeight(height);
        return state.withProperty(ageProperty(), MathHelper.clamp(age, 0, maxAge(state)));
    }

    public static boolean isWildSoil(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return OUTLINE;
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return natural || crop != Crop.CANNABIS ? EnumPlantType.Plains : EnumPlantType.Crop;
    }

    @Override
    protected boolean canSustainBush(IBlockState soil) {
        Block floor = soil.getBlock();
        if (sameCrop(floor)) {
            return true;
        }
        if (natural) {
            return isWildSoil(soil);
        }
        return floor == Blocks.FARMLAND || (crop != Crop.CANNABIS && isWildSoil(soil));
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        IBlockState soil = world.getBlockState(pos.down());
        if (natural) {
            return canSustainBush(soil);
        }
        return (world.getLight(pos) >= 8 || world.canSeeSky(pos))
                && soil.getBlock().canSustainPlant(soil, world, pos.down(), EnumFacing.UP, this);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        super.updateTick(world, pos, state, random);
        if (world.getBlockState(pos).getBlock() == this && world.getLightFromNeighbors(pos.up()) >= 9
                && canGrow(world, pos, state, false)
                && ForgeHooks.onCropsGrowPre(world, pos, state, random.nextFloat() < crop.chance)) {
            growSteps(world, pos, state, 1);
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(ageProperty()) < maxAge(state) || canGrowUp(world, pos);
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        growSteps(world, pos, state, 1 + random.nextInt(hasTop() ? 2 : 4));
    }

    private void growSteps(World world, BlockPos pos, IBlockState state, int steps) {
        for (int step = 0; step < steps; step++) {
            int age = state.getValue(ageProperty());
            if (age < maxAge(state)) {
                state = state.withProperty(ageProperty(), age + 1);
                world.setBlockState(pos, state, 3);
                if (!hasTop() || state.getValue(ageProperty()) < maxAge(state)) {
                    continue;
                }
            }
            if (!canGrowUp(world, pos)) {
                break;
            }
            pos = pos.up();
            // Newly grown sections use the cultivated block, even above wild plants.
            state = cultivatedBlock().stateForHeight(1);
            world.setBlockState(pos, state, 3);
        }
    }

    private boolean canGrowUp(World world, BlockPos pos) {
        if (pos.getY() + 1 >= world.getHeight() || !world.isAirBlock(pos.up())) {
            return false;
        }
        for (int below = 1; below < maxHeight(); below++) {
            if (!sameCrop(world.getBlockState(pos.down(below)).getBlock())) {
                return true;
            }
        }
        return false;
    }

    private boolean sameCrop(Block block) {
        return block instanceof TallCropBlock && ((TallCropBlock) block).crop == crop;
    }

    private TallCropBlock cultivatedBlock() {
        if (!natural) {
            return this;
        }
        switch (crop) {
            case CANNABIS: return PSBlocks.CANNABIS;
            case HOP: return PSBlocks.HOP;
            case COCA: return PSBlocks.COCA;
            case TOBACCO: return PSBlocks.TOBACCO;
            case COFFEA: return PSBlocks.COFFEA;
            default: throw new AssertionError(crop);
        }
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(seed());
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!BlockDrops.usesDecay(world, chance)) {
            super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
            return;
        }
        NonNullList<ItemStack> drops = NonNullList.create();
        getDrops(drops, world, pos, state, fortune);
        // Coffea's two ordinary rolls apply decay in their entry as well as at table level.
        if (crop == Crop.COFFEA && state.getValue(ageProperty()) > 0) {
            BlockDrops.decay(drops.get(0), world.rand, chance);
        }
        for (ItemStack stack : drops) BlockDrops.decay(stack, world.rand, chance);
        BlockDrops.spawn(drops, world, pos, state, fortune, harvesters.get());
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int age = state.getValue(ageProperty());
        Random random = world instanceof World ? ((World) world).rand : ThreadLocalRandom.current();
        switch (crop) {
            case CANNABIS:
                drops.add(new ItemStack(age >= 5 ? PSItems.CANNABIS_LEAF : PSItems.CANNABIS_SEEDS));
                if (age == 15) {
                    drops.add(new ItemStack(PSItems.CANNABIS_BUDS));
                    drops.add(new ItemStack(PSItems.CANNABIS_SEEDS, bonusCount(random, fortune)));
                }
                break;
            case HOP:
                drops.add(new ItemStack(age >= 11 ? PSItems.HOP_CONES : PSItems.HOP_SEEDS,
                        age == 15 ? 1 + bonusCount(random, fortune) : 1));
                break;
            case COCA:
                drops.add(new ItemStack(age >= 5 ? PSItems.COCA_LEAVES : PSItems.COCA_SEEDS));
                if (age >= 5) {
                    drops.add(new ItemStack(PSItems.COCA_SEEDS, bonusCount(random, fortune)));
                }
                break;
            case TOBACCO:
                drops.add(new ItemStack(age > 0 ? PSItems.TOBACCO_LEAVES : PSItems.TOBACCO_SEEDS, 2));
                if (age == 7 && state.getValue(TOP)) {
                    drops.add(new ItemStack(PSItems.TOBACCO_SEEDS, bonusCount(random, fortune)));
                }
                break;
            case COFFEA:
                if (age > 0) {
                    drops.add(new ItemStack(PSItems.COFFEA_CHERRIES, 6 + random.nextInt(2) + random.nextInt(2)));
                }
                if (age == 7 && state.getValue(TOP)) {
                    drops.add(new ItemStack(PSItems.COFFEA_CHERRIES, bonusCount(random, fortune)));
                }
                break;
        }
    }

    private Item seed() {
        switch (crop) {
            case CANNABIS: return PSItems.CANNABIS_SEEDS;
            case HOP: return PSItems.HOP_SEEDS;
            case COCA: return PSItems.COCA_SEEDS;
            case TOBACCO: return PSItems.TOBACCO_SEEDS;
            case COFFEA: return PSItems.COFFEA_CHERRIES;
            default: throw new AssertionError(crop);
        }
    }

    static int bonusCount(Random random, int fortune) {
        int count = 1;
        for (int trial = 0; trial < fortune + 3; trial++) {
            if (random.nextFloat() < 0.5714286F) {
                count++;
            }
        }
        return count;
    }

    static final class Coca extends TallCropBlock {
        private static final PropertyInteger COCA_AGE = PropertyInteger.create("age", 0, 12);

        Coca(boolean natural) {
            super(Crop.COCA, natural);
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            IBlockState soil = world.getBlockState(pos.down());
            return (soil.getBlock() == Blocks.FARMLAND || isWildSoil(soil)) && super.canPlaceBlockAt(world, pos);
        }

        @Override
        protected PropertyInteger ageProperty() {
            return COCA_AGE;
        }
    }

    static final class TwoPart extends TallCropBlock {
        private static final PropertyInteger SHORT_AGE = PropertyInteger.create("age", 0, 7);

        TwoPart(Crop crop, boolean natural) {
            super(crop, natural);
        }

        @Override
        protected PropertyInteger ageProperty() {
            return SHORT_AGE;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, ageProperty(), TOP);
        }
    }

    enum Crop {
        CANNABIS(15, 3, 0.12F), HOP(15, 3, 0.12F), COCA(12, 3, 0.1F),
        TOBACCO(7, 2, 0.1F), COFFEA(7, 2, 0.1F);

        final int maxAge;
        final int height;
        final float chance;

        Crop(int maxAge, int height, float chance) {
            this.maxAge = maxAge;
            this.height = height;
            this.chance = chance;
        }
    }
}

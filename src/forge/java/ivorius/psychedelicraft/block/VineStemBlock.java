/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class VineStemBlock extends BlockBush {
    public static final int MAX_AGE = 4;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MAX_AGE);
    private static final BlockPos[] SPREAD_OFFSETS = {
            BlockPos.ORIGIN, new BlockPos(0, 0, -1), new BlockPos(1, 0, -1),
            new BlockPos(1, 0, 0), new BlockPos(1, 0, 1), new BlockPos(0, 0, 1),
            new BlockPos(-1, 0, 1), new BlockPos(-1, 0, 0), new BlockPos(-1, 0, -1)
    };

    VineStemBlock() {
        setDefaultState(LatticeBlock.withConnections(blockState.getBaseState().withProperty(AGE, 0), 0));
        setSoundType(SoundType.PLANT);
        setCreativeTab(null);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        IProperty<?>[] properties = Arrays.copyOf(LatticeBlock.LINKS, LatticeBlock.LINKS.length + 1, IProperty[].class);
        properties[LatticeBlock.LINKS.length] = AGE;
        return new BlockStateContainer(this, properties);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, MathHelper.clamp(meta, 0, MAX_AGE));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        int mask = 0;
        for (EnumFacing face : EnumFacing.Plane.HORIZONTAL) {
            if (world.getBlockState(pos.offset(face)).getBlock() == PSBlocks.MORNING_GLORY_LATTICE) {
                mask |= 1 << face.getHorizontalIndex();
            }
        }
        return LatticeBlock.withConnections(state, mask);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BUSH_AABB.offset(state.getOffset(world, pos));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        super.updateTick(world, pos, state, random);
        if (world.getBlockState(pos).getBlock() != this) {
            return;
        }
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            world.setBlockState(pos, state.withProperty(AGE, age + 1), 3);
        }
        for (BlockPos offset : SPREAD_OFFSETS) {
            if (LatticeBlock.growOnto(world, pos.add(offset), PSBlocks.MORNING_GLORY_LATTICE)) {
                return;
            }
        }
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!PlantDrops.usesDecay(world, chance)) {
            super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
            return;
        }
        NonNullList<ItemStack> drops = NonNullList.create();
        getDrops(drops, world, pos, state, fortune);
        for (ItemStack stack : drops) PlantDrops.decay(stack, world.rand, chance);
        PlantDrops.spawn(drops, world, pos, state, fortune, harvesters.get());
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(PSItems.MORNING_GLORY));
        if (state.getValue(AGE) == MAX_AGE) {
            Random random = world instanceof World ? ((World) world).rand : ThreadLocalRandom.current();
            drops.add(new ItemStack(PSItems.MORNING_GLORY_SEEDS, TallCropBlock.bonusCount(random, fortune)));
        }
    }
}

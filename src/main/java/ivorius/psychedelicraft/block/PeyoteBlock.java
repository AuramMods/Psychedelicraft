/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.block.entity.PeyoteBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PeyoteBlock extends SucculentPlantBlock {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);
    private static final AxisAlignedBB[] SHAPES = {shape(2, 4), shape(3, 4), shape(4, 4), shape(5, 4)};

    PeyoteBlock() {
        super(3);
    }

    @Override
    protected PropertyInteger ageProperty() {
        return AGE;
    }

    @Override
    protected AxisAlignedBB[] shapes() {
        return SHAPES;
    }

    @Override
    protected int growthInterval() {
        return 20;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new PeyoteBlockEntity();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return PSItems.PEYOTE;
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return 1 + state.getValue(AGE);
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!BlockDrops.usesDecay(world, chance)) {
            super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
            return;
        }
        NonNullList<ItemStack> drops = NonNullList.create();
        if (BlockDrops.survives(world.rand, chance)) {
            getDrops(drops, world, pos, state, fortune);
            for (ItemStack stack : drops) BlockDrops.decay(stack, world.rand, chance);
        }
        BlockDrops.spawn(drops, world, pos, state, fortune, harvesters.get());
    }
}

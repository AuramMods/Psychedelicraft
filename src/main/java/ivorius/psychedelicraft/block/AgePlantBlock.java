/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.entity.PlantMovement;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public abstract class AgePlantBlock extends BlockBush implements IGrowable {
    private final int maxAge;

    protected AgePlantBlock(int maxAge) {
        this.maxAge = maxAge;
        setDefaultState(blockState.getBaseState().withProperty(ageProperty(), 0));
        setSoundType(SoundType.PLANT);
        setCreativeTab(null);
    }

    protected abstract PropertyInteger ageProperty();

    protected abstract AxisAlignedBB[] shapes();

    protected abstract int growthInterval();

    protected boolean hasGrowthLight(World world, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ageProperty());
    }

    @Override
    public final IBlockState getStateFromMeta(int metadata) {
        return withAge(getDefaultState(), metadata);
    }

    @Override
    public final int getMetaFromState(IBlockState state) {
        return state.getValue(ageProperty());
    }

    public final int maxAge() {
        return maxAge;
    }

    private IBlockState withAge(IBlockState state, int age) {
        return state.withProperty(ageProperty(), MathHelper.clamp(age, 0, maxAge));
    }

    @Override
    public final AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return shapes()[getMetaFromState(state)].offset(state.getOffset(world, pos));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        super.updateTick(world, pos, state, random);
        if (world.getBlockState(pos).getBlock() == this && canGrow(world, pos, state, false)
                && hasGrowthLight(world, pos)
                && ForgeHooks.onCropsGrowPre(world, pos, state, random.nextInt(growthInterval()) == 0)) {
            grow(world, random, pos, state);
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        return getMetaFromState(state) < maxAge;
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, withAge(state, getMetaFromState(state) + 1), 3);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    protected static boolean isShears(ItemStack stack) {
        return stack.getItem() instanceof ItemShears || stack.getItem().getToolClasses(stack).contains("shears");
    }

    protected final void harvest(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                 ItemStack tool, Item result, boolean shearing) {
        if (!world.isRemote) {
            if (shearing) {
                tool.damageItem(1, player);
            }
            spawnAsEntity(world, pos, new ItemStack(result, 1 + world.rand.nextInt(2)));
            world.setBlockState(pos, withAge(state, getMetaFromState(state) - 1), 2);
            if (shearing) {
                world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS,
                        1, 0.8F + world.rand.nextFloat() * 0.4F);
            }
        }
    }

    protected static void slow(Entity entity) {
        ((PlantMovement) entity).psychedelicraft$slowNextMove();
    }

    protected static AxisAlignedBB shape(double halfWidth, double height) {
        return new AxisAlignedBB((8 - halfWidth) / 16, 0, (8 - halfWidth) / 16,
                (8 + halfWidth) / 16, height / 16, (8 + halfWidth) / 16);
    }
}

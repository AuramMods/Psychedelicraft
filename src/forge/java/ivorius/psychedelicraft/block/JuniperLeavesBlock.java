/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class JuniperLeavesBlock extends Block implements IShearable {
    public static final int UNSUPPORTED = 7;
    public static final PropertyInteger DISTANCE = PropertyInteger.create("distance", 1, UNSUPPORTED);
    public static final PropertyBool PERSISTENT = PropertyBool.create("persistent");
    private static final float[] SAPLING = {0.05F, 0.0625F, 0.083333336F, 0.1F};
    private static final float[] STICKS_OR_BERRIES = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};
    private static final float[] APPLE = {0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F};
    private final boolean fruiting;

    JuniperLeavesBlock(boolean fruiting) {
        super(Material.LEAVES);
        this.fruiting = fruiting;
        setHardness(0.2F);
        setSoundType(SoundType.PLANT);
        setLightOpacity(1);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(DISTANCE, UNSUPPORTED).withProperty(PERSISTENT, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DISTANCE, PERSISTENT);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(DISTANCE, Math.min((metadata & 7) + 1, UNSUPPORTED))
                .withProperty(PERSISTENT, (metadata & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(DISTANCE) - 1) | (state.getValue(PERSISTENT) ? 8 : 0);
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos, state.withProperty(PERSISTENT, true), 2);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        beginLeavesDecay(state, world, pos);
    }

    @Override
    public void beginLeavesDecay(IBlockState state, World world, BlockPos pos) {
        if (!world.isRemote) {
            world.scheduleUpdate(pos, this, 1);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isAreaLoaded(pos, UNSUPPORTED - 1)) {
            updateDistance(world, pos, state);
        }
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (state.getValue(PERSISTENT) || !world.isAreaLoaded(pos, UNSUPPORTED - 1)) {
            return;
        }
        IBlockState updated = updateDistance(world, pos, state);
        if (updated.getValue(DISTANCE) == UNSUPPORTED) {
            dropBlockAsItem(world, pos, updated, 0);
            world.setBlockToAir(pos);
        } else if (!fruiting && random.nextFloat() < 0.01F) {
            world.setBlockState(pos, PSBlocks.FRUITING_JUNIPER_LEAVES.copyGrowth(updated), 3);
        }
    }

    private IBlockState updateDistance(World world, BlockPos pos, IBlockState state) {
        int distance = supportDistance(world, pos);
        IBlockState updated = state.withProperty(DISTANCE, distance);
        if (updated != state) {
            world.setBlockState(pos, updated, 3);
        }
        return updated;
    }

    private static int supportDistance(World world, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> frontier = new ArrayDeque<>();
        visited.add(origin);
        frontier.add(origin);
        for (int distance = 1; distance < UNSUPPORTED && !frontier.isEmpty(); distance++) {
            int currentSize = frontier.size();
            for (int i = 0; i < currentSize; i++) {
                BlockPos pos = frontier.remove();
                for (EnumFacing face : EnumFacing.VALUES) {
                    BlockPos next = pos.offset(face);
                    if (!visited.add(next)) {
                        continue;
                    }
                    IBlockState state = world.getBlockState(next);
                    Block block = state.getBlock();
                    if (block.canSustainLeaves(state, world, next)) {
                        return distance;
                    }
                    if (block.isLeaves(state, world, next)) {
                        frontier.add(next);
                    }
                }
            }
        }
        return UNSUPPORTED;
    }

    private IBlockState copyGrowth(IBlockState source) {
        return getDefaultState().withProperty(DISTANCE, source.getValue(DISTANCE))
                .withProperty(PERSISTENT, source.getValue(PERSISTENT));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        if (!fruiting) {
            return false;
        }
        if (!world.isRemote && world.setBlockState(pos, PSBlocks.JUNIPER_LEAVES.copyGrowth(state), 3)) {
            spawnAsEntity(world, pos, new ItemStack(PSItems.JUNIPER_BERRIES));
        }
        return true;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack tool, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(Item.getItemFromBlock(this)));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : RANDOM;
        if (fruiting) {
            drops.add(new ItemStack(PSItems.JUNIPER_BERRIES));
        } else if (roll(random, SAPLING, fortune)) {
            drops.add(new ItemStack(PSItems.JUNIPER_SAPLING));
        }
        if (roll(random, STICKS_OR_BERRIES, fortune)) {
            drops.add(new ItemStack(fruiting ? PSItems.JUNIPER_BERRIES : Items.STICK, 1 + random.nextInt(2)));
        }
        if (fruiting && roll(random, APPLE, fortune)) {
            drops.add(new ItemStack(Items.APPLE));
        }
    }

    private static boolean roll(Random random, float[] chances, int fortune) {
        return random.nextFloat() < chances[Math.max(0, Math.min(fortune, chances.length - 1))];
    }
}

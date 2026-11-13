/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.block.entity.LatticeCropBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public final class LatticeCropBlock extends LatticeBlock implements IGrowable {
    public static final int MAX_AGE = 3;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MAX_AGE);
    public static final PropertyBool PERSISTENT = PropertyBool.create("persistent");
    private static final int MAX_STEM_DISTANCE = 20;

    private final boolean requiresStem;
    private final int shearedAge;
    private final Supplier<Item> produce;

    LatticeCropBlock(boolean requiresStem, int shearedAge, Supplier<Item> produce) {
        this.requiresStem = requiresStem;
        this.shearedAge = shearedAge;
        this.produce = produce;
        setDefaultState(getDefaultState().withProperty(AGE, 0).withProperty(PERSISTENT, false));
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        IProperty<?>[] properties = Arrays.copyOf(LINKS, LINKS.length + 2, IProperty[].class);
        properties[LINKS.length] = AGE;
        properties[LINKS.length + 1] = PERSISTENT;
        return new BlockStateContainer(this, properties);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return withGrowth(state, getCrop(world, pos));
    }

    private static IBlockState withGrowth(IBlockState state, @Nullable LatticeCropBlockEntity tile) {
        return tile == null ? state
                : state.withProperty(AGE, tile.getAge()).withProperty(PERSISTENT, tile.isPersistent());
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new LatticeCropBlockEntity();
    }

    @Nullable
    public static LatticeCropBlockEntity getCrop(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof LatticeCropBlockEntity ? (LatticeCropBlockEntity) tile : null;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            LatticeCropBlockEntity tile = getCrop(world, pos);
            if (tile != null) {
                tile.setGrowth(stack.getMetadata() & MAX_AGE, true);
            }
        }
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return getActualState(state, world, pos).getValue(AGE) > 0 ? SoundType.PLANT : SoundType.WOOD;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        ItemStack tool = player.getHeldItem(hand);
        if (tool.getItem() != Items.SHEARS) {
            return false;
        }
        LatticeCropBlockEntity crop = getCrop(world, pos);
        if (crop == null || crop.getAge() < MAX_AGE || crop.isPersistent()) {
            return false;
        }
        if (!world.isRemote) {
            Channel.playEntitySound(player, SoundEvents.ENTITY_SHEEP_SHEAR, 1F, 1F);
            spawnAsEntity(world, pos, new ItemStack(produce.get(), 3 + world.rand.nextInt(2)));
            crop.setGrowth(shearedAge, false);
            tool.damageItem(1, player);
        }
        return true;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        LatticeCropBlockEntity crop = getCrop(world, pos);
        if (crop == null || crop.isPersistent()) {
            return;
        }
        int age = crop.getAge();
        if (!isRooted(world, state, pos)) {
            if (age > 0) {
                crop.setGrowth(age - 1, false);
            }
            return;
        }
        if (world.getLightFromNeighbors(pos) >= 9
                && ForgeHooks.onCropsGrowPre(world, pos, state, random.nextInt(35) == 0)) {
            if (age < MAX_AGE) {
                crop.setGrowth(age + 1, false);
                world.notifyNeighborsOfStateChange(pos, this, false);
            }
            if (age > 0) {
                spread(world, state, pos);
            }
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        LatticeCropBlockEntity crop = getCrop(world, pos);
        return crop != null && !crop.isPersistent()
                && (crop.getAge() < MAX_AGE || spreadTarget(world, state, pos) != null);
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        LatticeCropBlockEntity crop = getCrop(world, pos);
        if (crop != null && !crop.isPersistent()) {
            if (crop.getAge() < MAX_AGE) {
                crop.setGrowth(MAX_AGE, false);
            } else {
                spread(world, state, pos);
            }
        }
    }

    private void spread(World world, IBlockState state, BlockPos pos) {
        BlockPos target = spreadTarget(world, state, pos);
        if (target != null) {
            growOnto(world, target, this);
        }
    }

    @Nullable
    private static BlockPos spreadTarget(World world, IBlockState state, BlockPos pos) {
        for (BlockPos next : linkedPositions(state, pos)) {
            if (world.getBlockState(next).getBlock() == PSBlocks.LATTICE) {
                return next;
            }
        }
        return null;
    }

    private boolean isRooted(World world, IBlockState state, BlockPos pos) {
        if (!requiresStem) {
            return true;
        }
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> level = new ArrayDeque<>();
        visited.add(pos);
        level.add(pos);
        for (int depth = 0; depth < MAX_STEM_DISTANCE && !level.isEmpty(); depth++) {
            int count = level.size();
            for (int index = 0; index < count; index++) {
                BlockPos current = level.remove();
                IBlockState currentState = current.equals(pos) ? state : world.getBlockState(current);
                for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
                    if (!connected(currentState, direction)
                            && world.getBlockState(current.offset(direction)).getBlock() == PSBlocks.MORNING_GLORY) {
                        return true;
                    }
                }
                if (depth + 1 >= MAX_STEM_DISTANCE) {
                    continue;
                }
                for (BlockPos next : linkedPositions(currentState, current)) {
                    if (!visited.add(next)) {
                        continue;
                    }
                    Block neighbour = world.getBlockState(next).getBlock();
                    if (neighbour == PSBlocks.MORNING_GLORY) {
                        return true;
                    }
                    if (neighbour == this) {
                        level.add(next);
                    }
                }
            }
        }
        return false;
    }

    private static List<BlockPos> linkedPositions(IBlockState state, BlockPos pos) {
        List<BlockPos> positions = new ArrayList<>();
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            if (connected(state, direction)) {
                BlockPos side = pos.offset(direction);
                positions.add(side.up());
                positions.add(side);
                positions.add(side.down());
            }
        }
        return positions;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return getSilkTouchDrop(getActualState(state, world, pos));
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity tile, ItemStack tool) {
        IBlockState harvested = tile instanceof LatticeCropBlockEntity
                ? withGrowth(state, (LatticeCropBlockEntity) tile) : state;
        super.harvestBlock(world, player, pos, harvested, tile, tool);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this), 1, state.getValue(AGE));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : ThreadLocalRandom.current();
        drops.add(new ItemStack(random.nextBoolean() ? produce.get() : PSItems.LATTICE));
    }
}

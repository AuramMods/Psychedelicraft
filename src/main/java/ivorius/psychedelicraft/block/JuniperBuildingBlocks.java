package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

final class JuniperBuildingBlocks {
    private JuniperBuildingBlocks() {
    }

    static BlockFaceShape supportFace(IBlockAccess world, BlockPos pos, EnumFacing face) {
        IBlockState support = world.getBlockState(pos);
        if (support.getMaterial() == Material.LEAVES) return BlockFaceShape.UNDEFINED;
        if (support.getBlock() instanceof BlockTrapDoor && support.getValue(BlockTrapDoor.OPEN)) {
            return face == support.getValue(BlockTrapDoor.FACING).getOpposite()
                    ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
        }
        return support.getBlockFaceShape(world, pos, face);
    }

    static final class Stairs extends BlockStairs implements Waterloggable {
        Stairs(IBlockState planks) {
            super(planks);
        }

        @Override
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.CLAY;
        }
    }

    static final class Fence extends BlockFence implements Waterloggable {
        Fence() {
            super(Material.WOOD, MapColor.CLAY);
            setHardness(2);
            setResistance(5);
            setSoundType(SoundType.WOOD);
        }
    }

    static final class Gate extends BlockFenceGate {
        Gate() {
            super(BlockPlanks.EnumType.OAK);
            setHardness(2);
            setResistance(5);
            setSoundType(SoundType.WOOD);
        }

        @Override
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.CLAY;
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
        }
    }

    static final class Door extends BlockDoor {
        Door() {
            super(Material.WOOD);
            setHardness(3);
            setSoundType(SoundType.WOOD);
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            BlockPos upper = pos.up();
            return upper.getY() < world.getHeight() && hasSupport(world, pos)
                    && world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                    && world.getBlockState(upper).getBlock().isReplaceable(world, upper);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            if (world.isRemote) return;

            boolean isUpper = state.getValue(HALF) == EnumDoorHalf.UPPER;
            BlockPos lower = isUpper ? pos.down() : pos;
            BlockPos upper = lower.up();
            IBlockState lowerState = world.getBlockState(lower);
            IBlockState upperState = world.getBlockState(upper);
            if (isUpper && !isHalf(lowerState, EnumDoorHalf.LOWER)) {
                world.destroyBlock(pos, false);
            } else if (!isUpper && (!isHalf(upperState, EnumDoorHalf.UPPER) || !hasSupport(world, lower))) {
                world.destroyBlock(pos, true);
            } else if (block != this) {
                boolean receivesPower = world.isBlockPowered(lower) || world.isBlockPowered(upper);
                if (receivesPower != upperState.getValue(POWERED)) {
                    world.setBlockState(upper, upperState.withProperty(POWERED, receivesPower), 2);
                    toggleDoor(world, upper, receivesPower);
                }
            }
        }

        @Override
        public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
            // Normal removal lets the adjacent half fall through its neighbor update.
            // Suppress the lower half's loot when a creative player breaks the top.
            BlockPos lower = pos.down();
            if (player.capabilities.isCreativeMode && state.getValue(HALF) == EnumDoorHalf.UPPER
                    && isHalf(world.getBlockState(lower), EnumDoorHalf.LOWER)
                    && world.setBlockState(lower, Blocks.AIR.getDefaultState(), 2) && !world.isRemote) {
                world.notifyNeighborsOfStateExcept(lower, this, EnumFacing.UP);
            }
        }

        private static boolean hasSupport(World world, BlockPos lower) {
            return supportFace(world, lower.down(), EnumFacing.UP) == BlockFaceShape.SOLID;
        }

        private boolean isHalf(IBlockState state, EnumDoorHalf half) {
            return state.getBlock() == this && state.getValue(HALF) == half;
        }

        @Override
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.CLAY;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return state.getValue(HALF) == EnumDoorHalf.UPPER ? Items.AIR : PSItems.JUNIPER_DOOR;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(PSItems.JUNIPER_DOOR);
        }
    }

    static final class PressurePlate extends BlockPressurePlate {
        PressurePlate() {
            super(Material.WOOD, Sensitivity.EVERYTHING);
            setHardness(0.5F);
            setSoundType(SoundType.WOOD);
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            switch (supportFace(world, pos.down(), EnumFacing.UP)) {
                case SOLID:
                case BOWL:
                case CENTER_SMALL:
                case CENTER:
                case CENTER_BIG:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            if (!world.isRemote && from.equals(pos.down()) && !canPlaceBlockAt(world, pos)) {
                world.destroyBlock(pos, true);
            }
        }

        @Override
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.CLAY;
        }
    }
}

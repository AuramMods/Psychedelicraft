package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

final class JuniperBuildingBlocks {
    private JuniperBuildingBlocks() {
    }

    static final class Stairs extends BlockStairs implements Waterloggable {
        Stairs(IBlockState planks) {
            super(planks);
        }

        @Override
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.SILVER;
        }
    }

    static final class Fence extends BlockFence implements Waterloggable {
        Fence() {
            super(Material.WOOD, MapColor.SILVER);
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
            return MapColor.SILVER;
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
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.SILVER;
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
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.SILVER;
        }
    }
}

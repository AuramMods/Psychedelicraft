package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

final class JuniperTrapDoorBlock extends BlockTrapDoor {
    // The native facing, half and open properties already occupy all four metadata bits.
    private final boolean powered;

    JuniperTrapDoorBlock(boolean powered) {
        super(Material.WOOD);
        this.powered = powered;
        setHardness(3);
        setSoundType(SoundType.WOOD);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && world.isBlockPowered(pos)) {
            setPower(world, pos, state, true);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!world.isRemote) {
            boolean receivesPower = world.isBlockPowered(pos);
            if (receivesPower != powered && setPower(world, pos, state, receivesPower)
                    && state.getValue(OPEN) != receivesPower) {
                playSound(null, world, pos, receivesPower);
            }
        }
    }

    private static boolean setPower(World world, BlockPos pos, IBlockState state, boolean powered) {
        Block target = powered ? PSBlocks.POWERED_JUNIPER_TRAPDOOR : PSBlocks.JUNIPER_TRAPDOOR;
        return world.setBlockState(pos, target.getDefaultState().withProperty(FACING, state.getValue(FACING))
                .withProperty(HALF, state.getValue(HALF)).withProperty(OPEN, powered), 2);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.SILVER;
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos,
                                    EntityLiving.SpawnPlacementType placement) {
        return false;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return PSItems.JUNIPER_TRAPDOOR;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(PSItems.JUNIPER_TRAPDOOR);
    }
}

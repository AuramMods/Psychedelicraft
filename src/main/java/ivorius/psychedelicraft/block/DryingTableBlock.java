/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import javax.annotation.Nullable;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class DryingTableBlock extends BlockContainer {
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0, 0, 0, 1, 0.75, 1);

    DryingTableBlock(Material material) {
        super(material);
        boolean wood = material == Material.WOOD;
        setHardness(wood ? 2 : 5);
        setSoundType(wood ? SoundType.WOOD : SoundType.METAL);
        setLightOpacity(0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new DryingTableBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
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
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (getTable(world, pos) == null) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(Psychedelicraft.MOD_ID, PSGuiHandler.DRYING_TABLE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        DryingTableBlockEntity table = getTable(world, pos);
        if (!world.isRemote && table != null) {
            InventoryHelper.dropInventoryItems(world, pos, table);
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        DryingTableBlockEntity table = getTable(world, pos);
        return table == null ? 0 : (int) (table.getHeatRatio() * 15);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        DryingTableBlockEntity table = getTable(world, pos);
        return table == null ? 0 : (int) (table.getDryingProgress() * 15);
    }

    @Nullable
    public static DryingTableBlockEntity getTable(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof DryingTableBlockEntity ? (DryingTableBlockEntity) tile : null;
    }
}

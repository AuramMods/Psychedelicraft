/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public final class BarrelBlock extends FluidMachineBlock {
    public static final PropertyDirection FACING = BlockHopper.FACING;
    public static final PropertyBool TAPPED = PropertyBool.create("tapped");
    private static final AxisAlignedBB[][] SHAPES = {
            {box(0, 5, 2, 16, 13, 14), box(0, 3, 4, 16, 15, 12)},
            {box(2, 0, 4, 14, 16, 12), box(4, 0, 2, 12, 16, 14)},
            {box(2, 5, 0, 14, 13, 16), box(4, 3, 0, 12, 15, 16)}
    };

    BarrelBlock() {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TAPPED, true));
        setHardness(2);
        setSoundType(SoundType.WOOD);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, TAPPED);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new BarrelBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        EnumFacing facing = EnumFacing.getFront(metadata & 7);
        if (facing == EnumFacing.UP) facing = EnumFacing.DOWN;
        return getDefaultState().withProperty(FACING, facing).withProperty(TAPPED, (metadata & 8) == 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(TAPPED) ? 0 : 8);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
                                           int metadata, EntityLivingBase placer, EnumHand hand) {
        EnumFacing facing = EnumFacing.getDirectionFromEntityLiving(pos, placer);
        return getDefaultState().withProperty(FACING, facing.getAxis().isVertical() ? EnumFacing.DOWN : facing);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        AxisAlignedBB[] parts = SHAPES[state.getValue(FACING).getAxis().ordinal()];
        return parts[0].union(parts[1]);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB query,
                                     List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean actualState) {
        for (AxisAlignedBB part : SHAPES[state.getValue(FACING).getAxis().ordinal()]) {
            addCollisionBoxToList(pos, query, boxes, part);
        }
    }

    private static AxisAlignedBB box(int x, int y, int z, int maxX, int maxY, int maxZ) {
        return new AxisAlignedBB(x / 16.0, y / 16.0, z / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        BarrelBlockEntity barrel = getBarrel(world, pos);
        if (barrel == null) return false;
        if (!world.isRemote) {
            if (state.getValue(TAPPED) && state.getValue(FACING).getAxis().isHorizontal()
                    && barrel.dispense(player, hand)) return true;
            if (player.getHeldItem(hand).isEmpty() && barrel.collectSolidOutput(player)) return true;
            openMachine(world, pos, player, PSGuiHandler.BARREL);
        }
        return true;
    }

    @Nullable
    public static BarrelBlockEntity getBarrel(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof BarrelBlockEntity && !tile.isInvalid() ? (BarrelBlockEntity) tile : null;
    }
}

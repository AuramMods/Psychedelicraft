/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

class LabPropBlock extends Block {
    private static final Material MATERIAL = new Material(MapColor.IRON);
    private static final AxisAlignedBB BURNER_BOUNDS = centeredBounds(5, 5);

    private final AxisAlignedBB bounds;

    private LabPropBlock(AxisAlignedBB bounds) {
        super(MATERIAL);
        this.bounds = bounds;
        setSoundType(SoundType.METAL);
        setHardness(0.7F);
        setLightOpacity(0);
    }

    static Block burner() {
        return new LabPropBlock(BURNER_BOUNDS);
    }

    private static AxisAlignedBB centeredBounds(int halfX, int halfZ) {
        return new AxisAlignedBB((8 - halfX) / 16D, 0, (8 - halfZ) / 16D,
                (8 + halfX) / 16D, 2 / 16D, (8 + halfZ) / 16D);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return bounds;
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
    public int quantityDropped(Random random) {
        // The 1.20.1 props have no loot tables.
        return 0;
    }

    static final class Tray extends LabPropBlock {
        static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class,
                EnumFacing.Axis.X, EnumFacing.Axis.Z);
        private static final AxisAlignedBB X_BOUNDS = centeredBounds(9, 6);
        private static final AxisAlignedBB Z_BOUNDS = centeredBounds(6, 9);

        Tray() {
            super(X_BOUNDS);
            setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, AXIS);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(AXIS) == EnumFacing.Axis.X ? 0 : 1;
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
                                                 int meta, EntityLivingBase placer, EnumHand hand) {
            return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().rotateY().getAxis());
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(AXIS) == EnumFacing.Axis.Z ? Z_BOUNDS : super.getBoundingBox(state, world, pos);
        }
    }
}

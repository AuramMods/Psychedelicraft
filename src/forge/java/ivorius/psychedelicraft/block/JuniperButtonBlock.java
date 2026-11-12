package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

final class JuniperButtonBlock extends BlockButtonWood {
    private static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create(
            "axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

    JuniperButtonBlock() {
        setDefaultState(getDefaultState().withProperty(AXIS, EnumFacing.Axis.Z));
        setHardness(0.5F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED, AXIS);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float x, float y, float z,
                                           int meta, EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, x, y, z, meta, placer);
        if (state.getValue(FACING).getAxis().isVertical()) {
            return state.withProperty(AXIS, placer.getHorizontalFacing().getAxis());
        }
        return state;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = super.getStateFromMeta(meta);
        int attachment = meta & 7;
        if (attachment >= 6) {
            return state.withProperty(FACING, attachment == 6 ? EnumFacing.DOWN : EnumFacing.UP)
                    .withProperty(AXIS, EnumFacing.Axis.X);
        }
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        // The two unused attachment values store the alternate floor and ceiling orientation.
        if (facing.getAxis().isVertical() && state.getValue(AXIS) == EnumFacing.Axis.X) {
            int attachment = facing == EnumFacing.DOWN ? 6 : 7;
            return attachment | (state.getValue(POWERED) ? 8 : 0);
        }
        return super.getMetaFromState(state);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        IBlockState rotated = super.withRotation(state, rotation);
        if (state.getValue(FACING).getAxis().isVertical()) {
            EnumFacing direction = state.getValue(AXIS) == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.NORTH;
            return rotated.withProperty(AXIS, rotation.rotate(direction).getAxis());
        }
        return rotated;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        AxisAlignedBB box = super.getBoundingBox(state, world, pos);
        if (state.getValue(FACING).getAxis().isVertical() && state.getValue(AXIS) == EnumFacing.Axis.X) {
            return new AxisAlignedBB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX);
        }
        return box;
    }
}

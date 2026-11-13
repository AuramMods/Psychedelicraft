package ivorius.psychedelicraft.block;

import java.util.Arrays;
import java.util.Comparator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.VALUES) {
            if (hasSupport(world, pos, face)) return true;
        }
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return canPlaceBlockAt(world, pos);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float x, float y, float z,
                                           int meta, EntityLivingBase placer) {
        EnumFacing attachment = facing;
        if (!hasSupport(world, pos, facing)) {
            Vec3d look = placer.getLookVec();
            EnumFacing[] faces = EnumFacing.VALUES.clone();
            Arrays.sort(faces, Comparator.comparingDouble(face -> look.x * face.getFrontOffsetX()
                    + look.y * face.getFrontOffsetY() + look.z * face.getFrontOffsetZ()));
            for (EnumFacing face : faces) {
                if (hasSupport(world, pos, face)) {
                    attachment = face;
                    break;
                }
            }
        }
        EnumFacing.Axis axis = attachment.getAxis().isVertical()
                ? placer.getHorizontalFacing().getAxis() : EnumFacing.Axis.Z;
        return getDefaultState().withProperty(FACING, attachment).withProperty(AXIS, axis);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        EnumFacing face = state.getValue(FACING);
        if (!world.isRemote && from.equals(pos.offset(face.getOpposite())) && !hasSupport(world, pos, face)) {
            world.destroyBlock(pos, true);
        }
    }

    private static boolean hasSupport(World world, BlockPos pos, EnumFacing face) {
        return JuniperBuildingBlocks.supportFace(world, pos.offset(face.getOpposite()), face) == BlockFaceShape.SOLID;
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

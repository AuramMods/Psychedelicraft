package ivorius.psychedelicraft.block;

import javax.annotation.Nullable;

import ivorius.psychedelicraft.block.JuniperSignBlock.Form;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class JuniperHangingSigns {
    private static final AxisAlignedBB BOARD_X = box(1, 0, 7, 15, 10, 9);
    private static final AxisAlignedBB BOARD_Z = box(7, 0, 1, 9, 10, 15);
    private static final AxisAlignedBB BEAM_X = box(0, 14, 6, 16, 16, 10);
    private static final AxisAlignedBB BEAM_Z = box(6, 14, 0, 10, 16, 16);
    private static final AxisAlignedBB WALL_X = BOARD_X.union(BEAM_X);
    private static final AxisAlignedBB WALL_Z = BOARD_Z.union(BEAM_Z);
    private static final AxisAlignedBB DIAGONAL = box(3, 0, 3, 13, 16, 13);

    private JuniperHangingSigns() {
    }

    private static AxisAlignedBB box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new AxisAlignedBB(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    private static boolean spansBottom(AxisAlignedBB box, double minimum, double maximum) {
        return box != null && box.minY <= 0 && box.minX <= minimum && box.minZ <= minimum
                && box.maxX >= maximum && box.maxZ >= maximum;
    }

    private static boolean isHanging(IBlockState state) {
        return state.getBlock() instanceof JuniperSignBlock && ((JuniperSignBlock) state.getBlock()).form().isHanging();
    }

    public static boolean usesAttachedChains(World world, BlockPos pos, EntityPlayer placer) {
        if (placer.isSneaking()) {
            return true;
        }
        BlockPos above = pos.up();
        IBlockState support = world.getBlockState(above);
        EnumFacing.Axis axis = placer.getHorizontalFacing().getAxis();
        if (support.getBlock() == PSBlocks.JUNIPER_WALL_HANGING_SIGN) {
            return support.getValue(JuniperSignBlock.FACING).getAxis() != axis;
        }
        if (support.getBlock() == PSBlocks.JUNIPER_HANGING_SIGN) {
            int rotation = support.getValue(JuniperSignBlock.ROTATION);
            return (rotation & 3) != 0 || EnumFacing.getHorizontal(rotation / 4).getAxis() != axis;
        }
        return support.getBlockFaceShape(world, above, EnumFacing.DOWN) != BlockFaceShape.SOLID
                && !spansBottom(support.getCollisionBoundingBox(world, above), 0, 1);
    }

    public static int placementRotation(EntityPlayer placer, boolean attached) {
        return attached ? MathHelper.floor((placer.rotationYaw + 180F) * 16F / 360F + 0.5F) & 15
                : placer.getHorizontalFacing().getOpposite().getHorizontalIndex() * 4;
    }

    static final class Ceiling extends JuniperSignBlock.Rotating {
        Ceiling() {
            super(Form.HANGING_CEILING);
        }

        @Override
        public boolean canStay(World world, BlockPos pos, IBlockState state) {
            BlockPos above = pos.up();
            IBlockState support = world.getBlockState(above);
            BlockFaceShape face = support.getBlockFaceShape(world, above, EnumFacing.DOWN);
            return isHanging(support) || face == BlockFaceShape.SOLID || face == BlockFaceShape.CENTER
                    || face == BlockFaceShape.CENTER_BIG || face == BlockFaceShape.CENTER_SMALL
                    || spansBottom(support.getCollisionBoundingBox(world, above), 7 / 16.0, 9 / 16.0);
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            int rotation = state.getValue(ROTATION);
            return (rotation & 3) != 0 ? DIAGONAL : (rotation & 4) == 0 ? BOARD_X : BOARD_Z;
        }
    }

    static final class Wall extends JuniperSignBlock.Horizontal {
        Wall() {
            super(Form.HANGING_WALL);
        }

        @Override
        public boolean canStay(World world, BlockPos pos, IBlockState state) {
            EnumFacing lateral = state.getValue(FACING).rotateY();
            return canAttachTo(world, pos, lateral) || canAttachTo(world, pos, lateral.getOpposite());
        }

        private boolean canAttachTo(World world, BlockPos pos, EnumFacing side) {
            BlockPos neighbor = pos.offset(side);
            IBlockState support = world.getBlockState(neighbor);
            if (support.getBlock() == this) {
                return support.getValue(FACING).getAxis() != side.getAxis();
            }
            return support.getBlockFaceShape(world, neighbor, side.getOpposite()) == BlockFaceShape.SOLID;
        }

        private static boolean acrossX(IBlockState state) {
            return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z;
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            return acrossX(state) ? WALL_X : WALL_Z;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            return acrossX(state) ? BEAM_X : BEAM_Z;
        }

        @Override
        public boolean isPassable(IBlockAccess world, BlockPos pos) {
            return false;
        }

        @Nullable
        @Override
        public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
            RayTraceResult board = rayTrace(pos, start, end, acrossX(state) ? BOARD_X : BOARD_Z);
            RayTraceResult beam = rayTrace(pos, start, end, getCollisionBoundingBox(state, world, pos));
            if (board == null) {
                return beam;
            }
            if (beam == null) {
                return board;
            }
            return start.squareDistanceTo(board.hitVec) < start.squareDistanceTo(beam.hitVec) ? board : beam;
        }
    }
}

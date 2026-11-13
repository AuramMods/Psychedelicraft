/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.block.entity.WaterloggedBlockEntity;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LatticeBlock extends Block {
    // The property order is also the four-bit metadata layout.
    static final PropertyBool[] LINKS = {
            PropertyBool.create("south"), PropertyBool.create("west"),
            PropertyBool.create("north"), PropertyBool.create("east")
    };
    private static final EnumFacing[] HORIZONTAL = {
            EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST
    };
    private static final int NORTH_SOUTH = bit(EnumFacing.NORTH) | bit(EnumFacing.SOUTH);
    private static final int EAST_WEST = bit(EnumFacing.EAST) | bit(EnumFacing.WEST);
    private static final AxisAlignedBB CENTER = new AxisAlignedBB(0.4, 0, 0.4, 0.6, 1, 0.6);
    private static final AxisAlignedBB[] ARMS = {
            new AxisAlignedBB(0.4, 0, 0.6, 0.6, 1, 1),
            new AxisAlignedBB(0, 0, 0.4, 0.4, 1, 0.6),
            new AxisAlignedBB(0.4, 0, 0, 0.6, 1, 0.4),
            new AxisAlignedBB(0.6, 0, 0.4, 1, 1, 0.6)
    };

    LatticeBlock() {
        super(Material.WOOD);
        setDefaultState(withConnections(blockState.getBaseState(), EAST_WEST));
        setHardness(0.3F);
        setSoundType(SoundType.WOOD);
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LINKS);
    }

    @Override
    public final IBlockState getStateFromMeta(int meta) {
        return withConnections(getDefaultState(), meta);
    }

    @Override
    public final int getMetaFromState(IBlockState state) {
        return connections(state);
    }

    private static int bit(EnumFacing face) {
        return 1 << face.getHorizontalIndex();
    }

    static boolean connected(IBlockState state, EnumFacing face) {
        return state.getValue(LINKS[face.getHorizontalIndex()]);
    }

    public static int connections(IBlockState state) {
        int mask = 0;
        for (EnumFacing face : HORIZONTAL) {
            if (connected(state, face)) {
                mask |= bit(face);
            }
        }
        return mask;
    }

    static IBlockState withConnections(IBlockState state, int mask) {
        for (EnumFacing face : HORIZONTAL) {
            state = state.withProperty(LINKS[face.getHorizontalIndex()], (mask & bit(face)) != 0);
        }
        return state;
    }

    public static boolean growOnto(World world, BlockPos pos, LatticeCropBlock crop) {
        IBlockState previous = world.getBlockState(pos);
        if (previous.getBlock() != PSBlocks.LATTICE) return false;
        boolean wet = Waterlogging.isWet(world, pos, previous);
        if (!world.setBlockState(pos, withConnections(crop.getDefaultState(), connections(previous)), 3)) return false;
        if (wet) Waterlogging.fillSource(world, pos);
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new WaterloggedBlockEntity();
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (Waterlogging.isWet(world, pos, state)) {
            Blocks.FLOWING_WATER.updateTick(world, pos, Blocks.FLOWING_WATER.getDefaultState(), random);
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (!Waterlogging.isWet(world, pos, state)) return super.removedByPlayer(state, world, pos, player, willHarvest);
        onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState(), world.isRemote ? 11 : 3);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if (Waterlogging.isWet(world, pos)) {
            world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState(), 3);
            onBlockDestroyedByExplosion(world, pos, explosion);
        } else {
            super.onBlockExploded(world, pos, explosion);
        }
    }

    @Override
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos, IBlockState state, Entity entity,
                                          double yToTest, Material material, boolean testingHead) {
        if (material != Material.WATER || !Waterlogging.isWet(world, pos, state)) return null;
        return yToTest >= pos.getY() && (!testingHead || yToTest < pos.getY() + 1);
    }

    @Override
    public Boolean isAABBInsideMaterial(World world, BlockPos pos, AxisAlignedBB box, Material material) {
        return material == Material.WATER ? isAABBInsideLiquid(world, pos, box) : null;
    }

    @Override
    public Boolean isAABBInsideLiquid(World world, BlockPos pos, AxisAlignedBB box) {
        return Waterlogging.isWet(world, pos) ? box.intersects(new AxisAlignedBB(pos)) : null;
    }

    @Override
    public float getBlockLiquidHeight(World world, BlockPos pos, IBlockState state, Material material) {
        return material == Material.WATER && Waterlogging.isWet(world, pos, state) ? 1 : 0;
    }

    @Override
    public Vec3d modifyAcceleration(World world, BlockPos pos, Entity entity, Vec3d motion) {
        return Waterlogging.isWet(world, pos) ? Blocks.WATER.modifyAcceleration(world, pos, entity, motion) : motion;
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return !Waterlogging.isWet(world, pos) && super.isFlammable(world, pos, face);
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return Waterlogging.isWet(world, pos) ? 0 : super.getFlammability(world, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return Waterlogging.isWet(world, pos) ? 0 : super.getFireSpreadSpeed(world, pos, face);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
                                           int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState against = world.getBlockState(pos.offset(side.getOpposite()));
        int mask = side.getAxis().isVertical() && against.getBlock() instanceof LatticeBlock
                ? connections(against) : possibleConnections(world, pos);
        if (mask == 0) {
            mask = placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.Z ? EAST_WEST : NORTH_SOUTH;
        }
        return withConnections(getDefaultState(), mask);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!world.isRemote && Waterlogging.isWet(world, pos, state)) Waterlogging.scheduleFlow(world, pos);
        if (!world.isRemote && pos.getY() == from.getY()) {
            int mask = possibleConnections(world, pos);
            if (mask == 0) {
                mask = (connections(state) & NORTH_SOUTH) != 0 ? NORTH_SOUTH : EAST_WEST;
            }
            IBlockState updated = withConnections(state, mask);
            if (updated != state) {
                world.setBlockState(pos, updated, 3);
            }
        }
    }

    private static int possibleConnections(IBlockAccess world, BlockPos pos) {
        int mask = 0;
        for (EnumFacing face : HORIZONTAL) {
            BlockPos nextPos = pos.offset(face);
            IBlockState next = world.getBlockState(nextPos);
            if (next.getBlock() instanceof LatticeBlock || (canAttach(world, nextPos, next, face.getOpposite())
                    && canAttach(world, pos.offset(face.getOpposite()), world.getBlockState(pos.offset(face.getOpposite())), face))) {
                mask |= bit(face);
            }
        }
        return mask;
    }

    private static boolean canAttach(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing face) {
        Block block = state.getBlock();
        BlockFaceShape shape = state.getBlockFaceShape(world, pos, face);
        if (block instanceof LatticeBlock || (block instanceof BlockFenceGate && shape == BlockFaceShape.MIDDLE_POLE)) {
            return true;
        }
        return shape == BlockFaceShape.SOLID && !(block instanceof BlockLeaves) && !(block instanceof BlockShulkerBox)
                && block != Blocks.BARRIER && block != Blocks.PUMPKIN && block != Blocks.LIT_PUMPKIN && block != Blocks.MELON_BLOCK;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        int mask = 0;
        for (EnumFacing face : HORIZONTAL) {
            if (connected(state, face)) {
                mask |= bit(rotation.rotate(face));
            }
        }
        return withConnections(state, mask);
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        int mask = 0;
        for (EnumFacing face : HORIZONTAL) {
            if (connected(state, face)) {
                mask |= bit(mirror.mirror(face));
            }
        }
        return withConnections(state, mask);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        if (face.getAxis().isVertical()) {
            return BlockFaceShape.CENTER_SMALL;
        }
        return connected(state, face) ? BlockFaceShape.MIDDLE_POLE_THIN : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        AxisAlignedBB box = CENTER;
        for (EnumFacing face : HORIZONTAL) {
            if (connected(state, face)) {
                box = box.union(ARMS[face.getHorizontalIndex()]);
            }
        }
        return box;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB bounds,
                                      List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean actualState) {
        addCollisionBoxToList(pos, bounds, boxes, CENTER);
        for (EnumFacing face : HORIZONTAL) {
            if (connected(state, face)) {
                addCollisionBoxToList(pos, bounds, boxes, ARMS[face.getHorizontalIndex()]);
            }
        }
    }

    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult closest = rayTrace(pos, start, end, CENTER);
        for (EnumFacing face : HORIZONTAL) {
            if (connected(state, face)) {
                RayTraceResult hit = rayTrace(pos, start, end, ARMS[face.getHorizontalIndex()]);
                if (hit != null && (closest == null || hit.hitVec.squareDistanceTo(start) < closest.hitVec.squareDistanceTo(start))) {
                    closest = hit;
                }
            }
        }
        return closest;
    }
}

/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.BottleRackBlockEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

public final class BottleRackBlock extends BlockHorizontal {
    private static final AxisAlignedBB[] NORTH_PARTS = {
            bounds(0, 0, 5, 1, 16, 13), bounds(5, 0, 5, 6, 16, 13),
            bounds(10, 0, 5, 11, 16, 13), bounds(15, 0, 5, 16, 16, 13),
            bounds(1, 0, 3.7, 15, 0.9, 13.2), bounds(1, 5, 3.7, 15, 6, 13.2),
            bounds(1, 10, 3.7, 15, 11, 13.2), bounds(1, 15.1, 3.7, 15, 16, 13.2)
    };
    private static final AxisAlignedBB[][] PARTS = new AxisAlignedBB[4][NORTH_PARTS.length];

    static {
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            int rotations = (face.getHorizontalIndex() + 2) % 4;
            AxisAlignedBB[] parts = PARTS[face.getHorizontalIndex()];
            for (int part = 0; part < NORTH_PARTS.length; part++) {
                AxisAlignedBB box = NORTH_PARTS[part];
                for (int turn = 0; turn < rotations; turn++) {
                    box = new AxisAlignedBB(1 - box.maxZ, box.minY, box.minX,
                            1 - box.minZ, box.maxY, box.maxX);
                }
                parts[part] = box;
            }
        }
    }

    public BottleRackBlock() {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        setHardness(0.5F);
        setSoundType(SoundType.WOOD);
        setLightOpacity(0);
    }

    private static AxisAlignedBB bounds(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }

    public static int slotAt(EnumFacing facing, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (side != facing && side.getAxis().isHorizontal()) return -1;
        float horizontal;
        switch (facing) {
            case NORTH: horizontal = 1 - hitX; break;
            case SOUTH: horizontal = hitX; break;
            case WEST: horizontal = hitZ; break;
            case EAST: horizontal = 1 - hitZ; break;
            default: return -1;
        }
        int column = MathHelper.clamp(MathHelper.floor(horizontal * 3), 0, 2);
        int row = MathHelper.clamp(MathHelper.floor((1 - hitY) * 3), 0, 2);
        return column + 3 * row;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        int slot = slotAt(state.getValue(FACING), side, hitX, hitY, hitZ);
        TileEntity tile = world.getTileEntity(pos);
        if (slot < 0 || !(tile instanceof BottleRackBlockEntity) || !world.isBlockModifiable(player, pos)) return false;
        ItemStackHandler bottles = ((BottleRackBlockEntity) tile).getBottles();
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) {
            ItemStack bottle = bottles.extractItem(slot, 1, world.isRemote);
            if (bottle.isEmpty()) return false;
            if (!world.isRemote) player.setHeldItem(hand, bottle);
        } else {
            ItemStack remainder = bottles.insertItem(slot, held.copy(), world.isRemote);
            if (remainder.getCount() == held.getCount()) return false;
            if (!world.isRemote && !player.capabilities.isCreativeMode) player.setHeldItem(hand, remainder);
        }
        if (!world.isRemote) {
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.65F, held.isEmpty() ? 1 : 1.5F);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof BottleRackBlockEntity) ((BottleRackBlockEntity) tile).dropContents();
        super.breakBlock(world, pos, state);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z
                ? bounds(0, 0, 2.8, 16, 16, 13.2) : bounds(2.8, 0, 0, 13.2, 16, 16);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collisionBoxes, @Nullable Entity entity, boolean actual) {
        for (AxisAlignedBB box : PARTS[state.getValue(FACING).getHorizontalIndex()]) {
            addCollisionBoxToList(pos, entityBox, collisionBoxes, box);
        }
    }

    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult closest = null;
        for (AxisAlignedBB box : PARTS[state.getValue(FACING).getHorizontalIndex()]) {
            RayTraceResult hit = rayTrace(pos, start, end, box);
            if (hit != null && (closest == null || hit.hitVec.squareDistanceTo(start) < closest.hitVec.squareDistanceTo(start))) {
                closest = hit;
            }
        }
        return closest;
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new BottleRackBlockEntity();
    }
}

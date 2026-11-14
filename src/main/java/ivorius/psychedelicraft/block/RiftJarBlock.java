/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import com.google.common.collect.ImmutableList;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.RiftJarItem;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public final class RiftJarBlock extends BlockContainer {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static final AxisAlignedBB BOUNDS = box(4, 0, 12, 14);
    public static final List<AxisAlignedBB> SHAPE = ImmutableList.of(
            box(4, 0, 12, 5), box(4.5, 5, 11.5, 7), box(4, 7, 12, 12), box(5, 12, 11, 14));
    private static final double RAY_EPSILON = 1.0E-7;

    RiftJarBlock() {
        super(Material.GLASS);
        setHardness(0.5F);
        setSoundType(SoundType.GLASS);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    private static AxisAlignedBB box(double min, int bottom, double max, int top) {
        return new AxisAlignedBB(min / 16, bottom / 16D, min / 16, max / 16, top / 16D, max / 16);
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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing face, float hitX, float hitY,
                                           float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
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
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BOUNDS;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB box,
                                      List<AxisAlignedBB> collisions, @Nullable Entity entity, boolean actualState) {
        for (AxisAlignedBB part : SHAPE) addCollisionBoxToList(pos, box, collisions, part);
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < RAY_EPSILON) return null;

        Vec3d local = start.subtract(new Vec3d(pos));
        Vec3d probe = local.add(direction.scale(0.001));
        double nearest = 1;
        EnumFacing face = null;
        for (AxisAlignedBB part : SHAPE) {
            if (probe.y >= part.minY && probe.y < part.maxY
                    && probe.x >= part.minX && probe.x < part.maxX && probe.z >= part.minZ && probe.z < part.maxZ) {
                EnumFacing entry = EnumFacing.getFacingFromVector((float) direction.x, (float) direction.y,
                        (float) direction.z).getOpposite();
                return new RayTraceResult(start.add(direction.scale(0.001)), entry, pos);
            }
            double x = entryTime(local.x, direction.x, part.minX, part.maxX);
            double y = entryTime(local.y, direction.y, part.minY, part.maxY);
            double z = entryTime(local.z, direction.z, part.minZ, part.maxZ);
            if (x > 0 && x < nearest && within(local.y + x * direction.y, part.minY, part.maxY)
                    && within(local.z + x * direction.z, part.minZ, part.maxZ)) {
                nearest = x;
                face = direction.x > 0 ? EnumFacing.WEST : EnumFacing.EAST;
            }
            if (y > 0 && y < nearest && within(local.x + y * direction.x, part.minX, part.maxX)
                    && within(local.z + y * direction.z, part.minZ, part.maxZ)) {
                nearest = y;
                face = direction.y > 0 ? EnumFacing.DOWN : EnumFacing.UP;
            }
            if (z > 0 && z < nearest && within(local.x + z * direction.x, part.minX, part.maxX)
                    && within(local.y + z * direction.y, part.minY, part.maxY)) {
                nearest = z;
                face = direction.z > 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
            }
        }
        return face == null ? null : new RayTraceResult(start.add(direction.scale(nearest)), face, pos);
    }

    private static double entryTime(double start, double direction, double min, double max) {
        if (Math.abs(direction) <= RAY_EPSILON) return -1;
        return ((direction > 0 ? min : max) - start) / direction;
    }

    private static boolean within(double value, double min, double max) {
        return min - RAY_EPSILON < value && value < max + RAY_EPSILON;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new RiftJarBlockEntity();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof RiftJarBlockEntity) {
            ((RiftJarBlockEntity) tile).setAmount(RiftJarItem.getRiftFraction(stack));
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing face, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof RiftJarBlockEntity)) return false;
        if (!world.isRemote) {
            RiftJarBlockEntity jar = (RiftJarBlockEntity) tile;
            SoundEvent sound;
            if (player.isSneaking()) {
                jar.toggleMode();
                sound = PSSounds.RIFT_JAR_TOGGLE;
            } else {
                sound = jar.toggleLid() ? PSSounds.RIFT_JAR_OPEN : PSSounds.RIFT_JAR_CLOSE;
            }
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1, 1);
        }
        return true;
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        // The filled jar comes directly from its tile, outside the explosion loot pool.
        super.dropBlockAsItemWithChance(world, pos, state, 1F, fortune);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof RiftJarBlockEntity && !tile.isInvalid()) {
            drops.add(PSItems.RIFT_JAR.filled(((RiftJarBlockEntity) tile).getAmount()));
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity tile, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, tile, tool);
        world.setBlockToAir(pos);
    }
}

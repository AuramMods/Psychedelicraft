/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.client.particle.SlurryBubble;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.physical.WorldFluid;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public final class MashTubBlock extends BlockContainer {
    private static final int CENTER = 4;
    private static final double INNER_EDGE = 0.4375;
    public static final PropertyInteger OFFSET = PropertyInteger.create("offset", 0, 8);
    private static final AxisAlignedBB[] BASIN = {
            new AxisAlignedBB(-0.5, 0, -0.5, 1.5, 0.0625, 1.5),
            new AxisAlignedBB(-0.5, 0, -0.5, 1.5, 1, -0.4375),
            new AxisAlignedBB(-0.5, 0, 1.4375, 1.5, 1, 1.5),
            new AxisAlignedBB(-0.5, 0, -0.4375, -0.4375, 1, 1.4375),
            new AxisAlignedBB(1.4375, 0, -0.4375, 1.5, 1, 1.4375)
    };
    private final boolean edge;

    MashTubBlock(boolean edge) {
        super(Material.WOOD);
        this.edge = edge;
        setDefaultState(blockState.getBaseState().withProperty(OFFSET, CENTER));
        setHardness(2);
        setSoundType(SoundType.WOOD);
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, OFFSET);
    }

    public IBlockState edgeState(int dx, int dz) {
        return getDefaultState().withProperty(OFFSET, (dx + 1) * 3 + dz + 1);
    }

    public static BlockPos center(BlockPos pos, IBlockState state) {
        int offset = state.getValue(OFFSET);
        return pos.add(1 - offset / 3, 0, 1 - offset % 3);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(OFFSET, edge && meta >= 0 && meta <= 8 ? meta : CENTER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(OFFSET);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return edge ? new Edge() : new MashTubBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return edge ? EnumBlockRenderType.INVISIBLE : EnumBlockRenderType.MODEL;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
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
        return !edge && face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        MashTubBlockEntity vat = getVat(world, pos);
        FluidStack fluid = vat == null ? null : vat.getTank().getFluid();
        return fluid == null ? 0 : fluid.getFluid().getLuminosity(fluid);
    }

    public static boolean containsBody(World world, Entity entity, Material material) {
        if (material != Material.WATER && material != Material.LAVA) return false;
        AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.001D);
        BlockPos min = new BlockPos(box.minX, box.minY, box.minZ);
        BlockPos max = new BlockPos(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            if (!world.isBlockLoaded(pos) || world.getBlockState(pos).getBlock() != PSBlocks.MASH_TUB) continue;
            MashTubBlockEntity vat = getVat(world, pos);
            if (vat == null) continue;
            FluidStack fluid = vat.getTank().getFluid();
            // Body contact starts at the first of the donor's eight fill levels.
            if (fluid == null || fluid.amount * 8L < vat.getTank().getCapacity()) continue;
            if (material == Material.LAVA ? fluid.getFluid() == FluidRegistry.LAVA
                    : fluid.getFluid() == FluidRegistry.WATER || fluid.getFluid() instanceof WorldFluid) return true;
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        MashTubBlockEntity vat = getVat(world, pos);
        FluidStack fluid = vat == null ? null : vat.getTank().getFluid();
        if (fluid == null) return;
        if (fluid.getFluid() == PSFluids.SLURRY) {
            BlockPos center = vat.getPos();
            double x = Math.max(center.getX() - INNER_EDGE + 0.25,
                    Math.min(center.getX() + 1 + INNER_EDGE - 0.25, pos.getX() + 0.5));
            double z = Math.max(center.getZ() - INNER_EDGE + 0.25,
                    Math.min(center.getZ() + 1 + INNER_EDGE - 0.25, pos.getZ() + 0.5));
            SlurryBubble.ambient(world, new Vec3d(x, pos.getY() + vat.getFluidSurface(), z),
                    fluid.getFluid().getColor(fluid), 0.25, random);
        } else if (fluid.getFluid() == FluidRegistry.WATER) {
            Blocks.WATER.randomDisplayTick(Blocks.WATER.getDefaultState(), world, pos, random);
        } else if (fluid.getFluid() == FluidRegistry.LAVA) {
            Blocks.LAVA.randomDisplayTick(Blocks.LAVA.getDefaultState(), world, pos, random);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        BlockPos center = center(pos, state);
        int dx = center.getX() - pos.getX();
        int dz = center.getZ() - pos.getZ();
        return new AxisAlignedBB(Math.max(0, dx - 0.5), 0, Math.max(0, dz - 0.5),
                Math.min(1, dx + 1.5), 1, Math.min(1, dz + 1.5));
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB query,
                                     List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean actualState) {
        BlockPos center = center(pos, state);
        if (edge && world.getBlockState(center).getBlock() != PSBlocks.MASH_TUB) return;
        AxisAlignedBB cell = new AxisAlignedBB(pos);
        for (AxisAlignedBB part : BASIN) {
            AxisAlignedBB worldBox = part.offset(center);
            if (worldBox.intersects(cell)) {
                AxisAlignedBB clipped = worldBox.intersect(cell);
                if (query.intersects(clipped)) boxes.add(clipped);
            }
        }
    }

    public static boolean canPlaceBasin(World world, BlockPos center, EntityPlayer player, EnumFacing side,
                                        ItemStack held) {
        for (BlockPos part : BlockPos.getAllInBoxMutable(center.add(-1, 0, -1), center.add(1, 0, 1))) {
            if (!world.isBlockLoaded(part) || !world.getWorldBorder().contains(part)
                    || !world.getBlockState(part).getBlock().isReplaceable(world, part)
                    || !player.canPlayerEdit(part, side, held) || !world.isBlockModifiable(player, part)) return false;
        }
        for (AxisAlignedBB part : BASIN) {
            if (!world.checkNoEntityCollision(part.offset(center))) return false;
        }
        return center.getY() > 0 && center.getY() < world.getHeight();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        MashTubBlockEntity vat = getVat(world, pos);
        if (vat == null) return false;
        if (!world.isRemote && !vat.interactWithVessel(player, hand)) {
            ItemStack held = player.getHeldItem(hand);
            if (vat.addIngredient(held)) {
                if (!player.capabilities.isCreativeMode) held.shrink(1);
            } else if (!vat.dropSolidOutput(player)) {
                BlockPos center = vat.getPos();
                player.openGui(Psychedelicraft.MOD_ID, PSGuiHandler.MASH_TUB, world,
                        center.getX(), center.getY(), center.getZ());
            }
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        MashTubBlockEntity vat = getVat(world, pos);
        if (!world.isRemote && !edge && vat != null && stack.getSubCompound("fluid") != null) {
            vat.getTank().replace(FluidContents.read(stack.getSubCompound("fluid"), MashTubBlockEntity.CAPACITY));
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(PSItems.MASH_TUB);
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!edge && BlockDrops.usesDecay(world, chance)) {
            BlockDrops.dropPool(world, pos, state, chance, fortune, harvesters.get());
        } else {
            super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (edge) return;
        ItemStack stack = new ItemStack(PSItems.MASH_TUB);
        MashTubBlockEntity vat = getVat(world, pos);
        if (vat != null) {
            if (vat.getTank().getFluid() != null) stack.setTagInfo("fluid", FluidContents.write(vat.getTank().getFluid()));
            for (int slot = 0; slot < vat.getVessels().getSlots(); slot++) {
                ItemStack vessel = vat.getVessels().getStackInSlot(slot);
                if (!vessel.isEmpty()) drops.add(vessel.copy());
            }
            for (ItemStack ingredient : vat.getSuppliedIngredients()) drops.add(ingredient.copy());
            if (!vat.getSolidOutput().isEmpty()) drops.add(vat.getSolidOutput().copy());
        }
        drops.add(stack);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest) {
        if (willHarvest) return true;
        if (edge && player.capabilities.isCreativeMode) {
            BlockPos master = center(pos, state);
            if (world.getBlockState(master).getBlock() == PSBlocks.MASH_TUB) return world.destroyBlock(master, false);
        }
        return super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity tile, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, tile, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            if (edge) {
                BlockPos master = center(pos, state);
                if (world.getBlockState(master).getBlock() == PSBlocks.MASH_TUB) world.destroyBlock(master, true);
            } else {
                for (BlockPos part : BlockPos.getAllInBoxMutable(pos.add(-1, 0, -1), pos.add(1, 0, 1))) {
                    IBlockState neighbor = world.getBlockState(part);
                    if (neighbor.getBlock() == PSBlocks.MASH_TUB_EDGE && pos.equals(center(part, neighbor))) {
                        world.setBlockToAir(part);
                    }
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Nullable
    public static MashTubBlockEntity getVat(IBlockAccess world, BlockPos pos) {
        if (world == null) return null;
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof MashTubBlock)) return null;
        BlockPos master = center(pos, state);
        if (world instanceof World && !((World) world).isBlockLoaded(master)) return null;
        TileEntity tile = world.getTileEntity(master);
        return tile instanceof MashTubBlockEntity && !tile.isInvalid() ? (MashTubBlockEntity) tile : null;
    }

    public static final class Edge extends TileEntity {
        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
            MashTubBlockEntity vat = getVat(world, pos);
            return (vat != null && vat.hasCapability(capability, side)) || super.hasCapability(capability, side);
        }

        @Override
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
            MashTubBlockEntity vat = getVat(world, pos);
            return vat == null ? super.getCapability(capability, side) : vat.getCapability(capability, side);
        }
    }
}

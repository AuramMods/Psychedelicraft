/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.client.particle.SlurryBubble;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public final class PhysicalFluidBlock extends BlockFluidClassic {
    private static final PropertyBool ATTRIBUTED = PropertyBool.create("attributed");
    public static final IUnlistedProperty<FluidStack> CONTENTS = new IUnlistedProperty<FluidStack>() {
        @Override public String getName() { return "contents"; }
        @Override public boolean isValid(FluidStack value) { return value != null; }
        @Override public Class<FluidStack> getType() { return FluidStack.class; }
        @Override public String valueToString(FluidStack value) { return value.toString(); }
    };
    private static final int MAX_LEVEL = 7;
    private static final int NO_DROP = 1000;
    private final Fluid fluid;
    private final int spreadLoss;
    private final boolean waterLike;

    public PhysicalFluidBlock(Fluid fluid) {
        super(fluid, new MaterialLiquid(MapColor.WATER));
        this.fluid = fluid;
        spreadLoss = Math.max(1, fluid.getViscosity() / 1000);
        waterLike = isWaterLike(fluid);
        setTickRate(5);
        setTickRandomly(fluid == PSFluids.COFFEE);
        setDefaultState(getDefaultState().withProperty(ATTRIBUTED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer.Builder(this).add(LEVEL, ATTRIBUTED)
                .add(FLUID_RENDER_PROPS.toArray(new IUnlistedProperty<?>[0])).add(CONTENTS).build();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, meta & MAX_LEVEL).withProperty(ATTRIBUTED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL) | (state.getValue(ATTRIBUTED) ? 8 : 0);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(ATTRIBUTED);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new FluidAttributesTile();
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ((IExtendedBlockState) super.getExtendedState(state, world, pos)).withProperty(CONTENTS, contents(world, pos));
    }

    @Override
    public float getFluidHeightForRender(IBlockAccess world, BlockPos pos, IBlockState up) {
        // The high metadata bit identifies the tile, independent of the liquid level.
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this && state.getValue(LEVEL) == 0) {
            return up.getMaterial().isLiquid() ? 1 : quantaFraction;
        }
        return super.getFluidHeightForRender(world, pos, up);
    }

    public float surfaceHeight(IBlockAccess world, BlockPos pos, IBlockState state) {
        return world.getBlockState(pos.up()).getBlock() == this ? 1F : (8 - state.getValue(LEVEL)) / 9F;
    }

    @Override
    public float getFilledPercentage(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == this ? surfaceHeight(world, pos, state) : 0F;
    }

    @Override
    public IBlockState getStateAtViewpoint(IBlockState state, IBlockAccess world, BlockPos pos, Vec3d viewpoint) {
        return viewpoint.y < pos.getY() + (double) surfaceHeight(world, pos, state) ? state : Blocks.AIR.getDefaultState();
    }

    @Override
    public Fluid getFluid() {
        return fluid;
    }

    @Override
    public String getLocalizedName() {
        return PSFluids.displayName(new FluidStack(fluid, Fluid.BUCKET_VOLUME));
    }

    public FluidStack contents(IBlockAccess world, BlockPos pos) {
        return new FluidStack(fluid, Fluid.BUCKET_VOLUME, attributes(world, pos));
    }

    @Nullable
    private NBTTagCompound attributes(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this && state.getValue(ATTRIBUTED)) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof FluidAttributesTile) {
                NBTTagCompound tag = ((FluidAttributesTile) tile).getAttributes();
                return tag.hasNoTags() ? null : tag;
            }
        }
        return null;
    }

    private boolean setFluid(World world, BlockPos pos, int level, @Nullable NBTTagCompound attributes) {
        boolean tagged = attributes != null && !attributes.hasNoTags();
        IBlockState state = getDefaultState().withProperty(LEVEL, level).withProperty(ATTRIBUTED, tagged);
        boolean stateChanged = world.getBlockState(pos) != state;
        if (stateChanged && !world.setBlockState(pos, state, 3)) return false;
        if (tagged) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof FluidAttributesTile && ((FluidAttributesTile) tile).setAttributes(attributes) && !stateChanged) {
                world.notifyNeighborsOfStateChange(pos, this, false);
            }
        }
        return true;
    }

    @Nullable
    NBTTagCompound neighborAttributes(IBlockAccess world, BlockPos pos) {
        // The donor copies the first matching fluid in enum order, independently of its flow level.
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos neighbor = pos.offset(side);
            if (world instanceof World && !((World) world).isBlockLoaded(neighbor)) continue;
            if (world.getBlockState(neighbor).getBlock() == this) return attributes(world, neighbor);
        }
        return null;
    }

    @Override
    public boolean canDisplace(IBlockAccess world, BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() >= 256
                || (world instanceof World && !((World) world).isBlockLoaded(pos))) return false;
        IBlockState target = world.getBlockState(pos);
        return !target.getMaterial().isLiquid() && !(target.getBlock() instanceof IFluidBlock) && super.canDisplace(world, pos);
    }

    @Override
    public int place(World world, BlockPos pos, FluidStack stack, boolean doPlace) {
        if (stack.getFluid() != fluid || stack.amount < Fluid.BUCKET_VOLUME || !world.getWorldBorder().contains(pos)) return 0;
        IBlockState target = world.getBlockState(pos);
        boolean replacesFlow = target.getBlock() == this && target.getValue(LEVEL) > 0;
        if (!replacesFlow && !canDisplace(world, pos)) return 0;
        if (doPlace && !world.isRemote) {
            FluidUtil.destroyBlockOnFluidPlacement(world, pos);
            if (!setFluid(world, pos, 0, stack.tag)) return 0;
        }
        return Fluid.BUCKET_VOLUME;
    }

    @Override
    @Nullable
    public FluidStack drain(World world, BlockPos pos, boolean doDrain) {
        if (!isSourceBlock(world, pos)) return null;
        FluidStack result = contents(world, pos);
        return doDrain && !world.isRemote && !world.setBlockToAir(pos) ? null : result;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (waterLike && mixWithLava(world, pos)) return;
        int level = state.getValue(LEVEL);
        NBTTagCompound attributes;
        if (level != 0) {
            int refreshed = 1;
            if (world.getBlockState(pos.up()).getBlock() != this) {
                refreshed = MAX_LEVEL + 1;
                for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                    BlockPos neighbor = pos.offset(side);
                    if (!world.isBlockLoaded(neighbor)) continue;
                    IBlockState adjacent = world.getBlockState(neighbor);
                    if (adjacent.getBlock() != this) continue;
                    int candidate = (world.getBlockState(neighbor.up()).getBlock() == this
                            ? 0 : adjacent.getValue(LEVEL)) + spreadLoss;
                    refreshed = Math.min(refreshed, candidate);
                }
            }
            if (refreshed > MAX_LEVEL) {
                world.setBlockToAir(pos);
                return;
            }
            attributes = neighborAttributes(world, pos);
            if (refreshed != level) world.scheduleUpdate(pos, this, tickRate(world));
            setFluid(world, pos, refreshed, attributes);
            level = refreshed;
        } else {
            attributes = attributes(world, pos);
        }
        FluidStack bucket = new FluidStack(fluid, Fluid.BUCKET_VOLUME, attributes);
        BlockPos below = pos.down();
        if (canDisplace(world, below) || canFillVat(world, below, bucket)) {
            spread(world, below, 1, bucket);
            return;
        }
        if (level != 0 && isFlowingVertically(world, pos)) return;
        int horizontal = world.getBlockState(pos.up()).getBlock() == this ? spreadLoss : level + spreadLoss;
        if (horizontal > MAX_LEVEL) return;
        spreadHorizontally(world, pos, horizontal, bucket);
    }

    private void spreadHorizontally(World world, BlockPos pos, int level, FluidStack bucket) {
        // Keep Forge's slope search while using the incoming batch to evaluate each vat.
        int[] costs = new int[SIDES.size()];
        int min = NO_DROP;
        for (int index = 0; index < costs.length; index++) {
            EnumFacing side = SIDES.get(index);
            BlockPos next = pos.offset(side);
            costs[index] = NO_DROP;
            if (canFlowInto(world, next, bucket) && !isSourceBlock(world, next)) {
                costs[index] = canFlowInto(world, next.down(), bucket) ? 0 : flowCost(world, next, bucket, 1, side);
                min = Math.min(min, costs[index]);
            }
        }
        for (int index = 0; index < costs.length; index++) {
            if (costs[index] == min) spread(world, pos.offset(SIDES.get(index)), level, bucket);
        }
    }

    private int flowCost(World world, BlockPos pos, FluidStack bucket, int depth, EnumFacing from) {
        int cost = NO_DROP;
        for (EnumFacing side : SIDES) {
            if (side == from.getOpposite()) continue;
            BlockPos next = pos.offset(side);
            if (!canFlowInto(world, next, bucket) || isSourceBlock(world, next)) continue;
            if (canFlowInto(world, next.down(), bucket)) return depth;
            if (depth < quantaPerBlock / 2) cost = Math.min(cost, flowCost(world, next, bucket, depth + 1, side));
        }
        return cost;
    }

    private boolean canFlowInto(World world, BlockPos pos, FluidStack bucket) {
        return canFlowInto(world, pos) || canFillVat(world, pos, bucket);
    }

    private boolean canFillVat(World world, BlockPos pos, FluidStack bucket) {
        return world.isBlockLoaded(pos) && VatInflow.canAccept(world, pos, world.getBlockState(pos), bucket);
    }

    private boolean mixWithLava(World world, BlockPos pos) {
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos neighbor = pos.offset(side);
            if (!world.isBlockLoaded(neighbor)) continue;
            IBlockState lava = world.getBlockState(neighbor);
            if (!(lava.getBlock() instanceof BlockLiquid) || lava.getMaterial() != Material.LAVA) continue;
            if (side == EnumFacing.UP) {
                if (lava.getBlock() == Blocks.FLOWING_LAVA
                        && solidify(world, pos, neighbor, Blocks.STONE.getDefaultState())) return true;
            } else {
                int level = lava.getValue(BlockLiquid.LEVEL);
                if (level <= 4) {
                    solidify(world, neighbor, neighbor, level == 0 ? Blocks.OBSIDIAN.getDefaultState()
                            : Blocks.COBBLESTONE.getDefaultState());
                }
            }
        }
        return false;
    }

    private boolean solidify(World world, BlockPos target, BlockPos source, IBlockState product) {
        IBlockState replacement = ForgeEventFactory.fireFluidPlaceBlockEvent(world, target, source, product);
        if (!world.setBlockState(target, replacement, 3)) return false;
        fluid.vaporize(null, world, target, new FluidStack(fluid, Fluid.BUCKET_VOLUME));
        return true;
    }

    private void spread(World world, BlockPos pos, int level, FluidStack bucket) {
        if (!world.isBlockLoaded(pos) || VatInflow.fillIfVat(world, pos, world.getBlockState(pos), bucket)) return;
        if (displaceIfPossible(world, pos)) setFluid(world, pos, level, neighborAttributes(world, pos));
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (fluid == PSFluids.COFFEE) {
            NBTTagCompound tag = attributes(world, pos);
            if (tag == null) return;
            int warmth = PSFluids.warmth(new FluidStack(fluid, Fluid.BUCKET_VOLUME, tag));
            if (warmth == 0) return;
            if (warmth == 1) tag.removeTag("warmth");
            else tag.setInteger("warmth", warmth - 1);
            setFluid(world, pos, state.getValue(LEVEL), tag);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        boolean falling = world.getBlockState(pos.up()).getBlock() == this;
        IBlockState display = Blocks.WATER.getDefaultState().withProperty(BlockLiquid.LEVEL,
                falling ? 8 : state.getValue(LEVEL));
        Blocks.WATER.randomDisplayTick(display, world, pos, random);
        if (fluid == PSFluids.SLURRY) {
            SlurryBubble.ambient(world, new Vec3d(pos.getX() + 0.5, pos.getY() + getFilledPercentage(world, pos),
                    pos.getZ() + 0.5), fluid.getColor(), 0.5, random);
        }
    }

    @Override
    @Nullable
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos, IBlockState state, Entity entity,
                                           double y, Material material, boolean testingHead) {
        if (material != Material.WATER) return null;
        return waterLike && (testingHead ? y < pos.getY() + (double) surfaceHeight(world, pos, state)
                : intersectsFluid(world, pos, state, entity.getEntityBoundingBox().shrink(0.001D)));
    }

    @Override
    @Nullable
    public Boolean isAABBInsideMaterial(World world, BlockPos pos, AxisAlignedBB box, Material material) {
        return material == Material.WATER ? waterLike && isAABBInsideLiquid(world, pos, box) : null;
    }

    @Override
    public Boolean isAABBInsideLiquid(World world, BlockPos pos, AxisAlignedBB box) {
        return intersectsFluid(world, pos, world.getBlockState(pos), box);
    }

    @Override
    public float getBlockLiquidHeight(World world, BlockPos pos, IBlockState state, Material material) {
        return material == Material.WATER && waterLike ? surfaceHeight(world, pos, state) : 0;
    }

    private boolean intersectsFluid(IBlockAccess world, BlockPos pos, IBlockState state, AxisAlignedBB box) {
        return box.intersects(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + (double) surfaceHeight(world, pos, state), pos.getZ() + 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Vec3d getFogColor(World world, BlockPos pos, IBlockState state, Entity entity, Vec3d original, float partialTicks) {
        double cameraY = ActiveRenderInfo.projectViewFromEntity(entity, partialTicks).y;
        if (!waterLike || cameraY >= pos.getY() + (double) surfaceHeight(world, pos, state)) return original;
        int color = fluid.getColor(contents(world, pos));
        return new Vec3d((color >> 16 & 255) / 255D, (color >> 8 & 255) / 255D, (color & 255) / 255D);
    }

    public boolean isWaterLike() {
        return waterLike;
    }

    public static boolean isWaterLike(Fluid fluid) {
        return fluid == FluidRegistry.WATER || (fluid instanceof WorldFluid
                && fluid != PSFluids.AGAVE && fluid != PSFluids.KAVA && fluid != PSFluids.BATH_SALTS);
    }
}

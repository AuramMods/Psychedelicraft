package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.BubbleColumnBlock;
import ivorius.psychedelicraft.block.entity.WaterloggedBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class Waterlogging {
    private Waterlogging() {
    }

    public static boolean supports(IBlockState state) {
        return state.getBlock() instanceof Waterloggable;
    }

    public static boolean needsWaterTile(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof Waterloggable && !((Waterloggable) block).hasOwnWaterTile();
    }

    @Nullable
    public static WaterloggedBlockEntity tile(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof WaterloggedBlockEntity ? (WaterloggedBlockEntity) tile : null;
    }

    public static boolean isWet(IBlockAccess world, BlockPos pos) {
        return isWet(world, pos, world.getBlockState(pos));
    }

    public static boolean isWet(World world, BlockPos pos) {
        return isWet((IBlockAccess) world, pos);
    }

    public static boolean keepsWaterSource(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return BubbleColumnBlock.isColumn(state) || isWet(world, pos, state);
    }

    public static boolean isWet(IBlockAccess world, BlockPos pos, IBlockState state) {
        if (!supports(state)) return false;
        WaterloggedBlockEntity tile = tile(world, pos);
        return tile != null && tile.isWaterlogged();
    }

    public static boolean isWaterSource(IBlockState state) {
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER || BubbleColumnBlock.isColumn(state))
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    public static void fillSource(World world, BlockPos pos) {
        WaterloggedBlockEntity tile = tile(world, pos);
        if (tile != null) tile.fill(new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), true);
    }

    public static IBlockState fluidState(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return isWet(world, pos, state) ? Blocks.WATER.getDefaultState() : state;
    }

    public static IBlockState fluidState(World world, BlockPos pos) {
        return fluidState((IBlockAccess) world, pos);
    }

    public static float liquidShapeHeight(World world, BlockPos pos, IBlockState fluidState) {
        Block block = fluidState.getBlock();
        if (block instanceof BlockLiquid) {
            IBlockState above = fluidState(world, pos.up());
            return above.getMaterial() == fluidState.getMaterial() ? 1
                    : 1 - BlockLiquid.getLiquidHeightPercent(fluidState.getValue(BlockLiquid.LEVEL));
        }
        if (block instanceof PhysicalFluidBlock) {
            return ((PhysicalFluidBlock) block).surfaceHeight(world, pos, fluidState);
        }
        return block instanceof IFluidBlock ? ((IFluidBlock) block).getFilledPercentage(world, pos) : 0;
    }

    public static boolean isWater(IBlockAccess world, BlockPos pos) {
        return isWater(world, pos, world.getBlockState(pos));
    }

    public static boolean isWater(IBlockAccess world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        return state.getMaterial() == Material.WATER || isWet(world, pos, state)
                || block instanceof PhysicalFluidBlock && ((PhysicalFluidBlock) block).isWaterLike();
    }

    public static IBlockState waterState(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial() != Material.WATER && isWater(world, pos, state)
                ? Blocks.WATER.getDefaultState() : state;
    }

    public static void displayTick(Block block, IBlockState state, World world, BlockPos pos, Random random) {
        block.randomDisplayTick(state, world, pos, random);
        if (isWet(world, pos, state)) Blocks.WATER.randomDisplayTick(Blocks.WATER.getDefaultState(), world, pos, random);
    }

    public static boolean blocksPiston(boolean blocked, IBlockState state) {
        Block block = state.getBlock();
        if (!(block instanceof Waterloggable)) return blocked;
        return block.hasTileEntity(state) && !((Waterloggable) block).canPistonMove();
    }

    public static boolean canDetachForPiston(Block block, IBlockState state) {
        return block.hasTileEntity(state) && (!(block instanceof Waterloggable) || !((Waterloggable) block).canPistonMove());
    }

    public static void scheduleFlow(World world, BlockPos pos) {
        world.scheduleUpdate(pos, world.getBlockState(pos).getBlock(), Blocks.FLOWING_WATER.tickRate(world));
    }

    public static void scheduleIfWet(World world, BlockPos pos) {
        if (!world.isRemote && isWet(world, pos)) scheduleFlow(world, pos);
    }

    public static void tickBlock(Block block, World world, BlockPos pos, IBlockState state, Random random) {
        block.updateTick(world, pos, state, random);
        if (block == Blocks.SOUL_SAND) BubbleColumnBlock.updateAbove(world, pos);
        if (block instanceof Waterloggable && isWet(world, pos)) {
            Blocks.FLOWING_WATER.updateTick(world, pos, Blocks.FLOWING_WATER.getDefaultState(), random);
        }
    }

    public static IBlockState releasedState(World world, BlockPos pos) {
        return (isWet(world, pos) ? Blocks.FLOWING_WATER : Blocks.AIR).getDefaultState();
    }

    public static boolean remove(World world, BlockPos pos, IBlockState replacement, int flags) {
        return world.setBlockState(pos, replacement.getBlock() == Blocks.AIR ? releasedState(world, pos) : replacement, flags);
    }

    public static boolean replacePreservingWater(World world, BlockPos pos, IBlockState next, int flags) {
        boolean wet = isWet(world, pos);
        if (!world.setBlockState(pos, next, flags)) return false;
        if (wet) fillSource(world, pos);
        return true;
    }

    public static boolean bucketItem(ItemStack held) {
        return held.getItem() == Items.BUCKET || held.getItem() == Items.WATER_BUCKET;
    }

    public static boolean isOutletClosed(World world, BlockPos from, BlockPos to) {
        IBlockState state = world.getBlockState(from);
        if (!isWet(world, from, state)) return false;
        EnumFacing face = EnumFacing.getFacingFromVector(to.getX() - from.getX(),
                to.getY() - from.getY(), to.getZ() - from.getZ());
        return state.getBlockFaceShape(world, from, face) == BlockFaceShape.SOLID;
    }

    public static IBlockState flowDestination(World world, BlockPos to, BlockPos from) {
        return isOutletClosed(world, from, to) ? Blocks.BEDROCK.getDefaultState() : world.getBlockState(to);
    }

    @Nullable
    public static Boolean physicalFluidAtEyes(Entity entity, Material material) {
        if (material != Material.WATER || entity.getRidingEntity() instanceof EntityBoat) return null;
        double eyeY = entity.posY + entity.getEyeHeight();
        double sampleY = eyeY - (double) (1F / 9F);
        World world = entity.world;
        BlockPos sample = new BlockPos(entity.posX, sampleY, entity.posZ);
        IBlockState fluid = fluidState(world, sample);
        if (!(fluid.getBlock() instanceof PhysicalFluidBlock)) {
            BlockPos eyes = new BlockPos(entity.posX, eyeY, entity.posZ);
            if (!(world.getBlockState(eyes).getBlock() instanceof PhysicalFluidBlock)) return null;
        }
        return isWater(world, sample, fluid)
                && sampleY < sample.getY() + (double) liquidShapeHeight(world, sample, fluid);
    }

    @Nullable
    public static Boolean containsEntity(IBlockAccess world, BlockPos pos, IBlockState state, Entity entity,
                                         double y, Material material, boolean head) {
        if (material != Material.WATER || !isWet(world, pos, state)) return null;
        return y >= pos.getY() && (!head || y < pos.getY() + 1);
    }

    @Nullable
    public static Boolean containsMaterial(World world, BlockPos pos, AxisAlignedBB box, Material material) {
        return material == Material.WATER ? containsLiquid(world, pos, box) : null;
    }

    @Nullable
    public static Boolean containsLiquid(World world, BlockPos pos, AxisAlignedBB box) {
        return isWet(world, pos) ? box.intersects(new AxisAlignedBB(pos)) : null;
    }

    public static float liquidHeight(World world, BlockPos pos, IBlockState state, Material material) {
        return material == Material.WATER && isWet(world, pos, state) ? 1 : 0;
    }

    public static Vec3d acceleration(World world, BlockPos pos, Entity entity, Vec3d motion) {
        return isWet(world, pos) ? Blocks.WATER.modifyAcceleration(world, pos, entity, motion) : motion;
    }

    public static int fireValue(int normal, IBlockAccess world, BlockPos pos) {
        return normal != 0 && isWet(world, pos) ? 0 : normal;
    }

    @SubscribeEvent
    public static void useBucket(FillBucketEvent event) {
        RayTraceResult hit = event.getTarget();
        ItemStack held = event.getEmptyBucket();
        Item item = held.getItem();
        if (event.getResult() != Event.Result.DEFAULT || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || (item != Items.BUCKET && item != Items.WATER_BUCKET)) return;
        World world = event.getWorld();
        BlockPos clicked = hit.getBlockPos();
        BlockPos pos = clicked;
        boolean pickup = item == Items.BUCKET;
        if (!supports(world.getBlockState(pos))) {
            if (pickup || !supports(world.getBlockState(pos.offset(hit.sideHit)))) return;
            pos = pos.offset(hit.sideHit);
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!world.isBlockModifiable(player, clicked) || !world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(pos, hit.sideHit, held)) {
            event.setCanceled(true);
            return;
        }

        WaterloggedBlockEntity tile = tile(world, pos);
        if (tile == null) return;
        if (pickup ? tile.drain(Fluid.BUCKET_VOLUME, true) == null
                : tile.fill(new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), true) == 0) return;
        event.setFilledBucket(new ItemStack(pickup ? Items.WATER_BUCKET : Items.BUCKET));
        event.setResult(Event.Result.ALLOW);
        if (!world.isRemote) {
            player.addStat(StatList.getObjectUseStats(item));
            world.playSound(null, pos, pickup ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BUCKET_EMPTY,
                    SoundCategory.BLOCKS, 1, 1);
        }
    }

    public static void registerDispenserBehavior() {
        IBehaviorDispenseItem vanilla = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.WATER_BUCKET);
        IBehaviorDispenseItem fill = new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                return new ItemStack(Items.BUCKET);
            }
        };
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.WATER_BUCKET, (source, stack) -> {
            World world = source.getWorld();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            if (!supports(world.getBlockState(pos))) return vanilla.dispense(source, stack);
            WaterloggedBlockEntity tile = tile(world, pos);
            if (tile == null || tile.fill(new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), true) == 0) {
                return vanilla.dispense(source, stack);
            }
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1, 1);
            return fill.dispense(source, stack);
        });
        IBehaviorDispenseItem emptyBucket = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.BUCKET);
        BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();
        IBehaviorDispenseItem pickup = new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                ItemStack filled = new ItemStack(Items.WATER_BUCKET);
                if (stack.getCount() == 1) return filled;
                TileEntityDispenser dispenser = source.getBlockTileEntity();
                if (dispenser.addItemStack(filled) < 0) drop.dispense(source, filled);
                stack.shrink(1);
                return stack;
            }
        };
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.BUCKET, (source, stack) -> {
            World world = source.getWorld();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            if (!isWet(world, pos)) return emptyBucket.dispense(source, stack);
            WaterloggedBlockEntity tile = tile(world, pos);
            if (tile == null || tile.drain(Fluid.BUCKET_VOLUME, true) == null) return emptyBucket.dispense(source, stack);
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1);
            return pickup.dispense(source, stack);
        });
    }
}

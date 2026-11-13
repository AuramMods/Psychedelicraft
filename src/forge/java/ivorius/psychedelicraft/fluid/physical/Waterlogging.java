package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.LatticeBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.WaterloggedBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class Waterlogging {
    private Waterlogging() {
    }

    public static boolean supports(IBlockState state) {
        return state.getBlock() instanceof LatticeBlock;
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

    public static boolean isWet(IBlockAccess world, BlockPos pos, IBlockState state) {
        if (!supports(state)) return false;
        WaterloggedBlockEntity tile = tile(world, pos);
        return tile != null && tile.isWaterlogged();
    }

    public static boolean isWaterSource(IBlockState state) {
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
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

    public static boolean blocksPiston(Block block, IBlockState state) {
        return block != PSBlocks.LATTICE && block.hasTileEntity(state);
    }

    public static void scheduleFlow(World world, BlockPos pos) {
        world.scheduleUpdate(pos, world.getBlockState(pos).getBlock(), Blocks.FLOWING_WATER.tickRate(world));
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

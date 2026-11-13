/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.FluidVessel;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockLiquid;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.DispenseFluidContainer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class FilledBucketItem extends PortableFluidItem {
    public FilledBucketItem() {
        super(FluidVessel.BUCKET);
        setContainerItem(Items.BUCKET);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing face,
                                          float x, float y, float z, EnumHand hand) {
        Block block = world.getBlockState(pos).getBlock();
        return block instanceof IFluidBlock || block instanceof BlockLiquid ? EnumActionResult.PASS
                : super.onItemUseFirst(player, world, pos, face, x, y, z, hand);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        FluidStack contents = contents(held);
        RayTraceResult hit = rayTrace(world, player, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return new ActionResult<>(EnumActionResult.PASS, held);
        if (contents == null || contents.amount != Fluid.BUCKET_VOLUME) return new ActionResult<>(EnumActionResult.FAIL, held);
        BlockPos clicked = hit.getBlockPos();
        boolean replaceable = world.getBlockState(clicked).getBlock().isReplaceable(world, clicked);
        BlockPos target = replaceable && hit.sideHit == EnumFacing.UP ? clicked : clicked.offset(hit.sideHit);
        if (!world.isBlockModifiable(player, clicked) || !world.isBlockModifiable(player, target)
                || !player.canPlayerEdit(target, hit.sideHit, held)) return new ActionResult<>(EnumActionResult.FAIL, held);

        FluidVesselHandler source = FluidVesselHandler.forCrafting(held);
        if (source == null || !FluidUtil.tryPlaceFluid(player, world, target, source, contents)) {
            return new ActionResult<>(EnumActionResult.FAIL, held);
        }
        if (player instanceof EntityPlayerMP) CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, target, held);
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.SUCCESS, player.capabilities.isCreativeMode ? held : source.getContainer());
    }

    @SubscribeEvent
    public static void fillVanillaBucket(FillBucketEvent event) {
        RayTraceResult hit = event.getTarget();
        if (event.getResult() != Event.Result.DEFAULT || event.getEmptyBucket().getItem() != Items.BUCKET
                || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return;
        World world = event.getWorld();
        BlockPos pos = hit.getBlockPos();
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof PhysicalFluidBlock)) return;
        EntityPlayer player = event.getEntityPlayer();
        if (!world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(pos.offset(hit.sideHit), hit.sideHit, event.getEmptyBucket())) {
            event.setCanceled(true);
            return;
        }
        ItemStack filled = collect(world, pos, (PhysicalFluidBlock) block, event.getEmptyBucket());
        if (filled.isEmpty()) return;
        if (!world.isRemote) player.addStat(StatList.getObjectUseStats(Items.BUCKET));
        event.setFilledBucket(filled);
        event.setResult(Event.Result.ALLOW);
    }

    public static void registerDispenserBehavior() {
        BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(PSItems.FILLED_BUCKET, (source, stack) -> {
            FluidStack contents = PSItems.FILLED_BUCKET.contents(stack);
            return (contents != null && contents.amount == Fluid.BUCKET_VOLUME ? DispenseFluidContainer.getInstance() : drop)
                    .dispense(source, stack);
        });
        IBehaviorDispenseItem vanilla = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.BUCKET);
        BehaviorDefaultDispenseItem pickup = new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
                Block block = world.getBlockState(pos).getBlock();
                ItemStack filled = collect(world, pos, (PhysicalFluidBlock) block, stack);
                if (filled.isEmpty()) return super.dispenseStack(source, stack);
                if (stack.getCount() == 1) return filled;
                TileEntityDispenser dispenser = source.getBlockTileEntity();
                if (dispenser.addItemStack(filled) < 0) super.dispenseStack(source, filled);
                stack.shrink(1);
                return stack;
            }
        };
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.BUCKET, (source, stack) -> {
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            return (source.getWorld().getBlockState(pos).getBlock() instanceof PhysicalFluidBlock ? pickup : vanilla)
                    .dispense(source, stack);
        });
    }

    private static ItemStack collect(World world, BlockPos pos, PhysicalFluidBlock block, ItemStack empty) {
        FluidStack contents = block.drain(world, pos, !world.isRemote);
        if (contents == null) return ItemStack.EMPTY;
        world.playSound(null, pos, contents.getFluid().getFillSound(contents), SoundCategory.BLOCKS, 1, 1);
        return filled(contents, empty);
    }

    private static ItemStack filled(FluidStack contents, ItemStack empty) {
        ItemStack filled = new ItemStack(PSItems.FILLED_BUCKET);
        if (empty.hasTagCompound()) filled.setTagCompound(empty.getTagCompound().copy());
        filled.setTagInfo("fluid", FluidContents.write(contents));
        return filled;
    }
}

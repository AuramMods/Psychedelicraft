/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PhysicalFluidBlock;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class FilledBucketItem extends PortableFluidItem {
    public FilledBucketItem(){super(FluidVessel.BUCKET);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof LiquidBlock?InteractionResult.PASS:super.onItemUseFirst(stack,context);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);var contents=contents(held);var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.NONE);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(held);
        if(held.getCount()!=1||contents.getAmount()!=1000)return InteractionResultHolder.fail(held);
        var clicked=hit.getBlockPos();var target=level.getBlockState(clicked).canBeReplaced()&&hit.getDirection()==Direction.UP?clicked:clicked.relative(hit.getDirection());
        if(!level.mayInteract(player,clicked)||!level.mayInteract(player,target)||!player.mayUseItemAt(target,hit.getDirection(),held))return InteractionResultHolder.fail(held);
        var source=FluidVesselHandler.forCrafting(held);
        if(source==null||!FluidUtil.tryPlaceFluid(player,level,hand,target,source,contents))return InteractionResultHolder.fail(held);
        if(player instanceof ServerPlayer server)CriteriaTriggers.PLACED_BLOCK.trigger(server,target,held);
        player.awardStat(Stats.ITEM_USED.get(this));if(!level.isClientSide)level.gameEvent(player,GameEvent.FLUID_PLACE,target);
        return InteractionResultHolder.sidedSuccess(player.getAbilities().instabuild?held:source.getContainer(),level.isClientSide);
    }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack){return true;}
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack){return filled(FluidStack.EMPTY,stack);}
    public static ItemStack filled(FluidStack fluid,ItemStack empty){if(fluid.isEmpty())return new FluidVesselHandler(single(empty),FluidVessel.BUCKET).replaceContents(fluid);var result=new ItemStack(PSItems.FILLED_BUCKET.get());if(empty.hasTag())result.setTag(empty.getTag().copy());result.getOrCreateTag().put("fluid",FluidContents.write(fluid));return result;}
    private static ItemStack single(ItemStack stack){var result=stack.copy();result.setCount(1);return result;}
    @SubscribeEvent public static void pickup(FillBucketEvent event){
        if(event.getResult()!=Event.Result.DEFAULT||!event.getEmptyBucket().is(Items.BUCKET)||!(event.getTarget() instanceof BlockHitResult hit))return;
        var level=event.getLevel();var pos=hit.getBlockPos();if(!(level.getBlockState(pos).getBlock() instanceof PhysicalFluidBlock block))return;
        var player=event.getEntity();if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos.relative(hit.getDirection()),hit.getDirection(),event.getEmptyBucket())){event.setCanceled(true);return;}
        var fluid=block.drain(level,pos,level.isClientSide?FluidAction.SIMULATE:FluidAction.EXECUTE);if(fluid.isEmpty())return;
        var result=filled(fluid,event.getEmptyBucket());event.setFilledBucket(result);event.setResult(Event.Result.ALLOW);
        if(!level.isClientSide){player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BUCKET_FILL,net.minecraft.sounds.SoundSource.BLOCKS,1,1);level.gameEvent(player,GameEvent.FLUID_PICKUP,pos);if(player instanceof ServerPlayer server)CriteriaTriggers.FILLED_BUCKET.trigger(server,result);}
    }
    public static void registerDispenser(){
        var drop=new DefaultDispenseItemBehavior();
        DispenserBlock.registerBehavior(PSItems.FILLED_BUCKET.get(),(source,stack)->(PSItems.FILLED_BUCKET.get().contents(stack).getAmount()==1000?DispenseFluidContainer.getInstance():drop).dispense(source,stack));
        var vanilla=DispenserBlock.DISPENSER_REGISTRY.get(Items.BUCKET);
        var pickup=new DefaultDispenseItemBehavior(){@Override protected ItemStack execute(BlockSource source,ItemStack stack){
            var level=source.getLevel();var pos=source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));var block=(PhysicalFluidBlock)level.getBlockState(pos).getBlock();
            var fluid=block.drain(level,pos,FluidAction.EXECUTE);if(fluid.isEmpty())return super.execute(source,stack);var result=filled(fluid,stack);
            if(stack.getCount()==1)return result;stack.shrink(1);if(((DispenserBlockEntity)source.getEntity()).addItem(result)<0)super.execute(source,result);return stack;
        }};
        DispenserBlock.registerBehavior(Items.BUCKET,(source,stack)->{var pos=source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));return(source.getLevel().getBlockState(pos).getBlock() instanceof PhysicalFluidBlock?pickup:vanilla).dispense(source,stack);});
    }
}

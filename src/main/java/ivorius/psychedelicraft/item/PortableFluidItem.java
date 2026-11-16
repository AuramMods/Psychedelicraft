/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.fluid.*;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class PortableFluidItem extends Item {
    private final FluidVessel vessel;
    public PortableFluidItem(FluidVessel vessel){super(new Item.Properties().stacksTo(vessel==FluidVessel.BOTTLE?16:1));this.vessel=vessel;}
    public FluidVessel getVessel(){return vessel;}
    public FluidStack contents(ItemStack stack){return FluidVesselHandler.contents(stack,vessel);}
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,@Nullable CompoundTag tag){return new FluidVesselHandler(stack,vessel);}
    @Override public int getMaxStackSize(ItemStack stack){return vessel==FluidVessel.BOTTLE&&!contents(stack).isEmpty()?1:super.getMaxStackSize(stack);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        Player player=context.getPlayer();Level level=context.getLevel();
        if(player==null||level.isClientSide||!level.mayInteract(player,context.getClickedPos()))return InteractionResult.PASS;
        var block=level.getBlockState(context.getClickedPos()).getBlock();
        if(block instanceof ivorius.psychedelicraft.block.BarrelBlock||block instanceof ivorius.psychedelicraft.block.MashTubBlock||block instanceof ivorius.psychedelicraft.block.MashTubWallBlock)return InteractionResult.PASS;
        return FluidUtil.interactWithFluidHandler(player,context.getHand(),level,context.getClickedPos(),context.getClickedFace())?InteractionResult.SUCCESS:InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context){return ivorius.psychedelicraft.block.PlacedDrinksBlock.placeOnBlock(context);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);if(serving(stack).isEmpty())return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        FluidStack portion=serving(stack);if(level.isClientSide||portion.isEmpty()||!(entity instanceof Player player))return stack;
        PSFluids.consume(player,portion);ivorius.psychedelicraft.advancement.PSCriteria.drankBrew(player,portion);player.awardStat(Stats.ITEM_USED.get(this));
        if(player instanceof ServerPlayer server)CriteriaTriggers.CONSUME_ITEM.trigger(server,stack);
        if(player.getAbilities().instabuild)return stack;
        FluidVesselHandler handler=new FluidVesselHandler(stack,vessel);handler.drain(portion,FluidAction.EXECUTE);return handler.getContainer();
    }
    public FluidStack serving(ItemStack stack){
        FluidStack fluid=contents(stack);
        if(stack.getCount()!=1||vessel.serving==0||fluid.getAmount()<vessel.serving||!PSFluids.canConsume(fluid.getFluid(),vessel==FluidVessel.SYRINGE))return FluidStack.EMPTY;
        fluid.setAmount(vessel.serving);return fluid;
    }
    @Override public UseAnim getUseAnimation(ItemStack stack){return serving(stack).isEmpty()?UseAnim.NONE:vessel==FluidVessel.SYRINGE?UseAnim.BOW:UseAnim.DRINK;}
    @Override public int getUseDuration(ItemStack stack){return vessel.useTicks;}
    @Override public Component getName(ItemStack stack){FluidStack fluid=contents(stack);return fluid.isEmpty()?super.getName(stack):Component.translatable(getDescriptionId()+".filled",PSFluids.displayName(fluid));}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){
        FluidStack fluid=contents(stack);tooltip.add(Component.translatable("psychedelicraft.drink.levels",fluid.getAmount(),vessel.capacity).withStyle(ChatFormatting.GRAY));
        if(!fluid.isEmpty()&&PSFluids.brew(fluid.getFluid())!=null)BrewProfile.appendAttributes(fluid,tooltip);
        if(flag.isAdvanced()&&!fluid.isEmpty())tooltip.add(Component.literal(PSFluids.id(fluid.getFluid())).withStyle(ChatFormatting.DARK_GRAY));
    }
    @Override public boolean isBarVisible(ItemStack stack){FluidStack fluid=contents(stack);return !fluid.isEmpty()&&fluid.getAmount()<vessel.capacity;}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13*contents(stack).getAmount()/(float)vessel.capacity);}
    @Override public int getBarColor(ItemStack stack){return 0xaaaaff;}
}

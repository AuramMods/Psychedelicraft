/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class SmokingItem extends Item {
    private final int drawTicks,burnTime;
    protected SmokingItem(int uses,int drawTicks,int burnTime){
        super(new Item.Properties().durability(uses));this.drawTicks=drawTicks;this.burnTime=burnTime;
    }
    @Override public int getBurnTime(ItemStack stack,@Nullable RecipeType<?> recipeType){return burnTime;}
    @Override public int getUseDuration(ItemStack stack){return drawTicks;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.TOOT_HORN;}
    public boolean canDraw(Player player){return true;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(DrugCapability.get(player).isBreathingSmoke()||!canDraw(player))return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    protected static void recordDraw(Player player,ItemStack stack){
        if(player instanceof ServerPlayer server)CriteriaTriggers.CONSUME_ITEM.trigger(server,stack);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }
    protected static ItemStack wear(ItemStack stack,LivingEntity entity){
        if(entity instanceof Player player&&player.getAbilities().instabuild)return stack;
        if(!stack.isDamageableItem()){stack.shrink(1);return stack;}
        stack.hurtAndBreak(1,entity,user->user.broadcastBreakEvent(user.getUsedItemHand()));
        return stack;
    }
}

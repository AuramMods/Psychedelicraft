/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on Jan 1 2023. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class BongItem extends SmokingItem {
    private final Map<Item,Function<ItemStack,DrugInfluence>> ingredients=new HashMap<>();
    private final boolean bubbles;
    public BongItem(int uses,int burnTime,boolean bubbles){super(uses,30,burnTime);this.bubbles=bubbles;}
    BongItem consumes(Item item,DrugInfluence influence){return consumes(item,stack->influence.copy());}
    BongItem consumes(Item item,Function<ItemStack,DrugInfluence> influence){ingredients.put(item,influence);return this;}
    private int ingredientSlot(Player player){
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            ItemStack stack=player.getInventory().getItem(slot);
            if(!stack.isEmpty()&&ingredients.containsKey(stack.getItem()))return slot;
        }
        return -1;
    }
    @Override public boolean canDraw(Player player){return ingredientSlot(player)>=0;}
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(bubbles&&!level.isClientSide&&entity.getRandom().nextInt(3)==0)entity.playSound(SoundEvents.LAVA_POP,1,1);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(level.isClientSide||!(entity instanceof Player player))return stack;
        int slot=ingredientSlot(player);if(slot<0)return stack;
        ItemStack used=player.getInventory().removeItem(slot,1);
        var properties=DrugCapability.get(player);
        properties.inhale(ingredients.get(used.getItem()).apply(used),player,2);
        if(player instanceof ServerPlayer server)CriteriaTriggers.CONSUME_ITEM.trigger(server,used);
        properties.startBreathingSmoke(player,0xffffff);recordDraw(player,stack);
        return wear(stack,player);
    }
}

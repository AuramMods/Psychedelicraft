/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
/** Original fifteen-tick fruit food with creative inventory preservation. */
public class QuickFoodItem extends Item {
    public QuickFoodItem(){super(new Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(.5F).meat().build()));}
    @Override public int getUseDuration(ItemStack stack){return 15;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(user instanceof Player player&&player.getAbilities().instabuild){super.finishUsingItem(stack.copy(),level,user);return stack;}
        return super.finishUsingItem(stack,level,user);
    }
}

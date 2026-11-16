/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EdibleItem extends Item {
    private final DrugInfluence influence;
    public EdibleItem(int hunger, float saturation, DrugInfluence influence) {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(hunger).saturationMod(saturation).alwaysEat().build()));
        this.influence = influence;
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        if (!level.isClientSide && consumer instanceof Player player) DrugCapability.get(player).addInfluence(influence.copy());
        return super.finishUsingItem(stack, level, consumer);
    }
}

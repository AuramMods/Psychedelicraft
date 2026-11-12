/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import java.util.function.Supplier;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class EdibleItem extends ItemFood {
    private final Supplier<DrugInfluence> influence;

    public EdibleItem(int hunger, float saturation, Supplier<DrugInfluence> influence) {
        super(hunger, saturation, false);
        this.influence = influence;
        setAlwaysEdible();
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        // ItemFood shrinks even a creative player's stack in 1.12.
        if (user instanceof EntityPlayer && ((EntityPlayer) user).isCreative()) {
            super.onItemUseFinish(stack.copy(), world, user);
            return stack;
        }
        return super.onItemUseFinish(stack, world, user);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        super.onFoodEaten(stack, world, player);
        if (!world.isRemote) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
                properties.addInfluence(influence.get());
            }
        }
    }
}

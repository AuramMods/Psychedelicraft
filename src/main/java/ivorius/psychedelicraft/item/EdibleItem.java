/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class EdibleItem extends ItemFood {
    @Nullable
    private final Supplier<DrugInfluence> influence;

    public EdibleItem(int hunger, float saturation) {
        this(hunger, saturation, null);
    }

    public EdibleItem(int hunger, float saturation, @Nullable Supplier<DrugInfluence> influence) {
        super(hunger, saturation, false);
        this.influence = influence;
        setAlwaysEdible();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        // The 1.12 food check rejects Creative even for always-edible items.
        if (!player.isCreative()) return super.onItemRightClick(world, player, hand);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
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
        if (!world.isRemote && influence != null) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
                properties.addInfluence(influence.get());
            }
        }
    }
}

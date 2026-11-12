/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

public final class PowderItem extends EdibleItem {
    public PowderItem(DrugInfluence influence) {
        super(0, 0.1F, influence::copy);
        addPropertyOverride(Psychedelicraft.id("using"), (stack, world, entity) -> useModel(stack, entity));
    }

    static float useModel(ItemStack stack, EntityLivingBase entity) {
        return entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack ? 1F : 0F;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }
}

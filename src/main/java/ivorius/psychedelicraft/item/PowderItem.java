/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class PowderItem extends EdibleItem {
    public PowderItem(DrugInfluence influence) { super(0, 0.1F, influence); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.TOOT_HORN; }
}

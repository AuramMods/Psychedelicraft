/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class SmokeableItem extends SmokingItem {
    private final int color;
    private final DrugInfluence[] influences;

    public SmokeableItem(int uses, int burnTime, int color, DrugInfluence... influences) {
        super(uses, 25, burnTime);
        this.color = color;
        this.influences = influences.clone();
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) properties.startBreathingSmoke(player, color);
            if (!world.isRemote) {
                if (properties != null) applyInfluences(properties);
                recordDraw(player, stack);
            }
        }
        return wear(stack, entity);
    }

    public int smokeColor() {
        return color;
    }

    public void applyInfluences(DrugProperties properties) {
        for (DrugInfluence influence : influences) properties.addInfluence(influence.copy());
    }
}

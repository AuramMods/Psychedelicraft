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
    private final float nicotineSaturation;
    private final DrugInfluence[] influences;

    public SmokeableItem(int uses, int burnTime, int color, DrugInfluence... influences) {
        this(uses, burnTime, color, 2F, influences);
    }

    public SmokeableItem(int uses, int burnTime, int color, float nicotineSaturation, DrugInfluence... influences) {
        super(uses, 25, burnTime);
        this.color = color;
        this.nicotineSaturation = nicotineSaturation;
        this.influences = influences.clone();
    }

    SmokeableItem stackable() {
        setMaxStackSize(64);
        return this;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return stack.getItemDamage() > 0 ? 1 : super.getItemStackLimit(stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) properties.startBreathingSmoke(player, color);
            if (!world.isRemote) {
                if (properties != null) applyInfluences(player, properties);
                recordDraw(player, stack);
            }
            if (!player.capabilities.isCreativeMode && stack.getCount() > 1 && stack.getMaxStackSize() > 1) {
                ItemStack used = wear(stack.splitStack(1), player);
                if (!world.isRemote && !used.isEmpty() && !player.addItemStackToInventory(used)) {
                    player.dropItem(used, false);
                }
                return stack;
            }
        }
        return wear(stack, entity);
    }

    public int smokeColor() {
        return color;
    }

    public void applyInfluences(EntityPlayer player, DrugProperties properties) {
        for (DrugInfluence influence : influences) properties.inhale(influence.copy(), player, nicotineSaturation);
    }
}

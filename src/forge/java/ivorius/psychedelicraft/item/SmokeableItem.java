/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public final class SmokeableItem extends Item {
    private final int burnTime;
    private final int color;
    private final DrugInfluence[] influences;

    public SmokeableItem(int uses, int burnTime, int color, DrugInfluence... influences) {
        this.burnTime = burnTime;
        this.color = color;
        this.influences = influences.clone();
        setMaxStackSize(1);
        setMaxDamage(uses);
        addPropertyOverride(Psychedelicraft.id("using"), (stack, world, entity) -> PowderItem.useModel(stack, entity));
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return burnTime;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 25;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        DrugProperties properties = DrugCapability.get(player);
        if (properties == null || properties.isBreathingSmoke()) return new ActionResult<>(EnumActionResult.FAIL, stack);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) properties.startBreathingSmoke(player, color);
            if (!world.isRemote) {
                if (properties != null) applyInfluences(properties);
                if (player instanceof EntityPlayerMP) {
                    CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
                }
                player.addStat(StatList.getObjectUseStats(this));
            }
            if (player.capabilities.isCreativeMode) return stack;
        }
        if (!stack.isItemStackDamageable()) {
            stack.shrink(1);
            return stack;
        }
        stack.damageItem(1, entity);
        // The donor breaks at max damage. Vanilla 1.12 only breaks above it.
        if (!stack.isEmpty() && stack.getItemDamage() == stack.getMaxDamage()) {
            entity.renderBrokenItemStack(stack);
            stack.shrink(1);
            stack.setItemDamage(0);
            if (entity instanceof EntityPlayer) ((EntityPlayer) entity).addStat(StatList.getObjectBreakStats(this));
        }
        return stack;
    }

    public int smokeColor() {
        return color;
    }

    public void applyInfluences(DrugProperties properties) {
        for (DrugInfluence influence : influences) properties.addInfluence(influence.copy());
    }
}

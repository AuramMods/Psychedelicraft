/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on Jan 1 2023.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class BongItem extends SmokingItem {
    private final Map<Item, Function<ItemStack, DrugInfluence>> ingredients = new HashMap<>();
    private final boolean bubbles;

    public BongItem(int uses, int burnTime, boolean bubbles) {
        super(uses, 30, burnTime);
        this.bubbles = bubbles;
        addPropertyOverride(Psychedelicraft.id("filled"), (stack, world, entity) -> filledModel(stack, entity));
    }

    BongItem consumes(Item item, DrugInfluence influence) {
        return consumes(item, stack -> influence.copy());
    }

    BongItem consumes(Item item, Function<ItemStack, DrugInfluence> influence) {
        ingredients.put(item, influence);
        return this;
    }

    @Override
    boolean canDraw(EntityPlayer player) {
        return ingredientSlot(player.inventory) >= 0;
    }

    private float filledModel(ItemStack stack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return 0F;
        EntityPlayer player = (EntityPlayer) entity;
        // Other clients can see the active hand, but do not receive the whole inventory.
        return canDraw(player) || !player.isUser() && PowderItem.useModel(stack, player) > 0F ? 1F : 0F;
    }

    private int ingredientSlot(InventoryPlayer inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && ingredients.containsKey(stack.getItem())) return slot;
        }
        return -1;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
        if (bubbles && !entity.world.isRemote && entity.getRNG().nextInt(3) == 0) {
            entity.playSound(SoundEvents.BLOCK_LAVA_POP, 1, 1);
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return stack;
        EntityPlayer player = (EntityPlayer) entity;
        DrugProperties properties = DrugCapability.get(player);
        int slot = ingredientSlot(player.inventory);
        if (properties == null || slot < 0) return stack;
        if (world.isRemote) {
            properties.startBreathingSmoke(player, 0xffffff);
            return stack;
        }
        ItemStack used = player.inventory.decrStackSize(slot, 1);
        properties.addInfluence(ingredients.get(used.getItem()).apply(used));
        if (player instanceof EntityPlayerMP) CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, used);
        properties.startBreathingSmoke(player, 0xffffff);
        recordDraw(player, stack);
        return wear(stack, player);
    }
}

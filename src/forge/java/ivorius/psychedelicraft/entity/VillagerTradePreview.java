/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.network.Channel;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class VillagerTradePreview extends EntityAIBase {
    private static final int IDLE_TICKS = 40;
    private static final int MATCH_TICKS = 900;
    private static final int CYCLE_TICKS = 40;
    private static final double MAX_DISTANCE_SQ = 17;

    private final EntityVillager villager;
    @Nullable
    private EntityPlayer target;
    private ItemStack cost = ItemStack.EMPTY;
    private ItemStack shown = ItemStack.EMPTY;
    private List<ItemStack> matches = Collections.emptyList();
    private long endTime;
    private int remaining;
    private int index;
    private int cycle;

    VillagerTradePreview(EntityVillager villager) {
        this.villager = villager;
        setMutexBits(2);
    }

    @Override
    public boolean shouldExecute() {
        long time = villager.world.getWorldTime() % 24000L;
        if (!canPreview(time)) return false;
        double range = villager.getProfessionForge() == VillagerCareers.ADDICT && time >= 2000 && time < 9000
                ? Math.sqrt(MAX_DISTANCE_SQ) : 4;
        target = villager.world.getClosestPlayer(villager.posX, villager.posY, villager.posZ, range,
                entity -> entity.isEntityAlive() && !((EntityPlayer) entity).isSpectator()
                        && villager.getEntitySenses().canSee(entity));
        return target != null;
    }

    @Override
    public void startExecuting() {
        remaining = IDLE_TICKS;
        endTime = villager.world.getTotalWorldTime() + 400 + villager.getRNG().nextInt(1201);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return remaining > 0 && villager.world.getTotalWorldTime() <= endTime
                && canPreview(villager.world.getWorldTime() % 24000L)
                && target != null && target.isEntityAlive() && !target.isSpectator()
                && target.world == villager.world && villager.getDistanceSq(target) <= MAX_DISTANCE_SQ;
    }

    private boolean canPreview(long time) {
        return time >= 10 && time < 12000 && villager.isEntityAlive() && !villager.isChild()
                && !villager.isTrading() && !villager.isAIDisabled() && villager.hurtTime == 0
                && (villager.getProfessionForge() == VillagerCareers.DEALER || villager.getProfessionForge() == VillagerCareers.ADDICT);
    }

    @Override
    public void updateTask() {
        if (!shouldContinueExecuting()) {
            resetTask();
            return;
        }
        villager.getLookHelper().setLookPositionWithEntity(target, 30, villager.getVerticalFaceSpeed());
        ItemStack held = target.getHeldItemMainhand();
        if (!ItemStack.areItemsEqual(cost, held)) {
            cost = held.copy();
            matches = held.isEmpty() ? Collections.emptyList() : matchingResults(villager.getRecipes(target), held);
            index = cycle = 0;
            if (!matches.isEmpty()) {
                remaining = MATCH_TICKS;
                display(matches.get(0));
            }
        }
        if (matches.isEmpty()) {
            display(ItemStack.EMPTY);
            remaining = Math.min(remaining, IDLE_TICKS);
        } else if (matches.size() > 1 && ++cycle >= CYCLE_TICKS) {
            cycle = 0;
            index = (index + 1) % matches.size();
            display(matches.get(index));
        }
        remaining--;
    }

    static List<ItemStack> matchingResults(@Nullable MerchantRecipeList offers, ItemStack held) {
        if (offers == null) return Collections.emptyList();
        List<ItemStack> results = new ArrayList<>();
        for (MerchantRecipe offer : offers) {
            if (!offer.isRecipeDisabled() && (ItemStack.areItemsEqual(held, offer.getItemToBuy())
                    || ItemStack.areItemsEqual(held, offer.getSecondItemToBuy()))) {
                results.add(offer.getItemToSell().copy());
            }
        }
        return results;
    }

    @Override
    public void resetTask() {
        target = null;
        cost = ItemStack.EMPTY;
        matches = Collections.emptyList();
        index = cycle = 0;
        remaining = 0;
        display(ItemStack.EMPTY);
    }

    ItemStack shown() {
        return shown;
    }

    private void display(ItemStack item) {
        if (ItemStack.areItemStacksEqual(shown, item)) return;
        shown = item;
        Channel.showVillagerOffer(villager, item);
    }
}

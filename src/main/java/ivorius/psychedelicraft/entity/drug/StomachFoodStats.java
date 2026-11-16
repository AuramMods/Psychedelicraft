package ivorius.psychedelicraft.entity.drug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Applies drug exhaustion scaling while retaining the original food manager and its state. */
public final class StomachFoodStats extends FoodData {
    private final Player owner;
    private final FoodData delegate;

    private StomachFoodStats(Player owner, FoodData delegate) {
        this.owner = owner;
        this.delegate = delegate;
    }

    static void bind(Player player) {
        if (!(player.getFoodData() instanceof StomachFoodStats)) {
            player.foodData = new StomachFoodStats(player, player.getFoodData());
        }
    }

    public static void restoreSaturation(Player player, float amount) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()) return;
        changeSaturation(player, amount, true);
    }

    static float changeSaturation(Player player, float amount, boolean sync) {
        FoodData food = player.getFoodData();
        float current = food.getSaturationLevel();
        float updated = amount > 0 ? Math.max(current, Math.min(current + amount, food.getFoodLevel()))
                : current + amount <= 0.00001F ? 0F : current + amount;
        if (updated != current) {
            food.setSaturation(updated);
            if ((sync || updated == 0F) && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetHealthPacket(player.getHealth(), food.getFoodLevel(), updated));
            }
        }
        return updated;
    }

    @Override public void addExhaustion(float amount) {
        float scale = delegate.getSaturationLevel() > 0
                ? DrugCapability.get(owner).getMultiplier(Drug::saturationExhaustionModifier) : 1;
        delegate.addExhaustion(amount * scale);
    }

    @Override public void eat(int food, float saturation) { delegate.eat(food, saturation); }
    @Override public void eat(Item item, ItemStack stack) { delegate.eat(item, stack); }
    @Override public void eat(Item item, ItemStack stack, LivingEntity entity) { delegate.eat(item, stack, entity); }
    @Override public void tick(Player player) { delegate.tick(player); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { delegate.readAdditionalSaveData(tag); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { delegate.addAdditionalSaveData(tag); }
    @Override public int getFoodLevel() { return delegate.getFoodLevel(); }
    @Override public int getLastFoodLevel() { return delegate.getLastFoodLevel(); }
    @Override public boolean needsFood() { return delegate.needsFood(); }
    @Override public float getExhaustionLevel() { return delegate.getExhaustionLevel(); }
    @Override public float getSaturationLevel() { return delegate.getSaturationLevel(); }
    @Override public void setFoodLevel(int food) { delegate.setFoodLevel(food); }
    @Override public void setSaturation(float saturation) { delegate.setSaturation(saturation); }
    @Override public void setExhaustion(float exhaustion) { delegate.setExhaustion(exhaustion); }
}

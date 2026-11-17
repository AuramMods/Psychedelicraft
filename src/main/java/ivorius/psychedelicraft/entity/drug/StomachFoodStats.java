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
        ((FoodDataOwner)delegate).psychedelicraft$owner(owner);
    }

    public static void bind(Player player) {
        FoodData food=player.getFoodData();
        if(food instanceof StomachFoodStats stomach){
            if(stomach.owner==player)return;
            food=stomach.delegate;
        }
        player.foodData = new StomachFoodStats(player,food);
    }

    private Stomach stomach(){return DrugCapability.get(owner).getStomach();}
    int actualFoodLevel(){return delegate.getFoodLevel();}
    float actualSaturation(){return delegate.getSaturationLevel();}
    public static int packetFoodLevel(FoodData food){return food instanceof StomachFoodStats stomach?stomach.actualFoodLevel():food.getFoodLevel();}
    public static float packetSaturation(FoodData food){return food instanceof StomachFoodStats stomach?stomach.actualSaturation():food.getSaturationLevel();}

    public static void restoreSaturation(Player player, float amount) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()) return;
        changeSaturation(player, amount, true);
    }

    static float changeSaturation(Player player, float amount, boolean sync) {
        FoodData food = player.getFoodData();
        float current = packetSaturation(food);
        float updated = amount > 0 ? Math.max(current, Math.min(current + amount, packetFoodLevel(food)))
                : current + amount <= 0.00001F ? 0F : current + amount;
        if (updated != current) {
            food.setSaturation(updated);
            if ((sync || updated == 0F) && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetHealthPacket(player.getHealth(), packetFoodLevel(food), updated));
            }
        }
        return updated;
    }

    @Override public void addExhaustion(float amount) {
        delegate.addExhaustion(amount);
    }

    private void record(int food){if(!owner.level().isClientSide)stomach().recordFood(actualFoodLevel(),food);}
    @Override public void eat(int food, float saturation) { record(food);delegate.eat(food, saturation); }
    @Override public void eat(Item item, ItemStack stack) { var food=stack.getFoodProperties(null);if(food!=null)record(food.getNutrition());delegate.eat(item, stack); }
    @Override public void eat(Item item, ItemStack stack, LivingEntity entity) { var food=stack.getFoodProperties(entity);if(food!=null)record(food.getNutrition());delegate.eat(item, stack, entity); }
    @Override public void tick(Player player) { delegate.tick(player); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { delegate.readAdditionalSaveData(tag); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { delegate.addAdditionalSaveData(tag); }
    @Override public int getFoodLevel() { return stomach().perceivedFood(actualFoodLevel()); }
    @Override public int getLastFoodLevel() { return delegate.getLastFoodLevel(); }
    @Override public boolean needsFood() { return stomach().needsFood(delegate.needsFood()); }
    @Override public float getExhaustionLevel() { return delegate.getExhaustionLevel(); }
    @Override public float getSaturationLevel() { return stomach().perceivedSaturation(actualSaturation()); }
    @Override public void setFoodLevel(int food) { if(owner.level().isClientSide||!stomach().isLocked())delegate.setFoodLevel(food); }
    @Override public void setSaturation(float saturation) { delegate.setSaturation(saturation); }
    @Override public void setExhaustion(float exhaustion) { delegate.setExhaustion(exhaustion); }
}

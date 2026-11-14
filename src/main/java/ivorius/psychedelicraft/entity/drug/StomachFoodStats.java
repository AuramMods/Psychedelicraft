package ivorius.psychedelicraft.entity.drug;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.FoodStats;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

public final class StomachFoodStats extends FoodStats {
    private Stomach stomach;
    private boolean client;
    private float saturationExhaustion = 1F;

    void bind(Stomach state, boolean clientSide) {
        stomach = state;
        client = clientSide;
    }

    int actualFoodLevel() {
        return super.getFoodLevel();
    }

    float actualSaturation() {
        return super.getSaturationLevel();
    }

    public static int packetFoodLevel(FoodStats stats) {
        return stats instanceof StomachFoodStats ? ((StomachFoodStats) stats).actualFoodLevel() : stats.getFoodLevel();
    }

    public static float packetSaturation(FoodStats stats) {
        return stats instanceof StomachFoodStats ? ((StomachFoodStats) stats).actualSaturation() : stats.getSaturationLevel();
    }

    public static void restoreSaturation(EntityPlayer player, float amount) {
        if (player.world.isRemote || !player.isEntityAlive() || player.isSpectator()) return;
        changeSaturation(player, amount, true);
    }

    static float changeSaturation(EntityPlayer player, float amount, boolean sync) {
        FoodStats stats = player.getFoodStats();
        float current = packetSaturation(stats);
        float updated = amount > 0 ? Math.max(current, Math.min(current + amount, packetFoodLevel(stats)))
                : current + amount <= 0.00001F ? 0F : current + amount;
        if (updated == current) return current;
        try {
            SaturationField.VALUE.setFloat(stats, updated);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not update food saturation", e);
        }
        if ((sync || updated == 0F) && player instanceof EntityPlayerMP) ((EntityPlayerMP) player).setPlayerHealthUpdated();
        return updated;
    }

    private static final class SaturationField {
        // The vanilla saturation setter is stripped on dedicated servers.
        private static final Field VALUE = ReflectionHelper.findField(FoodStats.class, "foodSaturationLevel", "field_75125_b");
    }

    void setSaturationExhaustion(float multiplier) {
        saturationExhaustion = multiplier;
    }

    @Override
    public void addExhaustion(float exhaustion) {
        super.addExhaustion(actualSaturation() > 0F ? exhaustion * saturationExhaustion : exhaustion);
    }

    @Override
    public void addStats(int food, float saturationModifier) {
        if (stomach != null && !client) stomach.recordFood(actualFoodLevel(), food);
        super.addStats(food, saturationModifier);
    }

    @Override
    public boolean needFood() {
        return stomach == null ? super.needFood() : stomach.needsFood(super.needFood());
    }

    @Override
    public int getFoodLevel() {
        return stomach == null ? actualFoodLevel() : stomach.perceivedFood(actualFoodLevel());
    }

    @Override
    public float getSaturationLevel() {
        return stomach == null ? actualSaturation() : stomach.perceivedSaturation(actualSaturation());
    }

    @Override
    public void setFoodLevel(int level) {
        // Health packets carry raw food and update the client's projection baseline.
        if (client || stomach == null || !stomach.isLocked()) super.setFoodLevel(level);
    }
}

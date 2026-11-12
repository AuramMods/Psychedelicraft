package ivorius.psychedelicraft.entity.drug;

import net.minecraft.util.FoodStats;

public final class StomachFoodStats extends FoodStats {
    private Stomach stomach;
    private boolean client;

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

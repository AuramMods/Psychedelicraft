package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PaperBagItem;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.RandomSource;

public final class Stomach implements NbtSerialisable {
    private static final float EPSILON = 1.0E-5F;
    private static final int MAX_VOMITING_TICKS = 100;
    private static final int MAX_VOMIT_COUNT = 17;

    @Nullable
    private HungerLock lock;
    private float overeating;
    private int vomitCount;
    private int vomitingTicks;
    private boolean dirty;

    public boolean isLocked() {
        return lock != null;
    }

    public boolean needsFood(boolean vanilla) {
        return lock == null ? vanilla : !lock.full;
    }

    public int perceivedFood(int actual) {
        return lock == null ? actual : (int) HungerLock.project(actual, lock.food, lock.rate);
    }

    public float perceivedSaturation(float actual) {
        return lock == null ? actual : HungerLock.project(actual, lock.saturation, lock.rate);
    }

    void recordFood(int actual, int food) {
        if (lock != null && !lock.full && food > 0 && actual + food > 20) {
            overeating += food / 8F;
            dirty = true;
        }
    }

    void bind(Player player) { StomachFoodStats.bind(player); }

    public void tick(Player player, float hungerSuppression) {
        bind(player);
        if (!player.level().isClientSide && player.getFoodData() instanceof StomachFoodStats) {
            StomachFoodStats food = (StomachFoodStats) player.getFoodData();
            updateHunger(food.actualFoodLevel(), food.actualSaturation(), hungerSuppression);
        }
        if (player.level().isClientSide || !player.isAlive()) return;
        if (overeating >= 10) vomit(player);
        if (vomitingTicks > 0) {
            vomitingTicks--;
            RandomSource random = player.getRandom();
            if (player.tickCount % (1 + random.nextInt(3)) == 0) {
                int count = (int) (random.nextFloat() * (vomitingTicks / 2));
                for (int i = 0; i < count; i++) {
                    ItemEntity expelled = player.drop(new ItemStack(PSItems.VOMIT.get()), true, true);
                    if (expelled != null) expelled.setNeverPickUp();
                    playVomitSound(player);
                }
                vomitingTicks -= count;
            }
            if (vomitingTicks == 0) dirty = true;
        }
    }

    public void updateHunger(int food, float saturation, float modifier) {
        float rate = Mth.clamp(modifier, -1, 1);
        if (Math.abs(rate) <= EPSILON) {
            if (lock != null) {
                lock = null;
                dirty = true;
            }
            return;
        }
        if (lock == null) {
            lock = new HungerLock(food, saturation, rate > 0, rate);
            dirty = true;
        }
        lock.rate = rate;
        float newFood = rate > 0 ? Math.max(lock.food, food) : Math.min(lock.food, food);
        float newSaturation = rate > 0 ? Math.max(lock.saturation, saturation) : Math.min(lock.saturation, saturation);
        dirty |= lock.food != newFood || lock.saturation != newSaturation;
        lock.food = newFood;
        lock.saturation = newSaturation;
    }

    public void vomit(Player player) {
        if (player.level().isClientSide || !player.isAlive()) return;
        ItemStack bag = player.getOffhandItem();
        if (bag.getItem() == PSItems.PAPER_BAG.get() && PaperBagItem.getContents(bag).isEmpty()) {
            ItemStack filled = new ItemStack(PSItems.BAG_O_VOMIT.get());
            if(!player.getAbilities().instabuild&&bag.getCount()==1)player.setItemInHand(InteractionHand.OFF_HAND,filled);
            else if(!PaperBagItem.offer(player,filled))return;
            if(!player.getAbilities().instabuild)bag.shrink(1);
            playVomitSound(player);
        } else {
            vomitingTicks = 10 + player.getRandom().nextInt(MAX_VOMITING_TICKS - 9);
            if (vomitCount < MAX_VOMIT_COUNT) vomitCount++;
            if (vomitCount >= MAX_VOMIT_COUNT) {
                player.hurt(PSDamageTypes.of(player,PSDamageTypes.OVER_EATING), Integer.MAX_VALUE);
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
        overeating = 0;
        dirty = true;
    }

    private static void playVomitSound(Player player) {
        RandomSource random = player.getRandom();
        player.level().playSound(null,player,SoundEvents.VILLAGER_DEATH,player.getSoundSource(),
                1F,0.5F+(random.nextFloat()-random.nextFloat())*.25F);
    }

    boolean takeDirty() {
        boolean changed = dirty;
        dirty = false;
        return changed;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        if (lock != null) tag.put("hunger", lock.toNbt());
        tag.putFloat("overeating", overeating);
        tag.putInt("vomitCount", vomitCount);
        tag.putInt("vomitingTicks", vomitingTicks);
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        lock = tag.contains("hunger", 10) ? HungerLock.fromNbt(tag.getCompound("hunger")) : null;
        float saved = tag.getFloat("overeating");
        overeating = Float.isFinite(saved) ? Math.max(0, saved) : 0;
        vomitCount = Mth.clamp(tag.getInt("vomitCount"), 0, MAX_VOMIT_COUNT);
        vomitingTicks = Mth.clamp(tag.getInt("vomitingTicks"), 0, MAX_VOMITING_TICKS);
        dirty = false;
    }

    private static final class HungerLock {
        private float food;
        private float saturation;
        private final boolean full;
        private float rate;

        private HungerLock(float food, float saturation, boolean full, float rate) {
            this.food = food;
            this.saturation = saturation;
            this.full = full;
            this.rate = rate;
        }

        private static float project(float actual, float anchor, float rate) {
            anchor = rate > 0 ? Math.max(anchor, actual) : Math.min(anchor, actual);
            return Math.max(0, actual + (anchor - actual) * Math.abs(rate));
        }

        private CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("food", food);
            tag.putFloat("saturation", saturation);
            tag.putBoolean("full", full);
            tag.putFloat("rate", rate);
            return tag;
        }

        @Nullable
        private static HungerLock fromNbt(CompoundTag tag) {
            float food = tag.getFloat("food");
            float saturation = tag.getFloat("saturation");
            float rate = tag.getFloat("rate");
            if (!Float.isFinite(food) || !Float.isFinite(saturation) || !Float.isFinite(rate)) return null;
            return new HungerLock(Mth.clamp(food, 0, 20), Mth.clamp(saturation, 0, 20),
                    tag.getBoolean("full"), Mth.clamp(rate, -1, 1));
        }
    }
}

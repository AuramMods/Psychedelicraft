package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PaperBagItem;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.Random;

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

    boolean isLocked() {
        return lock != null;
    }

    boolean needsFood(boolean vanilla) {
        return lock == null ? vanilla : !lock.full;
    }

    int perceivedFood(int actual) {
        return lock == null ? actual : (int) HungerLock.project(actual, lock.food, lock.rate);
    }

    float perceivedSaturation(float actual) {
        return lock == null ? actual : HungerLock.project(actual, lock.saturation, lock.rate);
    }

    void recordFood(int actual, int food) {
        if (lock != null && !lock.full && food > 0 && actual + food > 20) {
            overeating += food / 8F;
            dirty = true;
        }
    }

    void bind(EntityPlayer player) {
        if (player.getFoodStats() instanceof StomachFoodStats) {
            ((StomachFoodStats) player.getFoodStats()).bind(this, player.world.isRemote);
        }
    }

    void tick(EntityPlayer player, float hungerSuppression) {
        bind(player);
        if (player.getFoodStats() instanceof StomachFoodStats) {
            StomachFoodStats food = (StomachFoodStats) player.getFoodStats();
            updateHunger(food.actualFoodLevel(), food.actualSaturation(), hungerSuppression);
        }
        if (player.world.isRemote || !player.isEntityAlive()) return;
        if (overeating >= 10) vomit(player);
        if (vomitingTicks > 0) {
            vomitingTicks--;
            Random random = player.getRNG();
            if (player.ticksExisted % (1 + random.nextInt(3)) == 0) {
                int count = (int) (random.nextFloat() * (vomitingTicks / 2));
                for (int i = 0; i < count; i++) {
                    EntityItem expelled = player.dropItem(new ItemStack(PSItems.VOMIT), true, true);
                    if (expelled != null) expelled.setInfinitePickupDelay();
                    playVomitSound(player);
                }
                vomitingTicks -= count;
            }
            if (vomitingTicks == 0) dirty = true;
        }
    }

    void updateHunger(int food, float saturation, float modifier) {
        float rate = MathHelper.clamp(modifier, -1, 1);
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

    public void vomit(EntityPlayer player) {
        if (player.world.isRemote || !player.isEntityAlive()) return;
        ItemStack bag = player.getHeldItemOffhand();
        if (bag.getItem() == PSItems.PAPER_BAG && PaperBagItem.getContents(bag).isEmpty()) {
            playVomitSound(player);
            if (!player.isCreative()) bag.shrink(1);
            ItemStack filled = new ItemStack(PSItems.BAG_O_VOMIT);
            if (bag.isEmpty()) player.setHeldItem(EnumHand.OFF_HAND, filled);
            else ItemHandlerHelper.giveItemToPlayer(player, filled);
        } else {
            vomitingTicks = 10 + player.getRNG().nextInt(MAX_VOMITING_TICKS - 9);
            if (vomitCount < MAX_VOMIT_COUNT) vomitCount++;
            if (vomitCount >= MAX_VOMIT_COUNT) {
                player.attackEntityFrom(PSDamageTypes.OVER_EATING, Integer.MAX_VALUE);
            }
        }
        player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 100, 1));
        overeating = 0;
        dirty = true;
    }

    private static void playVomitSound(EntityPlayer player) {
        Random random = player.getRNG();
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_VILLAGER_DEATH,
                player.getSoundCategory(), 1, 0.5F + (random.nextFloat() - random.nextFloat()) * 0.25F);
    }

    boolean takeDirty() {
        boolean changed = dirty;
        dirty = false;
        return changed;
    }

    @Override
    public void toNbt(NBTTagCompound tag) {
        if (lock != null) tag.setTag("hunger", lock.toNbt());
        tag.setFloat("overeating", overeating);
        tag.setInteger("vomitCount", vomitCount);
        tag.setInteger("vomitingTicks", vomitingTicks);
    }

    @Override
    public void fromNbt(NBTTagCompound tag) {
        lock = tag.hasKey("hunger", 10) ? HungerLock.fromNbt(tag.getCompoundTag("hunger")) : null;
        float saved = tag.getFloat("overeating");
        overeating = Float.isFinite(saved) ? Math.max(0, saved) : 0;
        vomitCount = MathHelper.clamp(tag.getInteger("vomitCount"), 0, MAX_VOMIT_COUNT);
        vomitingTicks = MathHelper.clamp(tag.getInteger("vomitingTicks"), 0, MAX_VOMITING_TICKS);
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

        private NBTTagCompound toNbt() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setFloat("food", food);
            tag.setFloat("saturation", saturation);
            tag.setBoolean("full", full);
            tag.setFloat("rate", rate);
            return tag;
        }

        @Nullable
        private static HungerLock fromNbt(NBTTagCompound tag) {
            float food = tag.getFloat("food");
            float saturation = tag.getFloat("saturation");
            float rate = tag.getFloat("rate");
            if (!Float.isFinite(food) || !Float.isFinite(saturation) || !Float.isFinite(rate)) return null;
            return new HungerLock(MathHelper.clamp(food, 0, 20), MathHelper.clamp(saturation, 0, 20),
                    tag.getBoolean("full"), MathHelper.clamp(rate, -1, 1));
        }
    }
}

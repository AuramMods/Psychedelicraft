/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import ivorius.psychedelicraft.entity.drug.type.SleepDeprivationDrug;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;

public final class DrugProperties implements NbtSerialisable {
    private final Map<DrugType, Drug> drugs = new LinkedHashMap<>();
    private final List<DrugInfluence> influences = new ArrayList<>();
    private final Stomach stomach = new Stomach();
    private final DrugSounds sounds = new DrugSounds();
    private final PeyoteFlight peyoteFlight = new PeyoteFlight();
    private boolean dirty;
    private boolean wakePending;
    private int smokeTicks;
    private int smokeColor;

    public void addToDrug(DrugType type, double value) {
        drugs.computeIfAbsent(type, DrugType::create).addToDesiredValue(value);
        dirty = true;
    }

    public void setDrugValue(DrugType type, double value) {
        drugs.computeIfAbsent(type, DrugType::create).setDesiredValue(value);
        dirty = true;
    }

    public void setDrugLocked(DrugType type, boolean locked) {
        drugs.computeIfAbsent(type, DrugType::create).setLocked(locked);
        dirty = true;
    }

    public void addToHarmonium(double value, int color) {
        addToDrug(DrugType.HARMONIUM, value);
        ((HarmoniumDrug) drugs.get(DrugType.HARMONIUM)).mixColor(value, color);
    }

    @Nullable
    public HarmoniumDrug getHarmonium() {
        return (HarmoniumDrug) drugs.get(DrugType.HARMONIUM);
    }

    public void addInfluence(DrugInfluence influence) {
        influences.add(influence);
        dirty = true;
    }

    public void inhale(DrugInfluence influence, EntityPlayer player, float nicotineSaturation) {
        addInfluence(influence);
        if (influence.drugType() == DrugType.TOBACCO) StomachFoodStats.restoreSaturation(player, nicotineSaturation);
    }

    public void tick(int age) {
        tick(age, null);
    }

    public void tick(int age, @Nullable EntityPlayer player) {
        if (player != null && wakePending && !player.isPlayerSleeping()) {
            wakePending = false;
            onAwoken(player);
        }
        if (player != null && player.world.getGameRules().getBoolean(SleepDeprivationDrug.GAME_RULE)) {
            if (!drugs.containsKey(DrugType.SLEEP_DEPRIVATION)) addToDrug(DrugType.SLEEP_DEPRIVATION, 0);
            if (age % 100 == 0) dirty = true;
        }
        if (age % 5 == 0 && influences.removeIf(influence -> influence.update(this))) {
            dirty = true;
        }
        for (Drug drug : drugs.values()) {
            drug.update(this, player);
        }
        if (player != null) {
            if (!player.world.isRemote) peyoteFlight.tick(player, getActiveValue(DrugType.PEYOTE));
            stomach.tick(player, getModifier(Drug::hungerSuppression));
            if (player.getFoodStats() instanceof StomachFoodStats) {
                ((StomachFoodStats) player.getFoodStats()).setSaturationExhaustion(getMultiplier(Drug::saturationExhaustionModifier));
            }
            dirty |= stomach.takeDirty();
            sounds.tick(this, player);
            if (player.isEntityAlive()) applyInvoluntaryActions(player);
        }
        if (smokeTicks > 0) {
            smokeTicks--;
            if (smokeTicks > 10 && player != null && !player.world.isRemote) SmokeEffects.exhale(player, smokeColor);
        }
    }

    private void applyInvoluntaryActions(EntityPlayer player) {
        Random random = player.getRNG();
        if (player.world.isRemote) {
            float jumpChance = getModifier(Drug::randomJumpChance);
            if (player.onGround && jumpChance > 0F && random.nextFloat() < jumpChance) player.jump();

            float swingChance = getModifier(Drug::randomSwingChance);
            if (!player.isSwingInProgress && swingChance > 0F && random.nextFloat() < swingChance) {
                player.swingArm(EnumHand.MAIN_HAND);
            }
        } else {
            float drowsiness = getModifier(Drug::drowsiness);
            if (drowsiness > 0F && random.nextFloat() < drowsiness) player.addExhaustion(0.05F);
        }
    }

    public boolean isBreathingSmoke() {
        return smokeTicks > 0;
    }

    public void startBreathingSmoke(EntityPlayer player, int color) {
        smokeTicks = 20 + player.getRNG().nextInt(10);
        smokeColor = color;
        dirty = true;
        SmokeEffects.playBreath(player);
    }

    public Stomach getStomach() {
        return stomach;
    }

    public float heartbeatPulse(float partialTicks) {
        return sounds.pulse(partialTicks);
    }

    public boolean isTripping() {
        return getModifier(Drug::movementHallucinationStrength) + getModifier(Drug::contextualHallucinationStrength)
                + getModifier(Drug::colorHallucinationStrength) > 0.7F;
    }

    public double getActiveValue(DrugType type) {
        Drug drug = drugs.get(type);
        return drug == null ? 0 : drug.getActiveValue();
    }

    public void onAwoken() {
        onAwoken(null);
    }

    void queueWakeUp() {
        wakePending = true;
    }

    private void onAwoken(@Nullable EntityPlayer player) {
        // A hangover can create a new effect while the current effects wake up.
        for (Drug drug : new ArrayList<>(drugs.values())) drug.onWakeUp(this, player);
        influences.clear();
        dirty = true;
    }

    @Nullable
    public String sleepWarning() {
        for (Drug drug : drugs.values()) {
            String warning = drug.sleepWarning();
            if (warning != null) return warning;
        }
        return null;
    }

    public float getMultiplier(Drug.Modifier modifier) {
        float value = 1;
        for (Drug drug : drugs.values()) value *= modifier.get(drug);
        return value;
    }

    public float getModifier(Drug.Modifier modifier) {
        float value = 0;
        for (Drug drug : drugs.values()) {
            value += modifier.get(drug);
        }
        return value;
    }

    public float getColorIntensification(float strength) {
        return blendStrength(strength, Drug::superSaturationHallucinationStrength);
    }

    public float getDesaturation(float strength) {
        return blendStrength(strength, Drug::desaturationHallucinationStrength);
    }

    public float blendStrength(float strength, Drug.Modifier modifier) {
        for (Drug drug : drugs.values()) {
            strength += (1 - strength) * modifier.get(drug);
        }
        return strength;
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean takeDirty() {
        boolean changed = dirty;
        dirty = false;
        return changed;
    }

    @Override
    public void toNbt(NBTTagCompound tag) {
        NBTTagCompound drugData = new NBTTagCompound();
        drugs.forEach((type, drug) -> drugData.setTag(type.id(), drug.toNbt()));
        tag.setTag("Drugs", drugData);

        NBTTagList pending = new NBTTagList();
        influences.forEach(influence -> pending.appendTag(influence.toNbt()));
        tag.setTag("drugInfluences", pending);
        tag.setTag("stomach", stomach.toNbt());
        if (peyoteFlight.isGranted()) tag.setBoolean("peyoteFlightGranted", true);
        if (smokeTicks > 0) {
            tag.setInteger("smokeTicks", smokeTicks);
            tag.setInteger("smokeColor", smokeColor);
        }
    }

    @Override
    public void fromNbt(NBTTagCompound tag) {
        drugs.clear();
        NBTTagCompound drugData = tag.getCompoundTag("Drugs");
        for (String id : drugData.getKeySet()) {
            DrugType.byId(id).ifPresent(type -> {
                Drug drug = type.create();
                drug.fromNbt(drugData.getCompoundTag(id));
                drugs.put(type, drug);
            });
        }

        influences.clear();
        NBTTagList pending = tag.getTagList("drugInfluences", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pending.tagCount(); i++) {
            DrugInfluence.loadFromNbt(pending.getCompoundTagAt(i)).ifPresent(influences::add);
        }
        stomach.fromNbt(tag.getCompoundTag("stomach"));
        peyoteFlight.restoreGrant(tag.getBoolean("peyoteFlightGranted"));
        smokeTicks = MathHelper.clamp(tag.getInteger("smokeTicks"), 0, 29);
        smokeColor = tag.getInteger("smokeColor") & 0xFFFFFF;
        dirty = false;
    }
}

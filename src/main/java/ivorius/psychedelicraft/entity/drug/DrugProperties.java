/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */
package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DrugProperties implements NbtSerialisable {
    private final Map<DrugType, Drug> drugs = new LinkedHashMap<>();
    private final List<DrugInfluence> influences = new ArrayList<>();
    private final Stomach stomach = new Stomach();
    private boolean dirty;
    private boolean wakePending;
    private int smokeTicks,smokeColor;
    private final PeyoteFlight peyoteFlight = new PeyoteFlight();

    public void addToDrug(DrugType type, double value) {
        drugs.computeIfAbsent(type, DrugType::create).addToDesiredValue(value);
        dirty = true;
    }

    public void addInfluence(DrugInfluence influence) {
        influences.add(influence);
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
        getHarmonium().mixColor(value, color);
    }

    public HarmoniumDrug getHarmonium() { return (HarmoniumDrug) drugs.get(DrugType.HARMONIUM); }

    public void inhale(DrugInfluence influence, Player player, float nicotineSaturation) {
        addInfluence(influence);
        if (influence.drugType() == DrugType.TOBACCO) StomachFoodStats.restoreSaturation(player, nicotineSaturation);
    }

    public boolean isBreathingSmoke(){return smokeTicks>0;}
    public void startBreathingSmoke(Player player,int color){
        smokeTicks=20+player.getRandom().nextInt(10);smokeColor=color&0xffffff;dirty=true;SmokeEffects.playBreath(player);
    }

    public Stomach getStomach(){return stomach;}

    public boolean isTripping(){return getModifier(Drug::movementHallucinationStrength)+getModifier(Drug::contextualHallucinationStrength)+getModifier(Drug::colorHallucinationStrength)>.7F;}

    public double getActiveValue(DrugType type) {
        Drug drug = drugs.get(type);
        return drug == null ? 0 : drug.getActiveValue();
    }

    public float getModifier(Drug.Modifier modifier) {
        float total = 0;
        for (Drug drug : drugs.values()) total += modifier.get(drug);
        return total;
    }

    public float getMultiplier(Drug.Modifier modifier) {
        float total = 1;
        for (Drug drug : drugs.values()) total *= modifier.get(drug);
        return total;
    }

    public float blendStrength(float strength, Drug.Modifier modifier) {
        for (Drug drug : drugs.values()) strength += (1 - strength) * modifier.get(drug);
        return strength;
    }

    public float getDesaturation(float strength) { return blendStrength(strength, Drug::desaturationHallucinationStrength); }
    public float getColorIntensification(float strength) { return blendStrength(strength, Drug::superSaturationHallucinationStrength); }

    public void tick(int age) { tick(age, null); }

    public void tick(int age, Player player) {
        if (player != null && wakePending && !player.isSleeping()) {
            wakePending = false;
            onAwoken(player);
        }
        if (player != null && player.level().getGameRules().getBoolean(Psychedelicraft.SLEEP_DEPRIVATION)) {
            if (!drugs.containsKey(DrugType.SLEEP_DEPRIVATION)) addToDrug(DrugType.SLEEP_DEPRIVATION, 0);
            if (age % 100 == 0) dirty = true;
        }
        if (age % 5 == 0 && influences.removeIf(influence -> influence.update(this))) dirty = true;
        for (Drug drug : drugs.values()) drug.update(this, player);
        if (player != null) {
            stomach.tick(player,getModifier(Drug::hungerSuppression));
            dirty |= stomach.takeDirty();
            if (!player.level().isClientSide) peyoteFlight.tick(player, getActiveValue(DrugType.PEYOTE));
            if (player.isAlive()) {
                if (player.level().isClientSide && player.isLocalPlayer()) {
                    if (player.onGround() && player.getRandom().nextFloat() < getModifier(Drug::randomJumpChance)) player.jumpFromGround();
                    if (!player.swinging && player.getRandom().nextFloat() < getModifier(Drug::randomSwingChance)) player.swing(InteractionHand.MAIN_HAND);
                } else if (!player.level().isClientSide && player.getRandom().nextFloat() < getModifier(Drug::drowsiness)) {
                    player.causeFoodExhaustion(0.05F);
                }
            }
        }
        if(smokeTicks>0){
            smokeTicks--;
            if(smokeTicks>10&&player!=null&&!player.level().isClientSide)SmokeEffects.exhale(player,smokeColor);
        }
    }

    void queueWakeUp() { wakePending = true; }
    public void onAwoken() { onAwoken(null); }
    private void onAwoken(Player player) {
        for (Drug drug : new ArrayList<>(drugs.values())) drug.onWakeUp(this, player);
        influences.clear();
        dirty = true;
    }

    public String sleepWarning() {
        for (Drug drug : drugs.values()) {
            String warning = drug.sleepWarning();
            if (warning != null) return warning;
        }
        return null;
    }

    public void markDirty() { dirty = true; }

    public boolean takeDirty() {
        boolean changed = dirty;
        dirty = false;
        return changed;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        CompoundTag effects = new CompoundTag();
        drugs.forEach((type, drug) -> effects.put(type.id(), drug.toNbt()));
        tag.put("Drugs", effects);
        ListTag pending = new ListTag();
        influences.forEach(influence -> pending.add(influence.toNbt()));
        tag.put("drugInfluences", pending);
        tag.put("stomach",stomach.toNbt());
        if (peyoteFlight.isGranted()) tag.putBoolean("peyoteFlightGranted", true);
        if(smokeTicks>0){tag.putInt("smokeTicks",smokeTicks);tag.putInt("smokeColor",smokeColor);}
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        drugs.clear();
        CompoundTag effects = tag.getCompound("Drugs");
        for (String id : effects.getAllKeys()) {
            DrugType.byId(id).ifPresent(type -> {
                Drug drug = type.create();
                drug.fromNbt(effects.getCompound(id));
                drugs.put(type, drug);
            });
        }
        influences.clear();
        ListTag pending = tag.getList("drugInfluences", Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            DrugInfluence.loadFromNbt(pending.getCompound(i)).ifPresent(influences::add);
        }
        stomach.fromNbt(tag.getCompound("stomach"));
        peyoteFlight.restoreGrant(tag.getBoolean("peyoteFlightGranted"));
        smokeTicks=Math.max(0,Math.min(29,tag.getInt("smokeTicks")));smokeColor=tag.getInt("smokeColor")&0xffffff;
        dirty = false;
    }
}

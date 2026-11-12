/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.entity.drug.type.SleepDeprivationDrug;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public final class DrugProperties implements NbtSerialisable {
    private final Map<DrugType, Drug> drugs = new LinkedHashMap<>();
    private final List<DrugInfluence> influences = new ArrayList<>();
    private boolean dirty;
    private boolean wakePending;

    public void addToDrug(DrugType type, double value) {
        drugs.computeIfAbsent(type, DrugType::create).addToDesiredValue(value);
        dirty = true;
    }

    public void addInfluence(DrugInfluence influence) {
        influences.add(influence);
        dirty = true;
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
        for (Drug drug : drugs.values()) {
            strength += (1 - strength) * drug.superSaturationHallucinationStrength();
        }
        return strength;
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
        dirty = false;
    }
}

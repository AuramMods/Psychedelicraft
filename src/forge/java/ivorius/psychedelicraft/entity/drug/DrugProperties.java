/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public final class DrugProperties implements NbtSerialisable {
    private final Map<DrugType, Drug> drugs = new LinkedHashMap<>();
    private final List<DrugInfluence> influences = new ArrayList<>();
    private boolean dirty;

    public void addToDrug(DrugType type, double value) {
        drugs.computeIfAbsent(type, DrugType::create).addToDesiredValue(value);
        dirty = true;
    }

    public void addInfluence(DrugInfluence influence) {
        influences.add(influence);
        dirty = true;
    }

    public void tick(int age) {
        if (age % 5 == 0 && influences.removeIf(influence -> influence.update(this))) {
            dirty = true;
        }
        drugs.values().forEach(Drug::update);
    }

    public void onAwoken() {
        drugs.values().forEach(Drug::reset);
        influences.clear();
        dirty = true;
    }

    public float getModifier(Drug.Modifier modifier) {
        float value = 0;
        for (Drug drug : drugs.values()) {
            value += modifier.get(drug);
        }
        return value;
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

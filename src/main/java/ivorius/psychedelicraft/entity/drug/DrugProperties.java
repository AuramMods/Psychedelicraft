/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */
package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public double getActiveValue(DrugType type) {
        Drug drug = drugs.get(type);
        return drug == null ? 0 : drug.getActiveValue();
    }

    public float getModifier(Drug.Modifier modifier) {
        float total = 0;
        for (Drug drug : drugs.values()) total += modifier.get(drug);
        return total;
    }

    public void tick(int age, Player player) {
        if (age % 5 == 0 && influences.removeIf(influence -> influence.update(this))) dirty = true;
        for (Drug drug : drugs.values()) drug.update(this, player);
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
        dirty = false;
    }
}

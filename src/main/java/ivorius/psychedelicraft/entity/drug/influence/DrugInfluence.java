/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */
package ivorius.psychedelicraft.entity.drug.influence;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public final class DrugInfluence {
    public static final int INGESTED_DELAY = 15;
    private final DrugType drugType;
    private final double influenceSpeed;
    private final double influenceSpeedPlus;
    private int delay;
    private double remaining;

    public DrugInfluence(DrugType drugType, int delay, double speed, double speedPlus, double remaining) {
        this.drugType = drugType;
        this.delay = delay;
        influenceSpeed = speed;
        influenceSpeedPlus = speedPlus;
        this.remaining = remaining;
    }

    public boolean update(DrugProperties properties) {
        if (delay > 0) delay--;
        if (delay == 0 && remaining > 0) {
            double addition = Math.min(remaining, influenceSpeedPlus + remaining * influenceSpeed);
            properties.addToDrug(drugType, addition);
            remaining -= addition;
        }
        return remaining <= 0;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "default");
        tag.putString("drugType", drugType.id());
        tag.putInt("delay", delay);
        tag.putDouble("influenceSpeed", influenceSpeed);
        tag.putDouble("influenceSpeedPlus", influenceSpeedPlus);
        tag.putDouble("maxInfluence", remaining);
        return tag;
    }

    public static Optional<DrugInfluence> loadFromNbt(CompoundTag tag) {
        double speed = tag.getDouble("influenceSpeed");
        double speedPlus = tag.getDouble("influenceSpeedPlus");
        double remaining = tag.getDouble("maxInfluence");
        if (!Double.isFinite(speed) || speed < 0 || !Double.isFinite(speedPlus) || speedPlus < 0
                || speed + speedPlus <= 0 || !Double.isFinite(remaining) || remaining <= 0
                || !"default".equals(tag.getString("type"))) return Optional.empty();
        return DrugType.byId(tag.getString("drugType")).map(type -> new DrugInfluence(type,
                Math.max(0, tag.getInt("delay")), speed, speedPlus, remaining));
    }
}

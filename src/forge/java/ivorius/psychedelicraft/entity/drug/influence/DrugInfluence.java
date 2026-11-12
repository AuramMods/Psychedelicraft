/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.influence;

import java.util.Optional;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.nbt.NBTTagCompound;

public final class DrugInfluence {
    public static final int INGESTED_DELAY = 15;

    private final DrugType drugType;
    private final double influenceSpeed;
    private final double influenceSpeedPlus;
    private int delay;
    private double remaining;

    public DrugInfluence(DrugType drugType, int delay, double influenceSpeed, double influenceSpeedPlus, double remaining) {
        this.drugType = drugType;
        this.delay = delay;
        this.influenceSpeed = influenceSpeed;
        this.influenceSpeedPlus = influenceSpeedPlus;
        this.remaining = remaining;
    }

    public boolean update(DrugProperties properties) {
        if (delay > 0) {
            delay--;
        }
        if (delay == 0 && remaining > 0) {
            double addition = Math.min(remaining, influenceSpeedPlus + remaining * influenceSpeed);
            properties.addToDrug(drugType, addition);
            remaining -= addition;
        }
        return remaining <= 0;
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", "default");
        tag.setString("drugType", drugType.id());
        tag.setInteger("delay", delay);
        tag.setDouble("influenceSpeed", influenceSpeed);
        tag.setDouble("influenceSpeedPlus", influenceSpeedPlus);
        tag.setDouble("maxInfluence", remaining);
        return tag;
    }

    public static Optional<DrugInfluence> loadFromNbt(NBTTagCompound tag) {
        double speed = tag.getDouble("influenceSpeed");
        double speedPlus = tag.getDouble("influenceSpeedPlus");
        double remaining = tag.getDouble("maxInfluence");
        if (!"default".equals(tag.getString("type")) || !Double.isFinite(speed) || speed < 0
                || !Double.isFinite(speedPlus) || speedPlus < 0 || speed + speedPlus <= 0
                || !Double.isFinite(remaining) || remaining <= 0) {
            return Optional.empty();
        }
        int delay = Math.max(0, tag.getInteger("delay"));
        return DrugType.byId(tag.getString("drugType"))
                .map(type -> new DrugInfluence(type, delay, speed, speedPlus, remaining));
    }
}

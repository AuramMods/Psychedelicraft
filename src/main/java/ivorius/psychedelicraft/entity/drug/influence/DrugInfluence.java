/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.influence;

import java.util.Optional;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

public final class DrugInfluence {
    public static final int IMMEDIATE = 0;
    public static final int INGESTED_DELAY = 15;
    public static final int INHALED_DELAY = 20;
    public static final int CONTACT_DELAY = 30;
    public static final int METABOLISED_DELAY = 60;

    private final DrugType drugType;
    private final double influenceSpeed;
    private final double influenceSpeedPlus;
    private final int color;
    private int delay;
    private double remaining;

    public DrugInfluence(DrugType drugType, int delay, double influenceSpeed, double influenceSpeedPlus, double remaining) {
        this(drugType, delay, influenceSpeed, influenceSpeedPlus, remaining, -1);
    }

    private DrugInfluence(DrugType drugType, int delay, double influenceSpeed, double influenceSpeedPlus, double remaining, int color) {
        this.drugType = drugType;
        this.delay = delay;
        this.influenceSpeed = influenceSpeed;
        this.influenceSpeedPlus = influenceSpeedPlus;
        this.remaining = remaining;
        this.color = color;
    }

    public static DrugInfluence harmonium(int delay, double remaining, int color) {
        return new DrugInfluence(DrugType.HARMONIUM, delay, 0.04, 0.01, remaining, color & 0xffffff);
    }

    public boolean update(DrugProperties properties) {
        if (delay > 0) {
            delay--;
        }
        if (delay == 0 && remaining > 0) {
            double addition = Math.min(remaining, influenceSpeedPlus + remaining * influenceSpeed);
            if (color >= 0) properties.addToHarmonium(addition, color);
            else properties.addToDrug(drugType, addition);
            remaining -= addition;
        }
        return remaining <= 0;
    }

    public DrugInfluence copy() {
        return new DrugInfluence(drugType, delay, influenceSpeed, influenceSpeedPlus, remaining, color);
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", color < 0 ? "default" : "harmonium");
        if (color >= 0) tag.setInteger("color", color);
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
        if (!Double.isFinite(speed) || speed < 0
                || !Double.isFinite(speedPlus) || speedPlus < 0 || speed + speedPlus <= 0
                || !Double.isFinite(remaining) || remaining <= 0) {
            return Optional.empty();
        }
        int delay = Math.max(0, tag.getInteger("delay"));
        if ("harmonium".equals(tag.getString("type")) && DrugType.HARMONIUM.id().equals(tag.getString("drugType"))
                && tag.hasKey("color", Constants.NBT.TAG_INT)) {
            int color = tag.getInteger("color") & 0xffffff;
            return Optional.of(new DrugInfluence(DrugType.HARMONIUM, delay, speed, speedPlus, remaining, color));
        }
        if (!"default".equals(tag.getString("type"))) return Optional.empty();
        return DrugType.byId(tag.getString("drugType"))
                .map(type -> new DrugInfluence(type, delay, speed, speedPlus, remaining));
    }
}

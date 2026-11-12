/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.entity.drug.Drug;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

public class SimpleDrug implements Drug {
    private final double decreaseSpeed;
    private final double decreaseSpeedPlus;

    private double desired;
    private double active;
    private boolean locked;
    private int ticksActive;

    public SimpleDrug(double decreaseSpeed, double decreaseSpeedPlus) {
        this.decreaseSpeed = decreaseSpeed;
        this.decreaseSpeedPlus = decreaseSpeedPlus;
    }

    @Override
    public double getActiveValue() {
        return active;
    }

    protected final int ticksActive() {
        return ticksActive;
    }

    protected final float activeRamp(float start) {
        return MathHelper.clamp(((float) active - start) / (1F - start), 0F, 1F);
    }

    @Override
    public void setDesiredValue(double value) {
        desired = MathHelper.clamp(value, 0, 1);
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public void addToDesiredValue(double value) {
        if (!locked) {
            desired += value;
        }
    }

    protected final void scaleDesiredValue(double scale) {
        desired *= scale;
    }

    protected final void clearActiveValue() {
        if (!locked) active = 0;
    }

    @Override
    public void update() {
        ticksActive = active > 0 ? ticksActive + 1 : 0;
        if (!locked) {
            desired = desired * decreaseSpeed - decreaseSpeedPlus;
        }
        desired = MathHelper.clamp(desired, 0, 1);

        double interpolated = active + (desired - active) * 0.05;
        if (interpolated > desired) {
            active = Math.max(interpolated - 0.005, desired);
        } else {
            active = Math.min(interpolated + 0.005, desired);
        }
    }

    @Override
    public void reset() {
        if (!locked) {
            desired = 0;
        }
    }

    @Override
    public void toNbt(NBTTagCompound tag) {
        tag.setDouble("effect", desired);
        tag.setDouble("effectActive", active);
        tag.setBoolean("locked", locked);
        tag.setInteger("ticksActive", ticksActive);
    }

    @Override
    public void fromNbt(NBTTagCompound tag) {
        desired = tag.getDouble("effect");
        if (!Double.isFinite(desired)) {
            desired = 0;
        }
        double savedActive = tag.getDouble("effectActive");
        active = Double.isFinite(savedActive) ? MathHelper.clamp(savedActive, 0, 1) : 0;
        locked = tag.getBoolean("locked");
        ticksActive = Math.max(0, tag.getInteger("ticksActive"));
    }
}

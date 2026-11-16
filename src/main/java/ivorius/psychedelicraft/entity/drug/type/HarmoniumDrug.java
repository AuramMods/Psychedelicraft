/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.nbt.Tag;

public final class HarmoniumDrug extends SimpleDrug {
    private final float[] color = {1, 1, 1};

    public HarmoniumDrug() {
        super(1, 0.0003);
    }

    public void mixColor(double dose, int tint) {
        double weight = dose + (1 - dose) * (1 - getActiveValue());
        for (int i = 0; i < color.length; i++) {
            float component = (tint >> ((2 - i) * 8) & 255) / 255F;
            color[i] += (component - color[i]) * weight;
        }
    }

    public float[] color() {
        return color.clone();
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        for (int i = 0; i < color.length; i++) tag.putFloat("currentColor[" + i + "]", color[i]);
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        super.fromNbt(tag);
        for (int i = 0; i < color.length; i++) {
            String key = "currentColor[" + i + "]";
            float component = tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : 1F;
            color[i] = Float.isFinite(component) ? Mth.clamp(component, 0F, 1F) : 1F;
        }
    }
}

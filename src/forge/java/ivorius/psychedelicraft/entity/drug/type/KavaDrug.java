/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import net.minecraft.util.math.MathHelper;

public final class KavaDrug extends AlcoholDrug {
    @Override
    public float heartbeatVolume() {
        float strength = (MathHelper.clamp(ticksActive(), 50, 250) - 50) / 200F;
        return activeRamp(0.4F) * 1.2F * strength;
    }
}

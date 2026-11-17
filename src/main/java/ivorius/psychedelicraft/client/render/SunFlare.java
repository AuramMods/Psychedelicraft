/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on 15 Jan 2023. */
package ivorius.psychedelicraft.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class SunFlare {
    private float alpha;

    static Vec3 direction(float sunAngle, float yaw, float pitch, boolean front) {
        Vec3 direction = new Vec3(-Mth.sin(sunAngle), Mth.cos(sunAngle), 0)
                .yRot(yaw * Mth.DEG_TO_RAD).xRot(pitch * Mth.DEG_TO_RAD);
        return front ? new Vec3(-direction.x, direction.y, -direction.z) : direction;
    }

    static float visibility(Vec3 direction, int width, int height, float rain, boolean clearSky) {
        if (!clearSky || direction.z <= 0) return 0;
        return (float)Math.max(0, 1 - Math.hypot(direction.x * width, direction.y * height) / 170) * (1 - rain);
    }

    void tick(float target) {
        alpha += (target - alpha) * .1F;
        alpha += Mth.clamp(target - alpha, -.01F, .01F);
    }

    float alpha() { return alpha; }
}

/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on 19 April 2023. */
package ivorius.psychedelicraft.client.render;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

final class Eyelids {
    private int blinkTicks, heartbeatCooldown;
    private float previous, opacity;
    record Events(boolean warning, boolean heartbeat, float tiredness) {}

    Events tick(float drowsiness, int age, RandomSource random) {
        float tired = Math.max(0, drowsiness - .6F);
        previous = opacity;
        float target = blinkTicks > 0 ? tired * .9F + Mth.sin(age / 10F) : 0;
        opacity += Mth.clamp(target - opacity, -.03F, .03F);
        if (heartbeatCooldown > 0) heartbeatCooldown--;
        boolean beat = tired > .3F && opacity > .6F && heartbeatCooldown == 0;
        // The pinned LTS throttles this warning sound to one per second.
        if (beat) heartbeatCooldown = 20;
        boolean warning = false;
        if (tired < .2F) blinkTicks = Math.max(blinkTicks - 1, 0);
        else if (--blinkTicks <= 0 && (blinkTicks < -300 || random.nextFloat() < drowsiness)) {
            blinkTicks = (int)(300 + (random.nextDouble() - random.nextDouble()) * 200);
            warning = true;
        }
        return new Events(warning, beat, tired);
    }

    float opacity(float partial) { return Mth.clamp(Mth.lerp(partial, previous, opacity), 0, 1); }
}

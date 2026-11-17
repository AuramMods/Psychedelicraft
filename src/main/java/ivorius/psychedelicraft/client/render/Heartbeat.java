/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on 15 Jan 2023. */
package ivorius.psychedelicraft.client.render;

import net.minecraft.util.Mth;

/** Shared timing for the local heartbeat sound and its injury-overlay pulse. */
final class Heartbeat {
    private int delay;
    private float previous, pulse, target;

    boolean tick(float volume, float speedModifier) {
        previous = pulse;
        pulse += Mth.clamp(target - pulse, -.2F, .2F);
        target = Math.max(0, target - .02F);
        if (delay > 0) delay--;
        if (delay == 0 && volume > 0) {
            float speed = 1 + speedModifier;
            delay = speed <= 1 ? 1 : Mth.floor(35 / (speed - 1));
            target = 1;
            return true;
        }
        return false;
    }

    float pulse(float partial) { return Mth.lerp(partial, previous, pulse); }
}

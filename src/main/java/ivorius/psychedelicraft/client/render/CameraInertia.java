/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 4 Jan 2023.
 */

package ivorius.psychedelicraft.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

final class CameraInertia {
    private final Axis horizontal = new Axis();
    private final Axis vertical = new Axis();
    private float pendingX;
    private float pendingY;
    private float smoothedX;
    private float smoothedY;
    private float previousPartial;

    static float sensitivity(float setting) {
        float scale = setting * 0.6F + 0.2F;
        return scale * scale * scale * 8F;
    }

    void tick(float strength, float sensitivity) {
        if (strength > 0F) {
            float multiplier = Mth.clamp(strength, 0F, 1F) * sensitivity;
            smoothedX = horizontal.smooth(pendingX, multiplier);
            smoothedY = vertical.smooth(pendingY, multiplier);
        } else {
            horizontal.reset();
            vertical.reset();
            smoothedX = smoothedY = 0F;
        }
        pendingX = pendingY = previousPartial = 0F;
    }

    Vec2 sample(float rawX, float rawY, float partial) {
        pendingX += rawX;
        pendingY += rawY;
        float progress = Math.max(0F, partial - previousPartial);
        previousPartial = partial;
        return new Vec2(smoothedX * progress, smoothedY * progress);
    }

    private static final class Axis {
        private double input;
        private double delivered;
        private double latency;

        float smooth(float movement, float multiplier) {
            input += movement;
            double remaining = input - delivered;
            double nextLatency = latency + (remaining - latency) * 0.5;
            double direction = Math.signum(remaining);
            // 1.20.1 tracks the unscaled movement when estimating latency.
            if (direction * remaining > direction * latency) remaining = nextLatency;
            latency = nextLatency;
            double output = remaining * multiplier;
            delivered += output;
            return (float) output;
        }

        void reset() {
            input = delivered = latency = 0;
        }
    }
}

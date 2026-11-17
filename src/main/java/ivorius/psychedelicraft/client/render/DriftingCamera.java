/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.minecraft.util.RandomSource;

final class DriftingCamera {
    private Vec3 previousPosition = Vec3.ZERO;
    private Vec3 previousRotation = Vec3.ZERO;
    private Vec3 position = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 rotation = Vec3.ZERO;
    private double accelerationX;
    private double accelerationY;
    private double accelerationZ;
    private float intensity;

    void update(float strength, int age, double motionX, double motionZ, RandomSource random) {
        previousPosition = position;
        previousRotation = rotation;
        intensity = Mth.clamp(strength, 0F, 1F);
        float weight = strength * Mth.sin(age / 100F);
        if (weight == 0F) {
            velocity = Vec3.ZERO;
            accelerationX = accelerationY = accelerationZ = 0;
            position = position.scale(0.9);
            rotation = rotation.scale(0.9);
            return;
        }

        velocity = velocity.add(accelerationX - Mth.clamp(motionX * 0.001F, -0.2, 0.2),
                accelerationY - 0.03, accelerationZ - Mth.clamp(motionZ * 0.001F, -0.2, 0.2));
        double distance = position.lengthSqr();
        if (distance > 10) {
            accelerationX = accelerationZ = 0;
            accelerationY = -0.5F / distance - 0.0001F;
        } else {
            accelerationY = 0;
            if (random.nextFloat() < 0.02F) {
                accelerationX = Math.sin((random.nextFloat() - 0.5) * weight * 2 * Math.PI) * random.nextFloat() / 3F;
            } else if (random.nextFloat() < 0.02F) {
                accelerationZ = Math.cos((random.nextFloat() - 0.5) * weight * 2 * Math.PI) * random.nextFloat() / 3F;
            }
        }

        rotation = new Vec3(rotation.x % Math.PI + 0.001 * Mth.sin(age / 200F),
                rotation.y % Math.PI + 0.001 * Mth.sin(age / 300F),
                rotation.z % Math.PI + 0.001 * Mth.sin(age / 400F));
        position = position.add(velocity.scale(weight));
        if (position.y > 0) position = new Vec3(position.x, 0, position.z);
        velocity = velocity.scale(0.999 / Math.max(position.lengthSqr() / 10, 1));
    }

    Vec3 position(float partial) {
        return interpolate(previousPosition, position, partial);
    }

    Vec3 rotation(float partial) {
        return interpolate(previousRotation, rotation, partial);
    }

    boolean isOutOfBody() {
        return position.lengthSqr() * intensity * intensity > 0.25;
    }

    private Vec3 interpolate(Vec3 previous, Vec3 current, float partial) {
        return intensity == 0F ? Vec3.ZERO : previous.add(current.subtract(previous).scale(partial)).scale(intensity);
    }
}

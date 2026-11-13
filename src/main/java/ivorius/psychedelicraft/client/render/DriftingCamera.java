/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

final class DriftingCamera {
    private Vec3d previousPosition = Vec3d.ZERO;
    private Vec3d previousRotation = Vec3d.ZERO;
    private Vec3d position = Vec3d.ZERO;
    private Vec3d velocity = Vec3d.ZERO;
    private Vec3d rotation = Vec3d.ZERO;
    private double accelerationX;
    private double accelerationY;
    private double accelerationZ;
    private float intensity;

    void update(float strength, int age, double motionX, double motionZ, Random random) {
        previousPosition = position;
        previousRotation = rotation;
        intensity = MathHelper.clamp(strength, 0F, 1F);
        float weight = strength * MathHelper.sin(age / 100F);
        if (weight == 0F) {
            velocity = Vec3d.ZERO;
            accelerationX = accelerationY = accelerationZ = 0;
            position = position.scale(0.9);
            rotation = rotation.scale(0.9);
            return;
        }

        velocity = velocity.addVector(accelerationX - MathHelper.clamp(motionX * 0.001F, -0.2, 0.2),
                accelerationY - 0.03, accelerationZ - MathHelper.clamp(motionZ * 0.001F, -0.2, 0.2));
        double distance = position.lengthSquared();
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

        rotation = new Vec3d(rotation.x % Math.PI + 0.001 * MathHelper.sin(age / 200F),
                rotation.y % Math.PI + 0.001 * MathHelper.sin(age / 300F),
                rotation.z % Math.PI + 0.001 * MathHelper.sin(age / 400F));
        position = position.add(velocity.scale(weight));
        if (position.y > 0) position = new Vec3d(position.x, 0, position.z);
        velocity = velocity.scale(0.999 / Math.max(position.lengthSquared() / 10, 1));
    }

    Vec3d position(float partial) {
        return interpolate(previousPosition, position, partial);
    }

    Vec3d rotation(float partial) {
        return interpolate(previousRotation, rotation, partial);
    }

    boolean isOutOfBody() {
        return position.lengthSquared() * intensity * intensity > 0.25;
    }

    private Vec3d interpolate(Vec3d previous, Vec3d current, float partial) {
        return intensity == 0F ? Vec3d.ZERO : previous.add(current.subtract(previous).scale(partial)).scale(intensity);
    }
}

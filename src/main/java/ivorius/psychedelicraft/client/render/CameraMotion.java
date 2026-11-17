/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/** Accepted camera equations, independent of the renderer's matrix ownership. */
final class CameraMotion {
    private CameraMotion() {}

    static float noise(float tick) { return new Random((long)(tick * 1000)).nextFloat() - .5F; }
    static float vertical(float tick, float strength) {
        double sin = Math.sin(tick / 3F);
        return (float)(sin * sin) * strength * .1F;
    }

    static Matrix4f view(Matrix4f nativeView, Vec3 position, Vec3 rotation,
                         float wobble, float tremble, float tick, boolean mirrored) {
        if (position.equals(Vec3.ZERO) && rotation.equals(Vec3.ZERO) && wobble == 0 && tremble == 0)
            return new Matrix4f(nativeView);
        // World-space drift; screen-space wobble/tremor; then the native camera rotation.
        Matrix4f result = new Matrix4f(nativeView).translate((float)position.x, (float)position.y, (float)position.z)
                .rotateXYZ((float)rotation.x, (float)rotation.y, (float)rotation.z)
                .mul(new Matrix4f(nativeView).invert());
        if (mirrored) result.rotateY((float)-Math.PI);
        if (wobble > 0) {
            float base = 5 / (wobble * wobble + 5) - wobble * .04F;
            base *= base;
            float angle = (float)Math.toRadians(tick * 3);
            result.rotateY(angle).rotateZ(angle)
                    .scale(axis(base, wobble, tick, 150), axis(base, wobble, tick, 170), axis(base, wobble, tick, 190))
                    .rotateZ(-angle).rotateY(-angle);
        }
        result.translate(noise(tick) * .05F * tremble, vertical(tick, tremble), 0);
        if (mirrored) result.rotateY((float)Math.PI);
        return result.mul(nativeView);
    }

    private static float axis(float base, float strength, float tick, float period) {
        return 1 / (base + strength * (float)Math.sin(tick / period * Math.PI) / 2);
    }
}

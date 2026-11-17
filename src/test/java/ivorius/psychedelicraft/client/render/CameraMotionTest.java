package ivorius.psychedelicraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class CameraMotionTest {
    @Test public void soberCameraPreservesNativeViewExactlyAndDoesNotMutateInput() {
        Matrix4f nativeView = new Matrix4f().rotateXYZ(.2F, 1.7F, -.3F);
        Matrix4f original = new Matrix4f(nativeView);
        assertEquals(original, CameraMotion.view(nativeView, Vec3.ZERO, Vec3.ZERO, 0, 0, 190.5F, false));
        CameraMotion.view(nativeView, new Vec3(.1, -.3, .2), new Vec3(.05, .02, -.1), .8F, .9F, 190.5F, true);
        assertEquals(original, nativeView);
    }

    @Test public void driftDisplacesWorldCoordinatesBeforeNativeCameraRotation() {
        Matrix4f nativeView = new Matrix4f().rotateXYZ(.6F, 1.1F, .2F);
        Matrix4f result = CameraMotion.view(nativeView, new Vec3(1, -2, 3), Vec3.ZERO, 0, 0, 0, false);
        Vector3f actual = result.transformPosition(new Vector3f(4, 5, 6));
        Vector3f expected = nativeView.transformPosition(new Vector3f(5, 3, 9));
        assertTrue(expected.equals(actual, .00001F));
    }

    @Test public void frontViewMirrorsScreenTremorAndStrongWobbleStaysInvertible() {
        float tick = 100.5F;
        Matrix4f first = CameraMotion.view(new Matrix4f(), Vec3.ZERO, Vec3.ZERO, 0, 1, tick, false);
        Matrix4f front = CameraMotion.view(new Matrix4f(), Vec3.ZERO, Vec3.ZERO, 0, 1, tick, true);
        assertEquals(-first.m30(), front.m30(), .000001F);
        assertEquals(first.m31(), front.m31(), .000001F);
        for (int t = 0; t < 2000; t++) {
            Matrix4f wobble = CameraMotion.view(new Matrix4f(), Vec3.ZERO, Vec3.ZERO, 1, 1, t, false);
            assertTrue(wobble.isFinite()); assertTrue(wobble.determinant() > 0);
        }
    }

    @Test public void nestedPoseScopesRestoreEveryCopiedTransformIncludingOverlayOffsets() {
        ModelPart head = new ModelPart(List.of(), Map.of()), hat = new ModelPart(List.of(), Map.of());
        head.xRot = .2F; head.yRot = .3F; head.zRot = .4F;
        hat.x = 9; hat.y = 8; hat.z = 7; hat.xScale = .75F; hat.yScale = .8F; hat.zScale = .9F;
        try (var outer = new PlayerPose(head, hat)) {
            head.zRot = 1; hat.copyFrom(head);
            try (var inner = new PlayerPose(head, hat)) { head.zRot = -1; hat.zRot = -1; }
            assertEquals(1, head.zRot, 0); assertEquals(1, hat.zRot, 0);
        }
        assertEquals(.2F, head.xRot, 0); assertEquals(.3F, head.yRot, 0); assertEquals(.4F, head.zRot, 0);
        assertEquals(9, hat.x, 0); assertEquals(8, hat.y, 0); assertEquals(7, hat.z, 0);
        assertEquals(.75F, hat.xScale, 0); assertEquals(.8F, hat.yScale, 0); assertEquals(.9F, hat.zScale, 0);
        assertEquals(0, hat.zRot, 0);
    }
}

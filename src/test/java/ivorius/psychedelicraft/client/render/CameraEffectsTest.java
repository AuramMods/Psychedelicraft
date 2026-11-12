package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CameraEffectsTest {
    @Test
    public void driftInterpolatesDisplacementAndStopsAtZeroStrength() {
        DriftingCamera camera = new DriftingCamera();
        Random noRandomAcceleration = new Random() {
            @Override
            public float nextFloat() {
                return 0.5F;
            }
        };
        camera.update(0.8F, 100, 0, 0, noRandomAcceleration);
        assertEquals(Vec3d.ZERO, camera.position(0F));
        assertEquals(camera.position(1F).y / 2, camera.position(0.5F).y, 0.000001);
        assertTrue(camera.position(1F).y < 0);
        assertFalse(camera.isOutOfBody());

        for (int tick = 101; tick <= 130; tick++) camera.update(0.8F, tick, 0, 0, noRandomAcceleration);
        assertTrue(camera.isOutOfBody());
        assertTrue(camera.rotation(1F).x > 0);
        camera.update(0F, 131, 0, 0, noRandomAcceleration);
        assertEquals(Vec3d.ZERO, camera.position(0.5F));
        assertEquals(Vec3d.ZERO, camera.rotation(0.5F));
        assertFalse(camera.isOutOfBody());
    }

    @Test
    public void mouseInputUsesTheNextTickAndResetsWhenTheEffectClears() {
        CameraInertia inertia = new CameraInertia();
        inertia.tick(1F, CameraInertia.sensitivity(0.5F));
        assertEquals(0F, inertia.sample(12F, -8F, 0.5F).x, 0F);

        inertia.tick(1F, CameraInertia.sensitivity(0.5F));
        Vec2f quarter = inertia.sample(0, 0, 0.25F);
        Vec2f half = inertia.sample(0, 0, 0.75F);
        assertTrue(quarter.x > 0F);
        assertTrue(quarter.y < 0F);
        assertEquals(2F * quarter.x, half.x, 0.000001F);
        assertEquals(0F, inertia.sample(0, 0, 0.75F).x, 0F);

        inertia.tick(0F, 1F);
        inertia.tick(1F, 1F);
        assertEquals(0F, inertia.sample(0, 0, 0.5F).x, 0F);
    }

    @Test
    public void trembleStrengthsStackThroughTheRemainingFraction() {
        DrugProperties drugs = new DrugProperties();
        set(drugs, DrugType.LSD, 0.9);
        set(drugs, DrugType.CAFFEINE, 0.9);
        set(drugs, DrugType.CANNABIS, 0.25);
        for (int tick = 1; tick <= 100; tick++) drugs.tick(tick);

        assertEquals(0.75F, drugs.blendStrength(0F, Drug::viewTrembleStrength), 0.00001F);
        assertEquals(0.9375F, drugs.blendStrength(0F, Drug::handTrembleStrength), 0.00001F);
        assertEquals(0.72F, drugs.getModifier(Drug::weightlessness), 0.00001F);
        assertEquals(2F, drugs.getModifier(Drug::headMotionInertia), 0.00001F);
    }

    private static void set(DrugProperties drugs, DrugType type, double value) {
        drugs.setDrugLocked(type, true);
        drugs.setDrugValue(type, value);
    }
}

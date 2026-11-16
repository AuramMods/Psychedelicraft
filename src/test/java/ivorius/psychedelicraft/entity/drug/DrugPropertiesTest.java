package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import org.junit.Test;

import static org.junit.Assert.*;

public class DrugPropertiesTest {
    @Test
    public void pendingDoseAndActiveCurveSurviveSnapshotRoundTrip() {
        DrugProperties continuous = new DrugProperties();
        continuous.addInfluence(new DrugInfluence(DrugType.RED_SHROOMS, 15, 0.005, 0.003, 0.5));
        for (int tick = 0; tick < 100; tick++) continuous.tick(tick, null);
        DrugProperties restored = new DrugProperties();
        restored.fromNbt(continuous.toNbt());
        for (int tick = 100; tick < 3100; tick++) {
            continuous.tick(tick, null);
            restored.tick(tick, null);
            assertEquals(continuous.toNbt(), restored.toNbt());
        }
        assertEquals(0, restored.getActiveValue(DrugType.RED_SHROOMS), 0);
    }

    @Test
    public void overlappingDosesKeepTheAcceptedMushroomPeakAndAvoidanceCurve() {
        for (int doses = 1; doses <= 4; doses++) {
            DrugProperties properties = new DrugProperties();
            for (int dose = 0; dose < doses; dose++) {
                properties.addInfluence(new DrugInfluence(DrugType.BROWN_SHROOMS, 15, 0.005, 0.003, 0.5));
            }
            float peak = 0;
            for (int tick = 0; tick < 1000; tick++) {
                properties.tick(tick, null);
                peak = Math.max(peak, properties.getModifier(Drug::mobAvoidanceChance));
            }
            assertEquals(doses / 4F, peak, 0.003F);
        }
    }
}

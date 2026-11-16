package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HarmoniumDrugTest {
    @Test
    public void pendingColorAndDelaySurviveSaveAndRestore() {
        DrugProperties before = new DrugProperties();
        DrugInfluence pending = DrugInfluence.harmonium(20, 0.65, 0x3399ff);
        before.addInfluence(pending.copy());
        for (int tick = 5; tick <= 45; tick += 5) before.tick(tick);

        DrugProperties restored = new DrugProperties();
        restored.fromNbt(before.toNbt());
        for (int tick = 50; tick < 100; tick += 5) restored.tick(tick);
        assertNull(restored.getHarmonium());

        restored.tick(100);
        assertTrue(restored.getHarmonium().getActiveValue() > 0);
        assertArrayEquals(new float[]{0.2F, 0.6F, 1F}, restored.getHarmonium().color(), 0.00001F);
        assertEquals(20, pending.toNbt().getInt("delay"));
    }

    @Test
    public void newDyeBlendsIntoTheActiveColorAndKeepsTheMixedState() {
        DrugProperties before = new DrugProperties();
        before.addToHarmonium(0.8, 0xff0000);
        for (int tick = 1; tick <= 20; tick++) before.tick(tick);
        before.addToHarmonium(0.1, 0x0000ff);
        float[] mixed = before.getHarmonium().color();
        assertTrue(mixed[0] > 0 && mixed[0] < 1 && mixed[2] > 0 && mixed[2] < 1);

        DrugProperties restored = new DrugProperties();
        restored.fromNbt(before.toNbt());
        assertArrayEquals(mixed, restored.getHarmonium().color(), 0F);
        assertEquals(before.getHarmonium().getActiveValue(), restored.getHarmonium().getActiveValue(), 0D);
    }
}

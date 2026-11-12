package ivorius.psychedelicraft.client.render;

import java.util.Random;

import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HallucinationManagerTest {
    @Test
    public void inactivePlayerHasNoHallucinations() {
        HallucinationManager hallucinations = new HallucinationManager();
        update(new DrugProperties(), hallucinations, new Random(72), 1, 200);
        assertInactive(hallucinations);
    }

    @Test
    public void stackedEffectsCanFillThePoolAndFadeBackToZero() {
        DrugProperties properties = new DrugProperties();
        properties.addToDrug(DrugType.BROWN_SHROOMS, 1);
        properties.addToDrug(DrugType.RED_SHROOMS, 1);
        HallucinationManager hallucinations = new HallucinationManager();
        Random random = new Random(72);
        update(properties, hallucinations, random, 1, 200);

        for (Effect effect : Effect.values()) {
            assertTrue(effect.name(), hallucinations.strength(effect) > 0);
        }

        properties.onAwoken();
        update(properties, hallucinations, random, 201, 700);
        assertInactive(hallucinations);
    }

    private static void assertInactive(HallucinationManager hallucinations) {
        for (Effect effect : Effect.values()) {
            assertEquals(effect.name(), 0F, hallucinations.strength(effect), 0F);
        }
    }

    private static void update(DrugProperties properties, HallucinationManager hallucinations,
                               Random random, int from, int through) {
        for (int age = from; age <= through; age++) {
            properties.tick(age);
            hallucinations.update(properties, random, age);
        }
    }
}

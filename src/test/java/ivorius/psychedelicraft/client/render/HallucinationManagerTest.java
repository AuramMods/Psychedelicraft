package ivorius.psychedelicraft.client.render;

import java.util.Random;

import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HallucinationManagerTest {
    @Test
    public void inactivePlayerHasNoHallucinations() {
        HallucinationManager hallucinations = new HallucinationManager();
        update(new DrugProperties(), hallucinations, new Random(72), 1, 200);
        assertInactive(hallucinations);
        assertNull(hallucinations.bloomColor(null));
        assertNull(hallucinations.contrastColor(null));
        assertNull(hallucinations.pulseColor());
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
        assertNull(hallucinations.pulseColor());
    }

    @Test
    public void terrainPulsesUseTheMindColorAndClampTheirOpacity() {
        DrugProperties properties = new DrugProperties();
        properties.addToDrug(DrugType.BROWN_SHROOMS, 1);
        properties.addToDrug(DrugType.RED_SHROOMS, 1);
        HallucinationManager hallucinations = new HallucinationManager();
        update(properties, hallucinations, new Random(72), 1, 420);

        float strength = hallucinations.strength(Effect.PULSES);
        assertTrue(strength > 1F);
        float[] pulse = hallucinations.pulseColor();
        float[] contrast = hallucinations.contrastColor(null);
        for (int channel = 0; channel < 3; channel++) {
            assertEquals(contrast[channel], pulse[channel], 0F);
        }
        assertEquals(1F, pulse[3], 0F);
    }

    @Test
    public void randomPaletteUsesTheDonorWeightsWhenMixedWithDye() {
        DrugProperties properties = new DrugProperties();
        properties.addToDrug(DrugType.BROWN_SHROOMS, 1);
        properties.addToDrug(DrugType.RED_SHROOMS, 1);
        properties.addToHarmonium(0.9, 0x224488);
        HallucinationManager hallucinations = new HallucinationManager();
        update(properties, hallucinations, new Random(72), 1, 200);

        HarmoniumDrug harmonium = properties.getHarmonium();
        float dyeStrength = (float) harmonium.getActiveValue();
        float[] dye = harmonium.color();
        float[] randomBloom = hallucinations.bloomColor(null);
        float[] mixedBloom = hallucinations.bloomColor(harmonium);
        assertTrue(randomBloom[3] > 0F);
        assertEquals(dyeStrength * 3F + randomBloom[3], mixedBloom[3], 0.00001F);
        assertEquals((dye[0] * dyeStrength * 3F + randomBloom[0] * randomBloom[3]) / mixedBloom[3],
                mixedBloom[0], 0.00001F);

        float[] randomContrast = hallucinations.contrastColor(null);
        float[] mixedContrast = hallucinations.contrastColor(harmonium);
        float combined = randomContrast[3] + dyeStrength;
        assertTrue(combined > 1F);
        assertEquals(1F, mixedContrast[3], 0F);
        assertEquals((dye[2] * dyeStrength + randomContrast[2] * randomContrast[3]) / combined,
                mixedContrast[2], 0.00001F);
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

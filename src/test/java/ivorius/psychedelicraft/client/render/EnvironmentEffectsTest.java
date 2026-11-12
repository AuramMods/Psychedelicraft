package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.config.PSConfig;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnvironmentEffectsTest {
    private final boolean heat = PSConfig.visual.doHeatDistortion;
    private final boolean water = PSConfig.visual.doWaterDistortion;
    private final boolean wet = PSConfig.visual.waterOverlayEnabled;
    private final boolean hurt = PSConfig.visual.hurtOverlayEnabled;

    @After
    public void restoreSettings() {
        PSConfig.visual.doHeatDistortion = heat;
        PSConfig.visual.doWaterDistortion = water;
        PSConfig.visual.waterOverlayEnabled = wet;
        PSConfig.visual.hurtOverlayEnabled = hurt;
    }

    @Test
    public void heatAndWaterUseDifferentConditionsAndPeyoteHasItsOwnStrength() {
        PSConfig.visual.doHeatDistortion = true;
        PSConfig.visual.doWaterDistortion = true;
        EnvironmentEffects effects = new EnvironmentEffects();
        for (int tick = 0; tick < 200; tick++) effects.update(20F, 2F, false, false);
        assertEquals(0.0015F, effects.heatDistortion(), 0.00001F);
        assertEquals(0.01073F, effects.waterDistortion(1F), 0F);

        effects.update(20F, 2F, true, false);
        assertEquals(0F, effects.heatDistortion(), 0F);
        assertEquals(0.025F, effects.waterDistortion(1F), 0F);
        PSConfig.visual.doWaterDistortion = false;
        assertEquals(0F, effects.waterDistortion(0F), 0F);
        assertEquals(0.01073F, effects.waterDistortion(1F), 0F);
    }

    @Test
    public void wetnessBuildsInRainAndWaterAndEvaporatesOnDryLand() {
        PSConfig.visual.waterOverlayEnabled = true;
        EnvironmentEffects effects = new EnvironmentEffects();
        for (int tick = 0; tick < 20; tick++) effects.update(20F, 0F, false, true);
        assertEquals(0.75F, effects.wetOverlay(), 0F);
        for (int tick = 0; tick < 6; tick++) effects.update(20F, 0F, true, false);
        assertEquals(0F, effects.wetOverlay(), 0F);
        for (int tick = 0; tick < 21; tick++) effects.update(20F, 0F, false, false);
        assertEquals(79F / 80F, effects.wetOverlay(), 0F);
        for (int tick = 0; tick < 79; tick++) effects.update(20F, 0F, false, false);
        assertEquals(0F, effects.wetOverlay(), 0F);
    }

    @Test
    public void injuryTakesTheStrongerFlashAndLowHealthLingers() {
        PSConfig.visual.hurtOverlayEnabled = true;
        EnvironmentEffects effects = new EnvironmentEffects();
        assertEquals(0.6F, effects.hurtOverlay(6, 10, 0.2F), 0F);
        assertEquals(0.8F, effects.hurtOverlay(6, 10, 0.8F), 0F);
        for (int tick = 0; tick < 200; tick++) effects.update(2F, 0F, false, false);
        assertEquals(0.5F, effects.hurtOverlay(0, 10, 0F), 0.00001F);
        effects.update(20F, 0F, false, false);
        assertTrue(effects.hurtOverlay(0, 10, 0F) < 0.5F);
        PSConfig.visual.hurtOverlayEnabled = false;
        assertEquals(0F, effects.hurtOverlay(6, 10, 0.8F), 0F);
    }
}

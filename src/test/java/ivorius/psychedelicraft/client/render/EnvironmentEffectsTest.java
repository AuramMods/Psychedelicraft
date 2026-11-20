package ivorius.psychedelicraft.client.render;

import com.electronwill.nightconfig.core.CommentedConfig;
import ivorius.psychedelicraft.config.PSClientConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnvironmentEffectsTest {
    @BeforeClass public static void config(){PSClientConfig.SPEC.setConfig(CommentedConfig.inMemory());}

    @Test public void injuryTakesTheStrongerHeartbeatOrDamageAndLowHealthLingers() {
        PSClientConfig.HURT_OVERLAY.set(true);
        EnvironmentEffects effects=new EnvironmentEffects();
        assertEquals(.6F,effects.hurtOverlay(6,10,.2F),0);assertEquals(.8F,effects.hurtOverlay(6,10,.8F),0);
        for(int tick=0;tick<200;tick++)effects.update(2,0,false,false);
        assertEquals(.5F,effects.hurtOverlay(0,10,0),.000001F);
        effects.update(20,0,false,false);assertTrue(effects.hurtOverlay(0,10,0)<.5F);
        PSClientConfig.HURT_OVERLAY.set(false);assertEquals(0,effects.hurtOverlay(6,10,.8F),0);PSClientConfig.HURT_OVERLAY.set(true);
    }

    @Test public void peyoteAndUnderwaterDistortionCanBeToggledIndependently() {
        boolean previousWater = PSClientConfig.WATER_DISTORTION.get();
        boolean previousPeyote = PSClientConfig.PEYOTE_DISTORTION.get();
        EnvironmentEffects effects = new EnvironmentEffects();
        try {
            PSClientConfig.WATER_DISTORTION.set(false);
            PSClientConfig.PEYOTE_DISTORTION.set(true);
            assertEquals(.01073F, effects.waterDistortion(1), 0);

            effects.update(0, true, false);
            assertEquals(.01073F, effects.waterDistortion(1), 0);

            PSClientConfig.PEYOTE_DISTORTION.set(false);
            assertEquals(0, effects.waterDistortion(1), 0);
            PSClientConfig.WATER_DISTORTION.set(true);
            assertEquals(.025F, effects.waterDistortion(1), 0);
        } finally {
            PSClientConfig.WATER_DISTORTION.set(previousWater);
            PSClientConfig.PEYOTE_DISTORTION.set(previousPeyote);
        }
    }
}

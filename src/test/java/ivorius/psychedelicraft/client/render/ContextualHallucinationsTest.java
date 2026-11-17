package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContextualHallucinationsTest {
    private static RandomSource rolls(float... values){return new LegacyRandomSource(0){private int index;@Override public float nextFloat(){return values[index++];}};}
    @Test public void acceptedSelectionKeepsCannabisThresholdHeadExclusionAndRegistryOrder(){
        assertEquals(HallucinationType.RASTA_HEAD,ContextualHallucinations.pickNatural(rolls(.1F),.41F,false));
        assertEquals(HallucinationType.MULTIPLE_ENTITY,ContextualHallucinations.pickNatural(rolls(.05F,.5F),.41F,true));
        assertEquals(HallucinationType.FRIENDLY_ZOMBIES,ContextualHallucinations.pickNatural(rolls(.05F,.7F),.4F,false));
        assertEquals(HallucinationType.FRIENDLY_ZOMBIES,ContextualHallucinations.pickNatural(rolls(.6F,.7F),0,false));
    }
    @Test public void forcedEnvelopeFadesInPeaksAndFlickersBeforeItsHardExpiry(){
        assertEquals(0,HallucinationTiming.forcedAlpha(400,400,0,0),.000001);
        assertEquals(1,HallucinationTiming.forcedAlpha(200,200,0,0),.000001);
        assertEquals(0,HallucinationTiming.forcedAlpha(4,4,0,0),0);
        assertEquals((Math.sin(.05*Math.PI)*2+.5)/2,HallucinationTiming.forcedAlpha(20,20,0,0),.000001);
        assertEquals(.5,HallucinationTiming.strength(0,1),0);assertEquals(1,HallucinationTiming.strength(1,0),0);
        for(int age=0;age<=400;age++)assertTrue(HallucinationTiming.forcedAlpha(400-age,400-age,.5F,age)>=0);
    }
    @Test public void apparitionLifetimeStartsAndEndsInvisible(){
        assertEquals(0,HallucinationTiming.lifeAlpha(0,60),0);assertEquals(1,HallucinationTiming.lifeAlpha(30,60),0);assertEquals(0,HallucinationTiming.lifeAlpha(60,60),.000001);
    }
}

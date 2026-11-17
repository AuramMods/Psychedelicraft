package ivorius.psychedelicraft.entity.drug;
import org.junit.Test;
import static org.junit.Assert.*;
public class BreathingTest {
    @Test public void alternatingBreathsUseTheAcceptedIntervalAndPitch(){
        var breathing=new Breathing();assertTrue(breathing.advance(.5F,.5F));assertEquals(1.125F,breathing.pitch(.5F),.000001);
        for(int tick=1;tick<20;tick++)assertFalse(breathing.advance(.5F,.5F));
        assertTrue(breathing.advance(.5F,.5F));assertEquals(.975F,breathing.pitch(.5F),.000001);
        for(int tick=1;tick<20;tick++)assertFalse(breathing.advance(.5F,.5F));
        assertTrue(breathing.advance(.5F,.5F));assertEquals(1.125F,breathing.pitch(.5F),.000001);
    }
    @Test public void ClearedVolumeLetsTheOldIntervalFinishWithoutEmitting(){
        var breathing=new Breathing();assertTrue(breathing.advance(.5F,0));
        for(int tick=1;tick<=30;tick++)assertFalse(breathing.advance(0,0));
        assertTrue(breathing.advance(.5F,0));assertEquals(1.1F,breathing.pitch(0),.000001);
    }
}

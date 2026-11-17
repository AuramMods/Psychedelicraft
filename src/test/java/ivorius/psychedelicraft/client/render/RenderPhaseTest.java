package ivorius.psychedelicraft.client.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class RenderPhaseTest {
    @Test public void nestedPostAndSkyScopesRestoreWorldAndThenNativeGui(){
        RenderPhase.reset();try(var world=RenderPhase.WORLD.enter()){assertTrue(RenderPhase.current().deforms());try(var post=RenderPhase.POST.enter()){assertFalse(RenderPhase.current().active());}assertEquals(RenderPhase.WORLD,RenderPhase.current());try(var sky=RenderPhase.SKY.enter()){assertTrue(RenderPhase.current().active());assertFalse(RenderPhase.current().deforms());}}assertEquals(RenderPhase.NORMAL,RenderPhase.current());
    }
    @Test public void exceptionalDrawAndRepeatedCloseCannotLeakEffectScope(){
        RenderPhase.reset();var scope=RenderPhase.HAND.enter();try(scope){throw new IllegalStateException("cancelled native draw");}catch(IllegalStateException expected){}assertEquals(RenderPhase.NORMAL,RenderPhase.current());scope.close();assertEquals(RenderPhase.NORMAL,RenderPhase.current());
    }
}

package ivorius.psychedelicraft.client.render.shader;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MotionTimelineTest {
    @Test public void capturesAtHalfTickCadenceRatherThanEveryRenderedFrame(){
        var history=new MotionTimeline();var captured=new ArrayList<Integer>();history.update(2.25F,captured::add);history.update(2.49F,captured::add);history.update(2.75F,captured::add);assertEquals(List.of(0,1),captured);assertEquals(1,history.index(0));assertFalse(history.visible(2,2.75F));assertEquals(.06F,history.opacity(29,1),.00001F);
    }
    @Test public void longFrameIntervalsPopulateOnlyTheCurrentHistoryWindow(){
        var history=new MotionTimeline();var captured=new ArrayList<Integer>();history.update(0,captured::add);captured.clear();history.update(100,captured::add);assertEquals(30,captured.size());assertEquals(30,new HashSet<>(captured).size());for(int i=0;i<30;i++)assertTrue(history.visible(i,100));assertFalse(history.visible(history.index(1),100.5F));
    }
    @Test public void clockRewindCannotReplayTexturesFromThePreviousTimeline(){
        var history=new MotionTimeline();history.update(20,i->{});history.update(22,i->{});history.update(1,i->{});assertTrue(history.visible(0,1));for(int i=1;i<30;i++)assertFalse(history.visible(i,1));assertFalse(history.visible(0,.5F));
    }
    @Test public void exactWindowBoundaryAndExplicitResetDoNotLeaveGhostFrames(){
        var history=new MotionTimeline();history.update(5,i->{});assertTrue(history.visible(0,19.999F));assertFalse(history.visible(0,20));history.reset();assertTrue(history.empty());assertFalse(history.visible(0,5));history.update(5,i->assertEquals(0,i));assertFalse(history.empty());
    }
}

package ivorius.psychedelicraft.client.render;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class OverlayMathTest {
    @Test public void heartbeatSoundAndInterpolatedFlashShareTheAcceptedClock() {
        Heartbeat heart=new Heartbeat();
        assertTrue(heart.tick(1,1));assertEquals(0,heart.pulse(1),0);
        assertFalse(heart.tick(1,1));assertEquals(.2F,heart.pulse(1),.000001F);assertEquals(.1F,heart.pulse(.5F),.000001F);
        for(int tick=2;tick<35;tick++)assertFalse(heart.tick(1,1));
        assertTrue(heart.tick(1,1));
        for(int tick=0;tick<80;tick++)assertFalse(heart.tick(0,1));
        assertEquals(0,heart.pulse(.5F),0);
    }

    @Test public void strongerHeartbeatShortensTheIntervalWithoutCreatingZeroVolumeFlashes() {
        Heartbeat fast=new Heartbeat(),silent=new Heartbeat();
        assertTrue(fast.tick(1,4));
        for(int tick=1;tick<8;tick++)assertFalse(fast.tick(1,4));
        assertTrue(fast.tick(1,4));
        for(int tick=0;tick<100;tick++){assertFalse(silent.tick(0,4));assertEquals(0,silent.pulse(1),0);}
    }

    @Test public void eyelidsInterpolateClampThrottleAndEventuallyClearTheirExistingBlink() {
        Eyelids eyes=new Eyelids();var random=RandomSource.create(7);
        assertTrue(eyes.tick(1,0,random).warning());assertEquals(0,eyes.opacity(1),0);
        float max=0;int lastBeat=-100;
        for(int age=1;age<700;age++){
            var events=eyes.tick(1,age,random);float alpha=eyes.opacity(.5F);assertTrue(alpha>=0&&alpha<=1);max=Math.max(max,alpha);
            if(events.heartbeat()){assertTrue(age-lastBeat>=20);lastBeat=age;}
        }
        assertTrue("blink reaches heartbeat threshold: "+max,max>.6F);assertTrue(lastBeat>0);
        for(int age=700;age<1700;age++)eyes.tick(0,age,random);
        assertEquals(0,eyes.opacity(.5F),0);
    }

    @Test public void sunDirectionRespectsFrontViewAndThePlayerLookingUpAtNoon() {
        Vec3 first=SunFlare.direction(0,0,-90,false),front=SunFlare.direction(0,0,-90,true);
        assertEquals(1,first.z,.000001);assertEquals(-1,front.z,.000001);
        assertEquals(1,SunFlare.visibility(first,640,360,0,true),.000001);
        assertEquals(0,SunFlare.visibility(front,640,360,0,true),0);
    }

    @Test public void sunVisibilityRejectsRoofFluidsRainAndTheScreenEdge() {
        Vec3 sun=new Vec3(.1,.1,1);
        float clear=SunFlare.visibility(sun,640,360,0,true);assertTrue(clear>0&&clear<1);
        assertEquals(clear*.5F,SunFlare.visibility(sun,640,360,.5F,true),.000001F);
        assertEquals(0,SunFlare.visibility(sun,640,360,0,false),0);
        assertEquals(0,SunFlare.visibility(sun,640,360,1,true),0);
        assertEquals(0,SunFlare.visibility(new Vec3(1,0,1),640,360,0,true),0);
        SunFlare smooth=new SunFlare();smooth.tick(1);assertEquals(.11F,smooth.alpha(),.000001F);
        for(int tick=0;tick<100;tick++)smooth.tick(0);assertEquals(0,smooth.alpha(),0);
    }

    @Test public void alcoholOpacityUsesTheDonorWaveAndNeverExceedsPointTwo() {
        assertEquals(0,OverlayMesh.alcoholAlpha(17,0),0);
        assertEquals(.125F,OverlayMesh.alcoholAlpha(0,.5F),0);
        for(int tick=0;tick<1000;tick++){float alpha=OverlayMesh.alcoholAlpha(tick,1);assertTrue(alpha>=0&&alpha<=.2F);}
    }

    @Test public void warmthHasThreeContinuousStripsWithBoundedOpacityAtScaledSizes() {
        for(int width:new int[]{320,640,854}){
            var quads=OverlayMesh.warmth(width,360,17,.5F);assertEquals(87,quads.size());
            for(int strip=0;strip<3;strip++)for(int row=0;row<29;row++){
                var q=quads.get(strip*29+row);assertTrue(q.bottomLeft().color().a()>=0&&q.bottomLeft().color().a()<=.5F);
                assertTrue(q.bottomLeft().y()>q.topLeft().y());
                if(row>0){var previous=quads.get(strip*29+row-1);assertEquals(previous.bottomLeft().x(),q.topLeft().x(),0);assertEquals(previous.bottomLeft().y(),q.topLeft().y(),0);assertEquals(previous.bottomLeft().v(),q.topLeft().v(),0);}
            }
        }
        assertTrue(OverlayMesh.warmth(640,360,0,0).isEmpty());
    }

    @Test public void powerParticlesAreReproducibleAndLightningIsBoundedAndFadesWithinItsTwoTicks() {
        var first=OverlayMesh.power(640,360,0,0,1);assertEquals(146,first.particles().size());assertEquals(first,OverlayMesh.power(640,360,0,0,1));
        assertTrue(OverlayMesh.power(640,360,0,0,0).particles().isEmpty());
        boolean sawLightning=false;
        for(int age=0;age<2000;age++){
            var a=OverlayMesh.power(640,360,age,.1F,1);assertTrue(a.lightning().size()<=3);
            if(!a.lightning().isEmpty()){
                var b=OverlayMesh.power(640,360,age,.9F,1);assertEquals(a.lightning().size(),b.lightning().size());
                var flash=a.lightning().get(0);assertTrue(flash.texture()>=0&&flash.texture()<4);assertTrue(flash.quad().bottomLeft().color().a()>b.lightning().get(0).quad().bottomLeft().color().a());sawLightning=true;
            }
        }
        assertTrue(sawLightning);
    }
}

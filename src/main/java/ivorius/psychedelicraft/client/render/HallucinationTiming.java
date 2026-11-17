/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.render;

/** Accepted onset, fade and final flicker envelopes, independent of the renderer. */
public final class HallucinationTiming {
    public static final int FORCED_TICKS=400;
    private HallucinationTiming(){}
    public static float lifeAlpha(int age,int maxAge){return Math.max(0,Math.min(1,(float)Math.sin((double)Math.min(age,maxAge-2)/(maxAge-2)*Math.PI)*18));}
    public static float forcedAlpha(int previous,int current,float partial,int playerAge){
        float remaining=(previous+(current-previous)*partial)/FORCED_TICKS;
        if(remaining<=.01F)return 0;
        float value=Math.abs(Math.min((float)Math.sin(remaining*Math.PI)*2,1));
        if(remaining<.1F)value=(value+.5F+.5F*(float)Math.sin((playerAge+partial)/3F))/2;
        return value;
    }
    public static float strength(float natural,float forced){float value=Math.max(0,Math.min(1,natural*15));return forced>0?(value+forced)/2:value;}
}

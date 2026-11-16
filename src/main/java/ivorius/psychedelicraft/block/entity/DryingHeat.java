package ivorius.psychedelicraft.block.entity;

/** The accepted light/biome drying curve, independent of platform inventory APIs. */
public final class DryingHeat {
    private DryingHeat(){}
    public static float at(int light,float biomeTemperature){
        float brightness=light/15F;
        float warmth=brightness*brightness*(biomeTemperature*0.75F+0.25F);
        return Math.max(0,Math.min(1,warmth*warmth));
    }
}

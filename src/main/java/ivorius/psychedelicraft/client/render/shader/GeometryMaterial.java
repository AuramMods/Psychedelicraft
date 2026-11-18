package ivorius.psychedelicraft.client.render.shader;

import ivorius.psychedelicraft.client.render.RenderPhase;

/** Native shader aliases for the accepted world/model/uniform/spherical material rules. */
public final class GeometryMaterial {
    private GeometryMaterial(){}
    public static boolean surface(RenderPhase phase,String shader){
        return phase.active()&&phase!=RenderPhase.STARS&&phase!=RenderPhase.BORDER
                &&!shader.contains("glint")&&!shader.contains("end_portal")&&!shader.contains("end_gateway")&&!shader.contains("outline")
                &&(phase!=RenderPhase.SKY||shader.equals("position"));
    }
    public static boolean pulses(String shader){return !shader.contains("outline");}
    public static float pulseShape(RenderPhase phase,String shader){
        if(phase==RenderPhase.SCREEN||phase==RenderPhase.END_SKY||phase==RenderPhase.BORDER
                ||phase==RenderPhase.SKY&&!shader.equals("position_color")||shader.contains("crumbling")
                ||shader.contains("end_portal")||shader.contains("end_gateway")||shader.contains("beacon_beam")
                ||shader.contains("see_through")||shader.contains("translucent_moving_block"))return 0;
        if(shader.contains("zero_matter"))return 1;
        if(phase==RenderPhase.STARS||phase==RenderPhase.SKY||phase==RenderPhase.HAND||phase==RenderPhase.MODEL)return 2;
        if(shader.contains("entity")||shader.contains("glint")||shader.contains("armor")||shader.contains("energy_swirl")||shader.contains("eyes")
                ||shader.contains("lightning")||shader.contains("leash")||shader.contains("text")||shader.contains("water_mask"))return 2;
        return 3;
    }
    public static float pulseShape(RenderPhase phase,String shader,boolean sphericalFog){
        float shape=pulseShape(phase,shader);
        return sphericalFog&&shape>1?1:shape;
    }
}

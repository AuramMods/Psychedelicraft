/* Copyright (c) 2014, Lukas Tenbrink. Native fluid-face geometry by Sollace. */
package ivorius.psychedelicraft.client.render;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.Direction;

public final class FluidBoxRenderer {
    private final PoseStack.Pose pose;private final VertexConsumer buffer;private final TankFluidAppearance appearance;private final int light,overlay;
    public FluidBoxRenderer(PoseStack pose,VertexConsumer buffer,TankFluidAppearance appearance,int light,int overlay){this.pose=pose.last();this.buffer=buffer;this.appearance=appearance;this.light=light;this.overlay=overlay;}
    private void vertex(float x,float y,float z,float u,float v,Direction side){
        int color=appearance.color();buffer.vertex(pose.pose(),x,y,z).color(color>>16&255,color>>8&255,color&255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(pose.normal(),side.getStepX(),side.getStepY(),side.getStepZ()).endVertex();
    }
    public void draw(float x,float y,float z,float width,float height,float length,Direction... sides){
        var sprite=appearance.sprite();float u0=sprite.getU0(),u1=sprite.getU1(),v0=sprite.getV0(),v1=sprite.getV1();
        for(Direction side:sides)switch(side){
            case DOWN->{vertex(x,y,z,u0,v0,side);vertex(x+width,y,z,u1,v0,side);vertex(x+width,y,z+length,u1,v1,side);vertex(x,y,z+length,u0,v1,side);}
            case UP->{vertex(x,y+height,z,u0,v0,side);vertex(x,y+height,z+length,u0,v1,side);vertex(x+width,y+height,z+length,u1,v1,side);vertex(x+width,y+height,z,u1,v0,side);}
            case EAST->{vertex(x+width,y,z,u0,v0,side);vertex(x+width,y+height,z,u1,v0,side);vertex(x+width,y+height,z+length,u1,v1,side);vertex(x+width,y,z+length,u0,v1,side);}
            case WEST->{vertex(x,y,z,u0,v0,side);vertex(x,y,z+length,u1,v0,side);vertex(x,y+height,z+length,u1,v1,side);vertex(x,y+height,z,u0,v1,side);}
            case NORTH->{vertex(x,y,z,u0,v0,side);vertex(x,y+height,z,u0,v1,side);vertex(x+width,y+height,z,u1,v1,side);vertex(x+width,y,z,u1,v0,side);}
            case SOUTH->{vertex(x,y,z+length,u0,v0,side);vertex(x+width,y,z+length,u1,v0,side);vertex(x+width,y+height,z+length,u1,v1,side);vertex(x,y+height,z+length,u0,v1,side);}
        }
    }
}

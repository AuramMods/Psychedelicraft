/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.render;

import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Accepted screen geometry, kept independent of native draw state. */
final class OverlayMesh {
    record Color(float r, float g, float b, float a) {}
    record Vertex(float x, float y, float u, float v, Color color) {}
    record Quad(Vertex bottomLeft, Vertex bottomRight, Vertex topRight, Vertex topLeft) {}
    record Lightning(int texture, Quad quad) {}
    record Power(List<Quad> particles, List<Lightning> lightning) {}

    static Quad rectangle(float left, float top, float right, float bottom,
                          float u0, float v0, float u1, float v1, Color color) {
        return new Quad(new Vertex(left,bottom,u0,v1,color),new Vertex(right,bottom,u1,v1,color),
                new Vertex(right,top,u1,v0,color),new Vertex(left,top,u0,v0,color));
    }

    static float alcoholAlpha(float ticks, float alcohol) {
        return Math.min(.8F, alcohol + Mth.sin(ticks / 80) * alcohol * .5F) * .25F;
    }

    static List<Quad> warmth(int width, int height, int age, float alpha) {
        List<Quad> quads = new ArrayList<>();
        if (alpha <= 0) return quads;
        int segment = width / 9;
        for (int strip = 0; strip < 3; strip++) {
            int center = (int)(segment * (strip * 3 + 1.5F));
            for (int row = 1; row < 30; row++) {
                float v0=(row-1)/30F,v1=row/30F,fade=1-v1;
                Color color=new Color(1,.5F+fade*.3F,.35F+fade*.1F,Math.max(0,alpha-fade*.4F));
                float x0=center+Mth.sin(v0*5+age/10F)*segment*.25F;
                float x1=center+Mth.sin(v1*5+age/10F)*segment*.25F;
                float y0=height/7+v0*height/7*5,y1=height/7+v1*height/7*5;
                quads.add(new Quad(new Vertex(x1-segment,y1,0,v1,color),new Vertex(x1+segment,y1,1,v1,color),
                        new Vertex(x0+segment,y0,1,v0,color),new Vertex(x0-segment,y0,0,v0,color)));
            }
        }
        return quads;
    }

    static Power power(int width, int height, int age, float partial, float strength) {
        List<Quad> particles=new ArrayList<>();List<Lightning> bolts=new ArrayList<>();
        if(strength<=0)return new Power(particles,bolts);
        Random random=new Random(age);
        int count=Mth.floor(random.nextFloat()*200*strength),w=Math.max(1,height/10),h=Mth.ceil(w*strength);
        for(int i=0;i<count;i++){
            int x=random.nextInt(width+w)-w,y=random.nextInt(height+h)-h;
            particles.add(rectangle(x,y,x+w,y+h,0,0,1,1,new Color(1,1,1,1)));
        }
        Random lightning=new Random(age/2*21124871824L);int flashes=0;
        while(lightning.nextFloat()<(strength-.5F)*.1F&&flashes<3)flashes++;
        float alpha=(.05F+strength*.1F)*(1-((age%2)+partial)*.5F);
        for(int i=0;i<flashes;i++){
            float x=lightning.nextInt(width+height)-height+(lightning.nextFloat()-.5F)*height*partial*2;
            int texture=lightning.nextInt(4);boolean inverted=lightning.nextBoolean();
            bolts.add(new Lightning(texture,rectangle(x,0,x+height,height,0,inverted?1:0,1,inverted?0:1,new Color(1,1,1,alpha))));
        }
        return new Power(particles,bolts);
    }
}

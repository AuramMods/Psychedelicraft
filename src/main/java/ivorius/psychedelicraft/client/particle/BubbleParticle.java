/* Copyright (c) 2014, Lukas Tenbrink. Native particle by Sollace. */
package ivorius.psychedelicraft.client.particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.DustParticleOptions;
public final class BubbleParticle extends TextureSheetParticle {
    public BubbleParticle(DustParticleOptions effect,SpriteSet sprites,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){
        super(level,x,y,z);pickSprite(sprites);setSize(.02F,.02F);quadSize*=random.nextFloat()*.6F+.2F;
        xd=vx*.2+(random.nextDouble()-.5)*.04;yd=vy*.2+(random.nextDouble()-.5)*.04;zd=vz*.2+(random.nextDouble()-.5)*.04;
        lifetime=(int)(8/(random.nextDouble()*.8+.2));var color=effect.getColor();setColor(color.x,color.y,color.z);
    }
    @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}yd+=.002;move(xd,yd,zd);xd*=.85;yd*=.85;zd*=.85;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
}

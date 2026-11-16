/* Target-version smoke renderer adapted from Sollace. */
package ivorius.psychedelicraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.DustParticleOptions;

public final class ExhaledSmokeParticle extends SmokeParticle {
    public ExhaledSmokeParticle(DustParticleOptions effect,SpriteSet sprites,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){
        super(level,x,y,z,vx,vy,vz,effect.getScale(),sprites);
        var color=effect.getColor();setColor(color.x,color.y,color.z);
    }
}

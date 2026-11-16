package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.particle.ExhaledSmokeParticle;
import ivorius.psychedelicraft.particle.PSParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientParticles {
    private ClientParticles(){}
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(PSParticles.EXHALED_SMOKE.get(),sprites->(effect,level,x,y,z,vx,vy,vz)->new ExhaledSmokeParticle(effect,sprites,level,x,y,z,vx,vy,vz));
    }
}

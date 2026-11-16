package ivorius.psychedelicraft.particle;

import com.mojang.serialization.Codec;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,Psychedelicraft.MOD_ID);
    public static final RegistryObject<ParticleType<DustParticleOptions>> EXHALED_SMOKE=TYPES.register("exhaled_smoke",()->new ParticleType<>(false,SmokeParticleOptions.DESERIALIZER){
        @Override public Codec<DustParticleOptions> codec(){return SmokeParticleOptions.CODEC;}
    });
    public static final RegistryObject<ParticleType<DustParticleOptions>> BUBBLE=TYPES.register("bubble",()->new ParticleType<>(false,BubbleParticleOptions.DESERIALIZER){
        @Override public Codec<DustParticleOptions> codec(){return BubbleParticleOptions.CODEC;}
    });
    private PSParticles(){}
}

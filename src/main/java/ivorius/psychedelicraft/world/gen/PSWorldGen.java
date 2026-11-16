package ivorius.psychedelicraft.world.gen;

import com.mojang.serialization.Codec;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public final class PSWorldGen {
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,Psychedelicraft.MOD_ID);
    private static final DeferredRegister<Codec<? extends BiomeModifier>> MODIFIERS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,Psychedelicraft.MOD_ID);
    public static final RegistryObject<TilledPatchFeature> TILLED_PATCH=FEATURES.register("tilled_patch",TilledPatchFeature::new);
    private static final RegistryObject<Codec<PlantBiomeModifier>> PLANTS=MODIFIERS.register("plants",()->PlantBiomeModifier.CODEC);
    private PSWorldGen(){}
    public static void register(IEventBus bus){FEATURES.register(bus);MODIFIERS.register(bus);}
}

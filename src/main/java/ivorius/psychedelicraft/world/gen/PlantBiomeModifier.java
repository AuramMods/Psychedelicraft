package ivorius.psychedelicraft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

public record PlantBiomeModifier(String plant, HolderSet<PlacedFeature> features) implements BiomeModifier {
    public static final Codec<PlantBiomeModifier> CODEC=RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("plant").forGetter(PlantBiomeModifier::plant),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(PlantBiomeModifier::features)).apply(instance,PlantBiomeModifier::new));

    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase==Phase.ADD && allows(biome)) features.forEach(feature ->
                builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,feature));
    }
    public boolean allows(Holder<Biome> biome) {
        String name=plant.equals("coca_jungle")?"coca":plant.equals("agave")?"peyote":plant;
        PSConfig.GenerationFeature options=PSConfig.GENERATION.get(name);
        if (options==null) throw new IllegalArgumentException("Unknown plant generation profile "+plant);
        if (!options.enabled().get() || options.excluded().get().stream().anyMatch(selector -> matches(biome,selector))) return false;
        if (plant.equals("coca_jungle")) return defaults(biome,plant);
        return defaults(biome,plant) || !options.included().get().isEmpty()
                && options.included().get().stream().allMatch(selector -> matches(biome,selector));
    }
    static boolean matches(Holder<Biome> biome,String selector) {
        String text=selector.trim();
        ResourceLocation id=ResourceLocation.tryParse(text.startsWith("#")?text.substring(1):text);
        if(id==null)return false;
        return text.startsWith("#")?biome.is(TagKey.create(Registries.BIOME,id)):biome.unwrapKey().map(key -> key.location().equals(id)).orElse(false);
    }
    static boolean defaults(Holder<Biome> biome,String plant) {
        return switch(plant) {
            case "coca_jungle" -> biome.is(BiomeTags.IS_JUNGLE) || matches(biome,"#forge:is_jungle");
            case "cannabis","hop","tobacco","coffea","coca" -> biome.is(BiomeTags.IS_OVERWORLD)
                    && (biome.value().getBaseTemperature()<0.15F || biome.is(BiomeTags.IS_HILL)
                    || biome.is(BiomeTags.IS_FOREST) || biome.is(Biomes.PLAINS));
            case "belladonna" -> biome.is(Biomes.DARK_FOREST);
            case "jimsonweed" -> biome.is(Biomes.JUNGLE)||biome.is(Biomes.SPARSE_JUNGLE);
            case "tomato" -> biome.is(Biomes.FOREST);
            case "peyote" -> biome.is(BiomeTags.IS_OVERWORLD) && (biome.is(BiomeTags.IS_SAVANNA)||dry(biome));
            case "agave" -> biome.is(BiomeTags.IS_OVERWORLD) && dry(biome);
            default -> throw new IllegalArgumentException("Unknown plant generation profile "+plant);
        };
    }
    private static boolean dry(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_BADLANDS)||biome.is(BiomeTags.HAS_DESERT_PYRAMID)||!biome.value().hasPrecipitation();
    }
    @Override public Codec<? extends BiomeModifier> codec(){return CODEC;}
}

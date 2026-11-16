package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,Psychedelicraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,Psychedelicraft.MOD_ID);
    public static final RegistryObject<RecipeType<DryingRecipe>> DRYING_TYPE=TYPES.register("drying",()->new RecipeType<>() {
        @Override public String toString(){return "psychedelicraft:drying";}
    });
    public static final RegistryObject<RecipeSerializer<DryingRecipe>> DRYING=SERIALIZERS.register("drying",DryingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<HarmoniumDyeRecipe>> HARMONIUM_DYE=SERIALIZERS.register("dye_harmonium",()->new SimpleCraftingRecipeSerializer<>(HarmoniumDyeRecipe::new));
    private PSRecipes(){}
    public static void register(IEventBus bus){TYPES.register(bus);SERIALIZERS.register(bus);}
}

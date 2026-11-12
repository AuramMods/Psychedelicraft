package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class JuniperWoodRecipes extends ShapedRecipes {
    private final IRecipe juniperRecipe;

    private JuniperWoodRecipes(ShapedRecipes generic, IRecipe juniperRecipe) {
        super(generic.getGroup(), generic.getWidth(), generic.getHeight(), generic.getIngredients(), generic.getRecipeOutput());
        this.juniperRecipe = juniperRecipe;
        setRegistryName(generic.getRegistryName());
    }

    public static void preferJuniperRecipes() {
        preferJuniperRecipe("trapdoor", "juniper_trapdoor");
        preferJuniperRecipe("wooden_button", "juniper_button");
        preferJuniperRecipe("wooden_pressure_plate", "juniper_pressure_plate");
    }

    private static void preferJuniperRecipe(String genericId, String juniperId) {
        IRecipe generic = CraftingManager.getRecipe(new ResourceLocation("minecraft", genericId));
        IRecipe juniper = CraftingManager.getRecipe(Psychedelicraft.id(juniperId));
        // Forge's generic plankWood ingredients also match the new juniper recipes.
        if (generic != null && generic.getClass() == ShapedRecipes.class && juniper != null) {
            ForgeRegistries.RECIPES.register(new JuniperWoodRecipes((ShapedRecipes) generic, juniper));
        }
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return super.matches(inventory, world) && !juniperRecipe.matches(inventory, world);
    }
}

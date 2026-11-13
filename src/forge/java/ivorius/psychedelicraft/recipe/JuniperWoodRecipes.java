package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

public final class JuniperWoodRecipes extends ShapedRecipes {
    private final IRecipe juniperRecipe;

    private JuniperWoodRecipes(ShapedRecipes generic, IRecipe juniperRecipe) {
        super(generic.getGroup(), generic.getWidth(), generic.getHeight(), generic.getIngredients(), generic.getRecipeOutput());
        this.juniperRecipe = juniperRecipe;
        setRegistryName(generic.getRegistryName());
    }

    public static void register() {
        for (Item wood : new Item[]{PSItems.JUNIPER_LOG, PSItems.JUNIPER_WOOD,
                PSItems.STRIPPED_JUNIPER_LOG, PSItems.STRIPPED_JUNIPER_WOOD}) {
            OreDictionary.registerOre("logWood", wood);
            GameRegistry.addSmelting(wood, new ItemStack(Items.COAL, 1, 1), 0.15F);
        }
        OreDictionary.registerOre("treeSapling", PSItems.JUNIPER_SAPLING);
        OreDictionary.registerOre("treeLeaves", PSItems.JUNIPER_LEAVES);
        OreDictionary.registerOre("plankWood", PSItems.JUNIPER_PLANKS);
        OreDictionary.registerOre("stairWood", PSItems.JUNIPER_STAIRS);
        OreDictionary.registerOre("slabWood", PSItems.JUNIPER_SLAB);
        OreDictionary.registerOre("fenceWood", PSItems.JUNIPER_FENCE);
        OreDictionary.registerOre("fenceGateWood", PSItems.JUNIPER_FENCE_GATE);
        OreDictionary.registerOre("doorWood", PSItems.JUNIPER_DOOR);

        preferJuniperRecipe("trapdoor", "juniper_trapdoor");
        preferJuniperRecipe("wooden_button", "juniper_button");
        preferJuniperRecipe("wooden_pressure_plate", "juniper_pressure_plate");
        preferJuniperRecipe("sign", "juniper_sign");
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

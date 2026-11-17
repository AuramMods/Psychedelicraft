/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.compat.jei;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.screen.DryingTableScreen;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.*;
import ivorius.psychedelicraft.recipe.*;
import ivorius.psychedelicraft.screen.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.*;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Loaded by JEI's client plugin discovery only; common registration never references JEI. */
@JeiPlugin
public final class PsychedelicraftJeiPlugin implements IModPlugin {
    static final List<String> CATEGORIES = List.of("drying", "mashing", "fermenting", "maturing", "distilling",
            "barrel_draw", "flask_draw", "distillery_draw", "harvest");
    private final Map<String, RecipeType<ProcessRecipe>> types = new LinkedHashMap<>();
    private final Map<String, ItemStack> indexed = new LinkedHashMap<>();
    private final List<SmeltingRecipe> hiddenHeating = new ArrayList<>();
    public PsychedelicraftJeiPlugin() { CATEGORIES.forEach(name -> types.put(name, ProcessCategory.type(name))); }
    @Override public ResourceLocation getPluginUid() { return Psychedelicraft.id("jei"); }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (var entry : PSItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof PortableFluidItem || item instanceof FluidMachineItem) {
                registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item,
                        (IIngredientSubtypeInterpreter<ItemStack>) (stack, context) -> subtype(stack));
            }
        }
    }
    @Override public <T> void registerFluidSubtypes(ISubtypeRegistration registration, mezz.jei.api.helpers.IPlatformFluidHelper<T> helper) {
        PSFluids.all().forEach(family -> registration.useNbtForSubtypes(family.get()));
    }
    static String subtype(ItemStack stack) {
        var handler = FluidVesselHandler.forCrafting(stack);
        var fluid = handler == null ? net.minecraftforge.fluids.FluidStack.EMPTY : handler.getFluid();
        // Quantity is displayed by the vessel, as in the accepted JEI integration. Identity,
        // fermentation, distillation, maturation, warmth, custom names and dye remain distinct.
        CompoundTag key = new CompoundTag();
        if (!fluid.isEmpty()) {
            key.putString("fluid", PSFluids.id(fluid.getFluid()));
            if (fluid.hasTag()) key.put("attributes", fluid.getTag().copy());
        }
        if (stack.hasTag() && stack.getTag().contains("display")) key.put("display", stack.getTag().getCompound("display").copy());
        return key.isEmpty() ? "" : key.toString();
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        Item[] icons = {PSItems.DRYING_TABLE.get(), PSItems.MASH_TUB.get(), PSItems.MASH_TUB.get(),
                PSItems.OAK_BARREL.get(), PSItems.DISTILLERY.get(), PSItems.OAK_BARREL.get(),
                PSItems.FLASK.get(), PSItems.DISTILLERY.get(), Items.SHEARS};
        for (int i = 0; i < CATEGORIES.size(); i++) registration.addRecipeCategories(
                new ProcessCategory(registration.getJeiHelpers().getGuiHelper(), types.get(CATEGORIES.get(i)), icons[i]));
    }
    @Override public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        var crafting = registration.getCraftingCategory();
        crafting.addCategoryExtension(FillReceptacleRecipe.class, FluidCraftingExtension::new);
        crafting.addCategoryExtension(FluidAwareShapelessRecipe.class, FluidCraftingExtension::new);
        crafting.addCategoryExtension(BottleRecipe.class, FluidCraftingExtension::new);
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        indexed.clear();
        hiddenHeating.clear();
        var level = Objects.requireNonNull(Minecraft.getInstance().level);
        var manager = level.getRecipeManager();
        ProcessRecipes.create(manager).forEach((name, recipes) -> {
            registration.addRecipes(types.get(name), recipes);
            recipes.forEach(recipe -> {
                recipe.itemsIn().forEach(items -> items.forEach(this::index));
                recipe.itemsOut().forEach(this::index);
            });
        });
        // JEI's vanilla plugin discovers these through our category extensions. Index their
        // variants here without registering the same native recipe a second time.
        for (var recipe : manager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (recipe instanceof FillReceptacleRecipe fill) {
                fill.getDisplayOutputs().forEach(this::index);
            } else if (recipe instanceof FluidAwareShapelessRecipe) {
                recipe.getIngredients().forEach(ingredient -> Arrays.stream(ingredient.getItems()).forEach(this::index));
            } else if (recipe instanceof BottleRecipe bottle) {
                index(bottle.getResultItem(level.registryAccess()));
                for (DyeColor color : DyeColor.values()) {
                    var output = bottle.getResultItem(level.registryAccess()).copy();
                    HarmoniumItem.setColor(output, color.getTextColor());
                    index(output);
                }
            }
        }
        List<SmeltingRecipe> heating = new ArrayList<>();
        for (var recipe : manager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
            if (!(recipe instanceof HeatingRecipe fluid)) continue;
            hiddenHeating.add(fluid);
            int variant = 0;
            for (ItemStack input : fluid.getDisplayInputs()) {
                ItemStack output = fluid.assemble(new SimpleContainer(input), level.registryAccess());
                index(input); index(output);
                heating.add(new SmeltingRecipe(new ResourceLocation(recipe.getId().getNamespace(), recipe.getId().getPath() + "/jei/" + variant++),
                        recipe.getGroup(), recipe.category(), StrictNBTIngredient.of(input), output, recipe.getExperience(), recipe.getCookingTime()));
            }
        }
        registration.addRecipes(RecipeTypes.SMELTING, heating);
    }
    private void index(ItemStack stack) {
        if (!stack.isEmpty() && (stack.getItem() instanceof PortableFluidItem || stack.getItem() instanceof FluidMachineItem)) {
            indexed.putIfAbsent(ForgeRegistries.ITEMS.getKey(stack.getItem()) + "/" + subtype(stack), stack.copy());
        }
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, indexed.values());
        runtime.getRecipeManager().hideRecipes(RecipeTypes.SMELTING, hiddenHeating);
    }
    @Override public void onRuntimeUnavailable() { indexed.clear(); hiddenHeating.clear(); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        catalyst(registration, PSItems.DRYING_TABLE.get(), "drying");
        catalyst(registration, PSItems.IRON_DRYING_TABLE.get(), "drying");
        catalyst(registration, PSItems.MASH_TUB.get(), "mashing", "fermenting");
        catalyst(registration, PSItems.FLASK.get(), "flask_draw");
        catalyst(registration, PSItems.DISTILLERY.get(), "distilling", "distillery_draw");
        catalyst(registration, Items.SHEARS, "harvest");
        PSItems.ITEMS.getEntries().stream().map(entry -> entry.get()).filter(item -> item instanceof FluidMachineItem
                && ForgeRegistries.ITEMS.getKey(item).getPath().endsWith("_barrel"))
                .forEach(item -> catalyst(registration, item, "maturing", "barrel_draw"));
    }
    private void catalyst(IRecipeCatalystRegistration registration, Item item, String... categories) {
        for (String category : categories) registration.addRecipeCatalyst(new ItemStack(item), types.get(category));
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(DryingTableScreen.class, 88, 34, 25, 16, types.get("drying"));
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(DryingTableMenu.class, PSMenus.DRYING_TABLE.get(), types.get("drying"), 1, 9, 10, 36);
    }
}

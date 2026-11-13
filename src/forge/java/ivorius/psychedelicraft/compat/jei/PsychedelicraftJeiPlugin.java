package ivorius.psychedelicraft.compat.jei;

import ivorius.psychedelicraft.client.screen.DryingTableScreen;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.BrewProcess;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.item.FluidMachineItem;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import ivorius.psychedelicraft.recipe.CoffeeHeating;
import ivorius.psychedelicraft.recipe.DryingRecipe;
import ivorius.psychedelicraft.recipe.FillReceptacleRecipe;
import ivorius.psychedelicraft.recipe.FluidVesselRecipes.BottleRecipe;
import ivorius.psychedelicraft.recipe.MashingRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IVanillaRecipeFactory;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;
import mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JEIPlugin
public final class PsychedelicraftJeiPlugin implements IModPlugin {
    private static final String DRYING = "drying";
    private static final String MASHING = "mashing";
    private static final String FERMENTING = "fermenting";
    private static final String MATURING = "maturing";
    private static final String DISTILLING = "distilling";
    private static final String BARREL = "barrel_draw";
    private static final String FLASK = "flask_draw";
    private static final String DISTILLERY = "distillery_draw";
    private static final String HARVEST = "harvest";
    private final Map<String, ItemStack> displayedVessels = new LinkedHashMap<>();
    private IIngredientRegistry ingredients;

    private static Item[] barrels() {
        return new Item[]{PSItems.OAK_BARREL, PSItems.SPRUCE_BARREL, PSItems.BIRCH_BARREL, PSItems.JUNGLE_BARREL,
                PSItems.ACACIA_BARREL, PSItems.DARK_OAK_BARREL};
    }

    private static Item[] vessels() {
        return new Item[]{PSItems.WOODEN_MUG, PSItems.STONE_CUP, PSItems.GLASS_CHALICE, PSItems.SHOT_GLASS,
                PSItems.BOTTLE, PSItems.MOLOTOV_COCKTAIL, PSItems.SYRINGE, Items.BOWL, Items.GLASS_BOTTLE, Items.BUCKET};
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        Item.REGISTRY.forEach(item -> {
            if (item instanceof PortableFluidItem || item instanceof FluidMachineItem) {
                registry.registerSubtypeInterpreter(item, PsychedelicraftJeiPlugin::subtype);
            }
        });
    }

    private static String subtype(ItemStack stack) {
        FluidStack fluid = stack.getItem() instanceof PortableFluidItem
                ? ((PortableFluidItem) stack.getItem()).contents(stack)
                : ((FluidMachineItem) stack.getItem()).contents(stack);
        NBTTagCompound display = stack.getSubCompound("display");
        String dye = display != null && display.hasKey("color", 99) ? "/" + display.getInteger("color") : "";
        if (fluid == null) return dye;
        return PSFluids.id(fluid.getFluid()) + (fluid.tag == null ? "" : "/" + fluid.tag) + dye;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM, displayedVessels.values());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper gui = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(
                new ProcessCategory(gui, DRYING, PSItems.DRYING_TABLE),
                new ProcessCategory(gui, MASHING, PSItems.MASH_TUB),
                new ProcessCategory(gui, FERMENTING, PSItems.MASH_TUB),
                new ProcessCategory(gui, MATURING, PSItems.OAK_BARREL),
                new ProcessCategory(gui, DISTILLING, PSItems.DISTILLERY),
                new ProcessCategory(gui, BARREL, PSItems.OAK_BARREL),
                new ProcessCategory(gui, FLASK, PSItems.FLASK),
                new ProcessCategory(gui, DISTILLERY, PSItems.DISTILLERY),
                new ProcessCategory(gui, HARVEST, Items.SHEARS));
    }

    @Override
    public void register(IModRegistry registry) {
        ingredients = registry.getIngredientRegistry();
        displayedVessels.clear();
        catalyst(registry, PSItems.DRYING_TABLE, DRYING);
        catalyst(registry, PSItems.IRON_DRYING_TABLE, DRYING);
        catalyst(registry, PSItems.MASH_TUB, MASHING, FERMENTING);
        catalyst(registry, PSItems.FLASK, FLASK);
        catalyst(registry, PSItems.DISTILLERY, DISTILLING, DISTILLERY);
        catalyst(registry, Items.SHEARS, HARVEST);
        for (Item barrel : barrels()) catalyst(registry, barrel, MATURING, BARREL);
        registry.addRecipeClickArea(DryingTableScreen.class, 88, 34, 25, 16, ProcessCategory.uid(DRYING));

        registry.addRecipes(DryingRecipe.recipes().stream().map(PsychedelicraftJeiPlugin::drying)
                .collect(Collectors.toList()), ProcessCategory.uid(DRYING));
        List<ProcessRecipe> mashing = new ArrayList<>();
        CraftingManager.REGISTRY.forEach(recipe -> {
            if (recipe instanceof MashingRecipe && recipe.getIngredients().stream()
                    .allMatch(ingredient -> ingredient.getMatchingStacks().length > 0)) {
                mashing.add(mashing((MashingRecipe) recipe));
            }
        });
        registry.addRecipes(mashing, ProcessCategory.uid(MASHING));
        registry.addRecipes(Arrays.asList(
                harvest(PSItems.MORNING_GLORY_LATTICE, PSItems.MORNING_GLORY),
                harvest(PSItems.WINE_GRAPE_LATTICE, PSItems.WINE_GRAPES),
                harvest(PSItems.FRUITING_JUNIPER_LEAVES, PSItems.JUNIPER_BERRIES)), ProcessCategory.uid(HARVEST));
        registerFluidRecipes(registry);
        registerCoffee(registry);
        registry.handleRecipes(FillReceptacleRecipe.class, FilledCrafting::new, VanillaRecipeCategoryUid.CRAFTING);
        registry.handleRecipes(BottleRecipe.class, recipe -> new ColoredBottle(recipe, null), VanillaRecipeCategoryUid.CRAFTING);
        List<ColoredBottle> dyedBottles = new ArrayList<>();
        CraftingManager.REGISTRY.forEach(recipe -> {
            if (recipe instanceof BottleRecipe) {
                for (EnumDyeColor color : EnumDyeColor.values()) {
                    ColoredBottle variant = new ColoredBottle((BottleRecipe) recipe, color);
                    dyedBottles.add(variant);
                    indexVessel(variant.output);
                }
            } else if (recipe instanceof FillReceptacleRecipe) {
                ((FillReceptacleRecipe) recipe).getDisplayOutputs().forEach(this::indexVessel);
            }
        });
        registry.addRecipes(dyedBottles, VanillaRecipeCategoryUid.CRAFTING);
    }

    private static void catalyst(IModRegistry registry, Item item, String... categories) {
        registry.addRecipeCatalyst(new ItemStack(item), Arrays.stream(categories).map(ProcessCategory::uid).toArray(String[]::new));
    }

    private static ProcessRecipe drying(DryingRecipe recipe) {
        return new ProcessRecipe(Collections.nCopies(DryingRecipe.INPUT_COUNT, Arrays.asList(recipe.getInput().getMatchingStacks())),
                Collections.singletonList(recipe.getOutput()), Collections.emptyList(), Collections.emptyList(),
                () -> I18n.format("psychedelicraft.jei.drying_time",
                        seconds(PSConfig.balancing.dryingTableTickDuration), seconds(PSConfig.balancing.ironDryingTableTickDuration)));
    }

    private static ProcessRecipe mashing(MashingRecipe recipe) {
        FluidStack pool = recipe.getMinimumPool();
        return new ProcessRecipe(recipe.getIngredients().stream().map(i -> Arrays.asList(i.getMatchingStacks())).collect(Collectors.toList()),
                Collections.emptyList(), Collections.singletonList(pool), Collections.singletonList(recipe.finish(pool.amount)),
                () -> I18n.format("psychedelicraft.jei.mashing_hint"));
    }

    private static ProcessRecipe harvest(Item source, Item output) {
        return new ProcessRecipe(Arrays.asList(Collections.singletonList(new ItemStack(source)),
                Collections.singletonList(new ItemStack(Items.SHEARS))), Collections.singletonList(new ItemStack(output)),
                Collections.emptyList(), Collections.emptyList(), () -> I18n.format("psychedelicraft.jei.harvest_hint"));
    }

    private void registerFluidRecipes(IModRegistry registry) {
        List<ProcessRecipe> fermentation = new ArrayList<>();
        List<ProcessRecipe> maturation = new ArrayList<>();
        List<ProcessRecipe> distillation = new ArrayList<>();
        List<ProcessRecipe> withdrawal = new ArrayList<>();
        for (Fluid fluid : PSFluids.all()) {
            if (fluid instanceof BrewFluid) {
                FluidStack fresh = new FluidStack(fluid, 1000);
                addStages(fresh, false, fermentation);
                FluidStack firstFerment = BrewProcess.FERMENT.finish(fresh);
                FluidStack fullyFermented = BrewProcess.FERMENT.finish(firstFerment);
                addStages(firstFerment, true, maturation);
                addStages(fullyFermented, true, maturation);
                addDistillation(firstFerment, distillation);
                addDistillation(fullyFermented, distillation);
                BrewFluid brew = (BrewFluid) fluid;
                brew.addCreativeVariants(1000, false, stack -> addWithdrawal(stack, withdrawal));
                if (brew == PSFluids.AGAVE) brew.addCreativeVariants(1000, true, stack -> addWithdrawal(stack, withdrawal));
            } else {
                addWithdrawal(new FluidStack(fluid, 1000), withdrawal);
                if (PSFluids.isFlowerExtract(fluid)) addDistillation(new FluidStack(fluid, 2), distillation);
            }
        }
        FluidStack hot = new FluidStack(PSFluids.COFFEE, 250);
        hot.tag = new NBTTagCompound();
        hot.tag.setInteger("warmth", 2);
        addWithdrawal(hot, withdrawal);
        addWithdrawal(BrewProcess.COOL.finish(hot), withdrawal);
        fermentation.add(process(BrewProcess.COOL, hot));
        FluidStack slurry = new FluidStack(PSFluids.SLURRY, BrewProcess.SLURRY_PER_DIRT);
        fermentation.add(process(BrewProcess.HARDEN, slurry));
        maturation.add(process(BrewProcess.HARDEN, slurry));
        registry.addRecipes(fermentation, ProcessCategory.uid(FERMENTING));
        registry.addRecipes(maturation, ProcessCategory.uid(MATURING));
        registry.addRecipes(distillation, ProcessCategory.uid(DISTILLING));
        registry.addRecipes(withdrawal, ProcessCategory.uid(BARREL));
        registry.addRecipes(withdrawal, ProcessCategory.uid(FLASK));
        registry.addRecipes(withdrawal, ProcessCategory.uid(DISTILLERY));
    }

    private static void addStages(FluidStack input, boolean aging, List<ProcessRecipe> recipes) {
        BrewProcess step = BrewProcess.find(input, aging, false);
        while (step != BrewProcess.IDLE) {
            recipes.add(process(step, input));
            input = step.finish(input);
            step = BrewProcess.find(input, aging, false);
        }
    }

    private static void addDistillation(FluidStack input, List<ProcessRecipe> recipes) {
        while (BrewProcess.findDistillation(input) == BrewProcess.DISTILL) {
            recipes.add(process(BrewProcess.DISTILL, input));
            FluidStack byproduct = BrewProcess.distillationOutput(input);
            input = PSFluids.isFlowerExtract(input.getFluid()) && byproduct != null
                    ? new FluidStack(byproduct.getFluid(), 2, byproduct.tag) : BrewProcess.DISTILL.finish(input);
        }
    }

    private static ProcessRecipe process(BrewProcess step, FluidStack input) {
        List<FluidStack> outputs = new ArrayList<>(2);
        FluidStack remains = step.finish(input);
        if (remains != null) outputs.add(remains);
        if (step == BrewProcess.DISTILL) {
            FluidStack byproduct = BrewProcess.distillationOutput(input);
            if (byproduct != null) outputs.add(byproduct);
        }
        List<ItemStack> solids = step == BrewProcess.HARDEN ? Collections.singletonList(new ItemStack(Blocks.DIRT))
                : Collections.emptyList();
        return new ProcessRecipe(Collections.emptyList(), solids, Collections.singletonList(input), outputs,
                () -> I18n.format("psychedelicraft.jei.process." + step.key(), seconds(step.duration(input))));
    }

    private void addWithdrawal(FluidStack tank, List<ProcessRecipe> recipes) {
        for (Item item : vessels()) {
            ItemStack empty = new ItemStack(item);
            FluidVesselHandler vessel = FluidVesselHandler.forCrafting(empty);
            int amount = vessel.fill(tank.copy(), true);
            if (amount > 0 && (item != Items.BUCKET || amount == Fluid.BUCKET_VOLUME)) {
                ItemStack filled = vessel.getContainer();
                indexVessel(filled);
                recipes.add(new ProcessRecipe(Collections.singletonList(Collections.singletonList(empty)),
                        Collections.singletonList(filled), Collections.singletonList(new FluidStack(tank, amount)),
                        Collections.emptyList(), () -> I18n.format("psychedelicraft.jei.draw_hint")));
            }
        }
    }

    private void indexVessel(ItemStack stack) {
        if (stack.getItem() instanceof PortableFluidItem) {
            displayedVessels.putIfAbsent(stack.getItem().getRegistryName() + "/" + subtype(stack), stack);
        }
    }

    private void registerCoffee(IModRegistry registry) {
        List<IRecipeWrapper> recipes = new ArrayList<>();
        IVanillaRecipeFactory factory = registry.getJeiHelpers().getVanillaRecipeFactory();
        ItemStack cup = FluidVesselHandler.forCrafting(new ItemStack(PSItems.STONE_CUP)).craftContents(PSFluids.COFFEE, -1, null);
        addCoffeeHeating(factory, recipes, cup);
        List<Item> machines = new ArrayList<>(Arrays.asList(barrels()));
        Collections.addAll(machines, PSItems.MASH_TUB, PSItems.FLASK, PSItems.DISTILLERY);
        for (Item item : machines) {
            ItemStack machine = new ItemStack(item);
            machine.setTagInfo("fluid", FluidContents.write(new FluidStack(PSFluids.COFFEE, ((FluidMachineItem) item).getCapacity())));
            addCoffeeHeating(factory, recipes, machine);
        }
        registry.addRecipes(recipes, VanillaRecipeCategoryUid.SMELTING);
    }

    private void addCoffeeHeating(IVanillaRecipeFactory factory, List<IRecipeWrapper> recipes, ItemStack input) {
        ItemStack output = CoffeeHeating.getResult(input);
        while (output != null) {
            recipes.add(factory.createSmeltingRecipe(Collections.singletonList(input), output));
            indexVessel(output);
            input = output;
            output = CoffeeHeating.getResult(input);
        }
    }

    private static int seconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    private static final class FilledCrafting implements ICraftingRecipeWrapper {
        private final FillReceptacleRecipe recipe;

        private FilledCrafting(FillReceptacleRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return recipe.getRegistryName();
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(VanillaTypes.ITEM, recipe.getIngredients().stream()
                    .map(i -> Arrays.asList(i.getMatchingStacks())).collect(Collectors.toList()));
            List<ItemStack> outputs = recipe.getDisplayOutputs();
            ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(outputs));
            ingredients.setOutputLists(VanillaTypes.FLUID, Collections.singletonList(outputs.stream()
                    .map(FluidVesselHandler::forCrafting).map(FluidVesselHandler::getFluid)
                    .collect(Collectors.toList())));
        }
    }

    private static final class ColoredBottle implements IShapedCraftingRecipeWrapper {
        private final BottleRecipe recipe;
        private final List<List<ItemStack>> inputs;
        private final ItemStack output;

        private ColoredBottle(BottleRecipe recipe, EnumDyeColor color) {
            this.recipe = recipe;
            ItemStack glass = color == null ? ItemStack.EMPTY : new ItemStack(Blocks.STAINED_GLASS, 1, color.getMetadata());
            Item stainedGlass = Item.getItemFromBlock(Blocks.STAINED_GLASS);
            inputs = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (!glass.isEmpty() && ingredient.apply(glass)) {
                    inputs.add(Collections.singletonList(glass));
                } else {
                    inputs.add(Arrays.stream(ingredient.getMatchingStacks()).filter(stack -> stack.getItem() != stainedGlass)
                            .collect(Collectors.toList()));
                }
            }
            output = color == null ? recipe.getRecipeOutput() : recipe.coloredOutput(color);
        }

        @Override
        public ResourceLocation getRegistryName() {
            return recipe.getRegistryName();
        }

        @Override
        public int getWidth() {
            return recipe.getRecipeWidth();
        }

        @Override
        public int getHeight() {
            return recipe.getRecipeHeight();
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutput(VanillaTypes.ITEM, output);
        }
    }
}

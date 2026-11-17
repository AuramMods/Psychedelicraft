/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.compat.jei;

import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Build every display from the same recipes and transitions used by machines. */
final class ProcessRecipes {
    private ProcessRecipes() {}
    static Map<String, List<ProcessRecipe>> create(RecipeManager manager) {
        Map<String, List<ProcessRecipe>> result = new LinkedHashMap<>();
        for (String name : PsychedelicraftJeiPlugin.CATEGORIES) result.put(name, new ArrayList<>());
        for (DryingRecipe recipe : manager.getAllRecipesFor(PSRecipes.DRYING_TYPE.get())) {
            result.get("drying").add(new ProcessRecipe(Collections.nCopies(9, Arrays.asList(recipe.getInput().getItems())),
                    List.of(recipe.getOutput()), List.of(), List.of(), Component.translatable("psychedelicraft.jei.drying_time",
                    seconds(PSConfig.DRYING_TABLE_TICKS.get()), seconds(PSConfig.IRON_DRYING_TABLE_TICKS.get()))));
        }
        for (MashingRecipe recipe : manager.getAllRecipesFor(PSRecipes.MASHING_TYPE.get())) {
            var pool = recipe.getMinimumPool();
            result.get("mashing").add(new ProcessRecipe(recipe.getIngredients().stream().map(i -> Arrays.asList(i.getItems())).toList(),
                    List.of(), List.of(pool), List.of(recipe.finish(pool.getAmount())),
                    Component.translatable("psychedelicraft.jei.mashing_hint")));
        }
        result.get("harvest").add(harvest(PSItems.MORNING_GLORY_LATTICE.get(), PSItems.MORNING_GLORY.get()));
        result.get("harvest").add(harvest(PSItems.WINE_GRAPE_LATTICE.get(), PSItems.WINE_GRAPES.get()));
        result.get("harvest").add(harvest(PSItems.FRUITING_JUNIPER_LEAVES.get(), PSItems.JUNIPER_BERRIES.get()));
        for (var family : PSFluids.all()) {
            var fresh = new FluidStack(family.get(), 1000);
            if (family.profile instanceof BrewProfile brew) {
                stages(fresh, false, result.get("fermenting"));
                var first = BrewProcess.FERMENT.finish(fresh);
                var full = BrewProcess.FERMENT.finish(first);
                stages(first, true, result.get("maturing"));
                stages(full, true, result.get("maturing"));
                distillation(first, result.get("distilling"));
                distillation(full, result.get("distilling"));
                brew.addCreativeVariants(1000, false, fluid -> withdrawal(fluid, result));
                if (family == PSFluids.AGAVE) brew.addCreativeVariants(1000, true, fluid -> withdrawal(fluid, result));
            } else {
                withdrawal(fresh, result);
                if (PSFluids.isFlowerExtract(family.get())) distillation(new FluidStack(family.get(), 2), result.get("distilling"));
            }
        }
        var hot = new FluidStack(PSFluids.COFFEE.get(), 250);
        hot.getOrCreateTag().putInt("warmth", 2);
        withdrawal(hot, result);
        withdrawal(BrewProcess.COOL.finish(hot), result);
        result.get("fermenting").add(process(BrewProcess.COOL, hot));
        var harden = process(BrewProcess.HARDEN, new FluidStack(PSFluids.SLURRY.get(), BrewProcess.SLURRY_PER_DIRT));
        result.get("fermenting").add(harden);
        result.get("maturing").add(harden);
        return result;
    }
    private static void stages(FluidStack input, boolean aging, List<ProcessRecipe> output) {
        for (BrewProcess step = BrewProcess.find(input, aging, false); step != BrewProcess.IDLE;
             step = BrewProcess.find(input, aging, false)) {
            output.add(process(step, input));
            input = step.finish(input);
        }
    }
    private static void distillation(FluidStack input, List<ProcessRecipe> output) {
        while (BrewProcess.findDistillation(input) == BrewProcess.DISTILL) {
            output.add(process(BrewProcess.DISTILL, input));
            var byproduct = BrewProcess.distillationOutput(input);
            input = PSFluids.isFlowerExtract(input.getFluid()) && !byproduct.isEmpty()
                    ? new FluidStack(byproduct, 2) : BrewProcess.DISTILL.finish(input);
        }
    }
    private static ProcessRecipe process(BrewProcess step, FluidStack input) {
        List<FluidStack> outputs = new ArrayList<>();
        var remains = step.finish(input);
        if (!remains.isEmpty()) outputs.add(remains);
        if (step == BrewProcess.DISTILL) {
            var byproduct = BrewProcess.distillationOutput(input);
            if (!byproduct.isEmpty()) outputs.add(byproduct);
        }
        return new ProcessRecipe(List.of(), step == BrewProcess.HARDEN ? List.of(new ItemStack(Items.DIRT)) : List.of(),
                List.of(input.copy()), outputs, Component.translatable("psychedelicraft.jei.process." + step.key(), seconds(step.duration(input))));
    }
    private static ProcessRecipe harvest(Item source, Item output) {
        return new ProcessRecipe(List.of(List.of(new ItemStack(source)), List.of(new ItemStack(Items.SHEARS))),
                List.of(new ItemStack(output)), List.of(), List.of(), Component.translatable("psychedelicraft.jei.harvest_hint"));
    }
    private static void withdrawal(FluidStack tank, Map<String, List<ProcessRecipe>> output) {
        for (Item item : vessels()) {
            var empty = new ItemStack(item);
            var handler = FluidVesselHandler.forCrafting(empty);
            int amount = handler == null ? 0 : handler.fill(tank.copy(), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (amount <= 0 || item == Items.BUCKET && amount != 1000) continue;
            var display = new ProcessRecipe(List.of(List.of(new ItemStack(item))), List.of(handler.getContainer()),
                    List.of(new FluidStack(tank, amount)), List.of(), Component.translatable("psychedelicraft.jei.draw_hint"));
            output.get("barrel_draw").add(display);
            output.get("flask_draw").add(display);
            output.get("distillery_draw").add(display);
        }
    }
    static List<Item> vessels() {
        return List.of(PSItems.WOODEN_MUG.get(), PSItems.STONE_CUP.get(), PSItems.GLASS_CHALICE.get(), PSItems.SHOT_GLASS.get(),
                PSItems.BOTTLE.get(), PSItems.MOLOTOV_COCKTAIL.get(), PSItems.SYRINGE.get(), Items.BOWL, Items.GLASS_BOTTLE, Items.BUCKET);
    }
    static int seconds(int ticks) { return Math.max(1, (ticks + 19) / 20); }
}

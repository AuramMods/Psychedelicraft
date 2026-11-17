package ivorius.psychedelicraft.gametest;

import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.recipe.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;

/** These run on a dedicated server without JEI; display helpers cannot require client APIs. */
@GameTestHolder("psychedelicrafttests")
@PrefixGameTestTemplate(false)
public final class RecipeDisplayGameTests {
    @GameTest(template="empty") public static void heatingExamplesUseNativeSmeltingAndPreserveTheirInputs(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        int count = 0;
        for (SmeltingRecipe recipe : recipes) {
            if (!(recipe instanceof HeatingRecipe heating)) continue;
            var inputs = heating.getDisplayInputs();
            check(!inputs.isEmpty(), "heating has examples");
            for (var input : inputs) {
                var saved = input.save(new CompoundTag());
                var output = heating.assemble(new SimpleContainer(input), RegistryAccess.EMPTY);
                check(!output.isEmpty() && input.save(new CompoundTag()).equals(saved), "display assembly is real and read-only");
                var before = FluidVesselHandler.forCrafting(input).getFluid();
                var after = FluidVesselHandler.forCrafting(output).getFluid();
                check(before.getAmount() == after.getAmount(), "heating display conserves fluid");
                check(BrewProfile.stage(after, "warmth", 2) > BrewProfile.stage(before, "warmth", 2), "heating advances actual warmth");
                count++;
            }
        }
        check(count >= 4, "full cup and machine stages are represented");
        helper.succeed();
    }
    @GameTest(template="empty") public static void fillExamplesReturnIndependentMetadata(GameTestHelper helper) {
        var recipe = (FillReceptacleRecipe) helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("psychedelicraft:fluids/holder_with_coffee")).orElseThrow();
        var first = recipe.getDisplayOutputs();
        check(!first.isEmpty() && first.stream().anyMatch(stack -> !stack.isEmpty()), "fill has a valid display");
        var valid = first.stream().filter(stack -> !stack.isEmpty()).findFirst().orElseThrow();
        valid.getOrCreateTag().putInt("probe", 29);
        check(recipe.getDisplayOutputs().stream().noneMatch(stack -> stack.hasTag() && stack.getTag().contains("probe")), "display output copies are independent");
        helper.succeed();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

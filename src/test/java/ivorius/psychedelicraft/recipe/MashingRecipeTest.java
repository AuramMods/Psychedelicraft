package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonParser;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.crafting.JsonContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MashingRecipeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void completedBatchRequiresEveryRecipeIngredient() {
        MashingRecipe recipe = (MashingRecipe) new MashingRecipe.Factory().parse(new JsonContext("psychedelicraft"),
                new JsonParser().parse("{\"base_fluid\":{\"fluid\":\"minecraft:water\"},"
                        + "\"result\":{\"fluid\":\"psychedelicraft:wheat\"},"
                        + "\"ingredients\":[{\"item\":\"minecraft:wheat\"},{\"item\":\"minecraft:apple\"}]}").getAsJsonObject());
        ItemStack wheat = new ItemStack(Items.WHEAT);
        ItemStack apple = new ItemStack(Items.APPLE);
        assertTrue(recipe.matchesIngredients(Arrays.asList(apple, wheat)));
        assertFalse(recipe.matchesIngredients(Collections.singletonList(wheat)));
        assertFalse(recipe.matchesIngredients(Arrays.asList(wheat, wheat)));
    }
}

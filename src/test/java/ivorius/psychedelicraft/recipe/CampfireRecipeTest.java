package ivorius.psychedelicraft.recipe;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CampfireRecipeTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void nativeCampfireRecipesCookOneItemWithoutChangingTheInput() {
        Item[][] recipes = {
                {Items.BEEF, Items.COOKED_BEEF},
                {Items.CHICKEN, Items.COOKED_CHICKEN},
                {Items.MUTTON, Items.COOKED_MUTTON},
                {Items.PORKCHOP, Items.COOKED_PORKCHOP},
                {Items.RABBIT, Items.COOKED_RABBIT},
                {Items.POTATO, Items.BAKED_POTATO}
        };
        for (Item[] recipe : recipes) {
            ItemStack input = new ItemStack(recipe[0], 5);
            ItemStack output = CampfireRecipe.cook(input);
            assertSame(recipe[1], output.getItem());
            assertEquals(1, output.getCount());
            assertEquals(5, input.getCount());
        }
    }

    @Test
    public void codAndSalmonRetainSpeciesAndOtherFurnaceRecipesAreRejected() {
        for (int species = 0; species < 2; species++) {
            ItemStack output = CampfireRecipe.cook(new ItemStack(Items.FISH, 3, species));
            assertSame(Items.COOKED_FISH, output.getItem());
            assertEquals(1, output.getCount());
            assertEquals(species, output.getMetadata());
        }
        assertTrue(CampfireRecipe.cook(new ItemStack(Items.FISH, 1, 2)).isEmpty());
        assertTrue(CampfireRecipe.cook(new ItemStack(Items.FISH, 1, 3)).isEmpty());
        assertTrue(CampfireRecipe.cook(new ItemStack(Items.IRON_INGOT)).isEmpty());
        assertTrue(CampfireRecipe.cook(new ItemStack(Items.COOKED_BEEF)).isEmpty());
        assertTrue(CampfireRecipe.cook(ItemStack.EMPTY).isEmpty());
    }
}

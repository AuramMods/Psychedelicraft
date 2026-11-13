package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonParser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.JsonContext;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FluidAwareShapelessRecipeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void drainsOnlyTheSlotAssignedToTheFluidIngredient() {
        IRecipe recipe = parse("{\"fluid\":{\"fluid\":\"minecraft:water\",\"level\":1000}},"
                + "[{\"item\":\"minecraft:water_bucket\"},{\"item\":\"minecraft:lava_bucket\"}]");
        InventoryCrafting grid = grid();
        grid.setInventorySlotContents(0, new ItemStack(Items.WATER_BUCKET).setStackDisplayName("Water sample"));
        grid.setInventorySlotContents(3, new ItemStack(Items.LAVA_BUCKET).setStackDisplayName("Lava reagent"));

        assertTrue(recipe.matches(grid, null));
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid);
        assertSame(Items.BUCKET, remaining.get(0).getItem());
        assertEquals("Water sample", remaining.get(0).getDisplayName());
        assertSame(Items.BUCKET, remaining.get(3).getItem());
        assertFalse(remaining.get(3).hasDisplayName());
        assertSame(Items.WATER_BUCKET, grid.getStackInSlot(0).getItem());

        grid.setInventorySlotContents(1, new ItemStack(Items.PAPER));
        assertFalse(recipe.matches(grid, null));
    }

    @Test
    public void checksTheFluidVolumeAndAttributes() {
        InventoryCrafting grid = grid();
        grid.setInventorySlotContents(1, new ItemStack(Items.WATER_BUCKET));
        assertTrue(parse("{\"fluid\":\"minecraft:water\"}").matches(grid, null));
        assertFalse(parse("{\"fluid\":{\"fluid\":\"minecraft:water\",\"level\":1001}}").matches(grid, null));
        assertFalse(parse("{\"fluid\":{\"fluid\":\"minecraft:water\",\"attributes\":{\"batch\":1}}}").matches(grid, null));
    }

    private static IRecipe parse(String ingredients) {
        return new FluidAwareShapelessRecipe.Factory().parse(new JsonContext("psychedelicraft"),
                new JsonParser().parse("{\"ingredients\":[" + ingredients
                        + "],\"result\":{\"item\":\"minecraft:stick\"}}").getAsJsonObject());
    }

    private static InventoryCrafting grid() {
        return new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 2);
    }
}

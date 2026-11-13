package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonParser;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ChangeReceptacleRecipeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void transfersAFullBucketWithoutReturningAnotherBucket() {
        IRecipe recipe = conversion("bucket", "{\"item\":\"minecraft:paper\"}");
        InventoryCrafting grid = grid(new ItemStack(Items.PAPER),
                new ItemStack(Items.LAVA_BUCKET).setStackDisplayName("Sample bucket"));

        assertTrue(recipe.matches(grid, null));
        ItemStack result = recipe.getCraftingResult(grid);
        FluidStack fluid = FluidVesselHandler.forCrafting(result).getFluid();
        assertSame(Items.LAVA_BUCKET, result.getItem());
        assertSame(FluidRegistry.LAVA, fluid.getFluid());
        assertEquals(1000, fluid.amount);
        assertTrue(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty));
        assertEquals("Sample bucket", grid.getStackInSlot(3).getDisplayName());
    }

    @Test
    public void refusesToLoseFluidOrPickBetweenTwoVessels() {
        InventoryCrafting grid = grid(new ItemStack(Items.PAPER), new ItemStack(Items.LAVA_BUCKET));
        assertFalse(conversion("glass_bottle", "{\"item\":\"minecraft:paper\"}").matches(grid, null));
        assertFalse(conversion("stick", "{\"item\":\"minecraft:paper\"}").matches(grid, null));

        grid.setInventorySlotContents(0, new ItemStack(Items.BOWL));
        assertFalse(conversion("bucket", "{\"item\":\"minecraft:bowl\"}").matches(grid, null));
    }

    private static IRecipe conversion(String result, String reagent) {
        return new ChangeReceptacleRecipe.Factory().parse(new JsonContext("psychedelicraft"),
                new JsonParser().parse("{\"ingredients\":[{\"item\":\"minecraft:lava_bucket\"}," + reagent
                        + "],\"result\":{\"item\":\"minecraft:" + result + "\"}}").getAsJsonObject());
    }

    private static InventoryCrafting grid(ItemStack first, ItemStack last) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 2);
        grid.setInventorySlotContents(0, first);
        grid.setInventorySlotContents(3, last);
        return grid;
    }
}

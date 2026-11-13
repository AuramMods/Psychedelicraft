package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonParser;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FillReceptacleRecipeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void filledOutputReusesItsVesselAndKeepsTheReagentRemainder() {
        IRecipe recipe = new FillReceptacleRecipe.Factory().parse(new JsonContext("psychedelicraft"),
                new JsonParser().parse("{\"receptical\":{\"item\":\"minecraft:milk_bucket\"},"
                        + "\"ingredients\":[{\"item\":\"minecraft:water_bucket\"}],"
                        + "\"result\":{\"fluid\":\"psychedelicraft:agave\",\"level\":100}}").getAsJsonObject());
        InventoryCrafting inventory = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 2);
        inventory.setInventorySlotContents(0, new ItemStack(Items.WATER_BUCKET));
        inventory.setInventorySlotContents(3, new ItemStack(Items.MILK_BUCKET).setStackDisplayName("Sample bucket"));

        assertTrue(recipe.matches(inventory, null));
        ItemStack result = recipe.getCraftingResult(inventory);
        FluidStack fluid = FluidVesselHandler.forCrafting(result).getFluid();
        assertSame(PSFluids.AGAVE, fluid.getFluid());
        assertEquals(1000, fluid.amount);
        assertEquals("Sample bucket", result.getDisplayName());

        NonNullList<ItemStack> remaining = recipe.getRemainingItems(inventory);
        assertSame(Items.BUCKET, remaining.get(0).getItem());
        assertTrue(remaining.get(3).isEmpty());
        assertSame(Items.MILK_BUCKET, inventory.getStackInSlot(3).getItem());
    }
}

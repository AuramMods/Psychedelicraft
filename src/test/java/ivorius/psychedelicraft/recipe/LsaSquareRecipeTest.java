package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LsaSquareRecipeTest {
    private final LsaSquareRecipe recipe = new LsaSquareRecipe();

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void drainsOneMillibucketWithoutChangingTheOriginalStack() {
        ItemStack input = extract(new ItemStack(PSItems.FILLED_BOWL), 3, 2);
        InventoryCrafting inventory = withPaperAnd(input);

        assertTrue(recipe.matches(inventory, null));
        assertSame(PSItems.LSA_SQUARE, recipe.getCraftingResult(inventory).getItem());
        ItemStack remainder = recipe.getRemainingItems(inventory).get(3);
        FluidStack remainingFluid = PSItems.FILLED_BOWL.contents(remainder);
        assertEquals(2, remainingFluid.amount);
        assertEquals(PSItems.FILLED_BOWL.contents(input).tag, remainingFluid.tag);
        assertEquals("Sample", remainder.getDisplayName());
        assertEquals(3, PSItems.FILLED_BOWL.contents(input).amount);
    }

    @Test
    public void returnsTheEmptyBowlAfterTheLastMillibucket() {
        ItemStack input = extract(new ItemStack(PSItems.FILLED_BOWL), 1, 2);
        ItemStack remainder = recipe.getRemainingItems(withPaperAnd(input)).get(3);

        assertSame(Items.BOWL, remainder.getItem());
        assertEquals("Sample", remainder.getDisplayName());
        assertFalse(remainder.getTagCompound().hasKey("fluid"));
    }

    @Test
    public void rejectsTheWrongStageAndAnInjectionVessel() {
        assertFalse(recipe.matches(withPaperAnd(extract(new ItemStack(PSItems.FILLED_BOWL), 3, 1)), null));
        assertFalse(recipe.matches(withPaperAnd(extract(new ItemStack(PSItems.SYRINGE), 3, 2)), null));

        InventoryCrafting extraPaper = withPaperAnd(extract(new ItemStack(PSItems.FILLED_BOWL), 3, 2));
        extraPaper.setInventorySlotContents(2, new ItemStack(Items.PAPER));
        assertFalse(recipe.matches(extraPaper, null));
        assertTrue(recipe.getRemainingItems(extraPaper).get(3).isEmpty());
    }

    private static InventoryCrafting withPaperAnd(ItemStack vessel) {
        InventoryCrafting inventory = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 2);
        inventory.setInventorySlotContents(0, new ItemStack(Items.PAPER));
        inventory.setInventorySlotContents(3, vessel);
        return inventory;
    }

    private static ItemStack extract(ItemStack vessel, int volume, int stage) {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger(BrewFluid.DISTILLATION, stage);
        attributes.setString("batch", "seed stock");
        vessel.setTagInfo("fluid", FluidContents.write(new FluidStack(PSFluids.MORNING_GLORY_EXTRACT, volume, attributes)));
        vessel.setStackDisplayName("Sample");
        return vessel;
    }
}

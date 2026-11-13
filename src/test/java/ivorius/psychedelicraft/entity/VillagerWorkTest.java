package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.DryingRecipe;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VillagerWorkTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void loadsOneCompleteBatchWithoutMovingTheRemainingIngredients() {
        InventoryBasic inventory = new InventoryBasic("test", false, 8);
        inventory.setInventorySlotContents(0, new ItemStack(PSItems.TOBACCO_LEAVES, 5));
        inventory.setInventorySlotContents(2, new ItemStack(PSItems.CANNABIS_LEAF, 3));
        inventory.setInventorySlotContents(5, new ItemStack(PSItems.TOBACCO_LEAVES, 6));
        DryingTableBlockEntity table = new DryingTableBlockEntity();

        assertTrue(VillagerWork.loadBatch(inventory, table));
        assertSame(PSItems.DRIED_TOBACCO, DryingRecipe.match(table).getOutput().getItem());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(3, inventory.getStackInSlot(2).getCount());
        assertEquals(2, inventory.getStackInSlot(5).getCount());
    }

    @Test
    public void incompleteBatchesAndOccupiedTablesRemainUntouched() {
        InventoryBasic inventory = new InventoryBasic("test", false, 8);
        inventory.setInventorySlotContents(0, new ItemStack(PSItems.CANNABIS_LEAF, 8));
        inventory.setInventorySlotContents(1, new ItemStack(PSItems.CANNABIS_BUDS, 8));
        DryingTableBlockEntity table = new DryingTableBlockEntity();

        assertFalse(VillagerWork.loadBatch(inventory, table));
        assertTrue(table.isEmpty());
        inventory.getStackInSlot(0).grow(1);
        table.setInventorySlotContents(1, new ItemStack(PSItems.TOBACCO_LEAVES));
        assertFalse(VillagerWork.loadBatch(inventory, table));
        assertEquals(9, inventory.getStackInSlot(0).getCount());
        assertEquals(8, inventory.getStackInSlot(1).getCount());
        assertSame(PSItems.TOBACCO_LEAVES, table.getStackInSlot(1).getItem());
    }

    @Test
    public void previewMatchesBothCostsAndKeepsOfferStacksUntouched() {
        MerchantRecipeList offers = new MerchantRecipeList();
        ItemStack tagged = new ItemStack(PSItems.WINE_GRAPES, 3);
        tagged.setStackDisplayName("Preview");
        offers.add(new MerchantRecipe(new ItemStack(Items.EMERALD, 7), tagged));
        offers.add(new MerchantRecipe(new ItemStack(Items.WHEAT), new ItemStack(Items.EMERALD), new ItemStack(Items.BREAD)));
        offers.add(new MerchantRecipe(new ItemStack(Items.EMERALD), ItemStack.EMPTY, new ItemStack(Items.CARROT), 1, 1));
        offers.add(new MerchantRecipe(new ItemStack(Blocks.EMERALD_BLOCK), new ItemStack(Items.APPLE)));

        List<ItemStack> results = VillagerTradePreview.matchingResults(offers, new ItemStack(Items.EMERALD));
        assertEquals(2, results.size());
        assertTrue(ItemStack.areItemStacksEqual(tagged, results.get(0)));
        assertSame(Items.BREAD, results.get(1).getItem());
        results.get(0).shrink(1);
        assertEquals(3, offers.get(0).getItemToSell().getCount());
    }
}

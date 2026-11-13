package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class VillagerStockTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        ForgeRegistries.ITEMS.register(PSItems.LSA_SQUARE);
    }

    @Test
    public void restockPreservesOfferTermsAndMissedRestocksLowerDemand() {
        MerchantRecipe soldOut = new MerchantRecipe(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.PAPER, 2),
                new ItemStack(PSItems.LSA_SQUARE, 3), 2, 2);
        NBTTagCompound saved = soldOut.writeToTags();
        saved.setBoolean("rewardExp", false);
        MerchantRecipeList offers = new MerchantRecipeList();
        offers.add(new MerchantRecipe(saved));
        MerchantRecipe external = new MerchantRecipe(new ItemStack(Items.EMERALD), new ItemStack(Items.APPLE));
        offers.add(external);
        NBTTagCompound data = new NBTTagCompound();

        VillagerCareers.updateStock(data, offers, VillagerCareers.DEALER, 0);

        String demandKey = PSItems.LSA_SQUARE.getRegistryName().toString();
        assertEquals(2, data.getCompoundTag("Demand").getInteger(demandKey));
        MerchantRecipe restocked = offers.get(0);
        assertEquals(6, restocked.getItemToBuy().getCount());
        assertEquals(0, restocked.getToolUses());
        assertEquals(2, restocked.getMaxTradeUses());
        assertEquals(2, restocked.getSecondItemToBuy().getCount());
        assertSame(Items.PAPER, restocked.getSecondItemToBuy().getItem());
        assertSame(PSItems.LSA_SQUARE, restocked.getItemToSell().getItem());
        assertFalse(restocked.getRewardsExp());
        assertSame(external, offers.get(1));

        restocked.incrementToolUses();
        VillagerCareers.updateStock(data, offers, VillagerCareers.DEALER, 2);

        assertEquals(-2, data.getCompoundTag("Demand").getInteger(demandKey));
        assertEquals(3, offers.get(0).getItemToBuy().getCount());
        assertEquals(0, offers.get(0).getToolUses());
        assertFalse(offers.get(0).getRewardsExp());
        assertSame(external, offers.get(1));
    }
}

package ivorius.psychedelicraft.entity;

import com.google.gson.JsonParser;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.DryingRecipe;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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
        ForgeRegistries.ITEMS.register(PSItems.TOBACCO_LEAVES);
        ForgeRegistries.ITEMS.register(PSItems.DRIED_TOBACCO);
        ForgeRegistries.RECIPES.register(new DryingRecipe.Factory().parse(new JsonContext("psychedelicraft"),
                new JsonParser().parse("{\"ingredient\":{\"item\":\"psychedelicraft:tobacco\"},"
                        + "\"result\":{\"item\":\"psychedelicraft:dried_tobacco\",\"count\":3}}").getAsJsonObject())
                .setRegistryName(new ResourceLocation("psychedelicraft", "test_drying_tobacco")));
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

    @Test
    public void adultWorkUsesTheSameWindowOnEachWorldDay() {
        WorkWorld world = new WorkWorld();
        EntityVillager villager = new EntityVillager(world);
        for (long day : new long[] {-24000, 0, 24000}) {
            world.dayTime = day + 1999;
            assertFalse(VillagerWork.canWork(villager));
            world.dayTime++;
            assertTrue(VillagerWork.canWork(villager));
            world.dayTime = day + 8999;
            assertTrue(VillagerWork.canWork(villager));
            world.dayTime++;
            assertFalse(VillagerWork.canWork(villager));
        }
        world.dayTime = 2000;
        villager.setGrowingAge(-1);
        assertFalse(VillagerWork.canWork(villager));
    }

    private static final class WorkWorld extends World {
        private long dayTime;

        private WorkWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        @Override
        public long getWorldTime() {
            return dayTime;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return false;
        }
    }
}

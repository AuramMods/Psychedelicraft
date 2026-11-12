package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ChestLootTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void lootSwitchesAreIndependentAndPreserveExistingPools() {
        PSConfig.Generation options = PSConfig.balancing.worldGeneration;
        boolean oldDungeons = options.dungeonChests;
        boolean oldVillages = options.villageChests;
        try {
            options.dungeonChests = true;
            options.villageChests = false;
            assertNotNull(supplement(LootTableList.CHESTS_SIMPLE_DUNGEON));
            assertNotNull(supplement(LootTableList.CHESTS_ABANDONED_MINESHAFT));
            assertNull(supplement(LootTableList.CHESTS_VILLAGE_BLACKSMITH));

            options.dungeonChests = false;
            options.villageChests = true;
            assertNull(supplement(LootTableList.CHESTS_SIMPLE_DUNGEON));
            assertNull(supplement(LootTableList.CHESTS_ABANDONED_MINESHAFT));
            assertNotNull(supplement(LootTableList.CHESTS_VILLAGE_BLACKSMITH));
            assertNull(supplement(LootTableList.CHESTS_DESERT_PYRAMID));
        } finally {
            options.dungeonChests = oldDungeons;
            options.villageChests = oldVillages;
        }
    }

    private static LootPool supplement(ResourceLocation location) {
        LootPool existing = new LootPool(new LootEntry[0], new LootCondition[0],
                new RandomValueRange(1), new RandomValueRange(0), "main");
        LootTable table = new LootTable(new LootPool[]{existing});
        ChestLoot.load(new LootTableLoadEvent(location, table, null));
        assertSame(existing, table.getPool("main"));
        return table.getPool("psychedelicraft:chest_loot");
    }
}

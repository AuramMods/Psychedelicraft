/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class ChestLoot {
    private static final LootCondition[] NO_CONDITIONS = new LootCondition[0];
    private static final String ADDITIONAL_POOL = "psychedelicraft:chest_loot";
    private static final ResourceLocation MINESHAFT = Psychedelicraft.id("chests/abandoned_mineshaft");
    private static final ResourceLocation DUNGEON = Psychedelicraft.id("chests/simple_dungeon");
    private static final ResourceLocation[] VILLAGE_PROFILES = {
            Psychedelicraft.id("chests/village/village_shepherd"),
            Psychedelicraft.id("chests/village/village_tannery"),
            Psychedelicraft.id("chests/village/village_temple"),
            Psychedelicraft.id("chests/village/village_toolsmith"),
            Psychedelicraft.id("chests/village/village_weaponsmith")
    };

    private ChestLoot() {
    }

    public static void register() {
        LootTableList.register(MINESHAFT);
        LootTableList.register(DUNGEON);
        for (ResourceLocation profile : VILLAGE_PROFILES) LootTableList.register(profile);
    }

    @SubscribeEvent
    public static void load(LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        PSConfig.Generation config = PSConfig.balancing.worldGeneration;
        if (id.equals(LootTableList.CHESTS_ABANDONED_MINESHAFT) && config.dungeonChests) {
            addSupplement(event, MINESHAFT);
        } else if (id.equals(LootTableList.CHESTS_SIMPLE_DUNGEON) && config.dungeonChests) {
            addSupplement(event, DUNGEON);
        } else if (id.equals(LootTableList.CHESTS_VILLAGE_BLACKSMITH) && config.villageChests) {
            addSupplement(event, VILLAGE_PROFILES);
        } else if (id.equals(LootTableList.GAMEPLAY_FISHING_TREASURE)) {
            LootPool treasure = event.getTable().getPool("main");
            if (treasure != null) treasure.addEntry(new LootEntryItem(PSItems.NAUTILUS_SHELL, 1, 0,
                    new LootFunction[0], NO_CONDITIONS, "psychedelicraft:nautilus_shell"));
        }
    }

    private static void addSupplement(LootTableLoadEvent event, ResourceLocation... profiles) {
        LootEntry[] entries = new LootEntry[profiles.length];
        for (int index = 0; index < profiles.length; index++) {
            ResourceLocation profile = profiles[index];
            entries[index] = new LootEntryTable(profile, 1, 0, NO_CONDITIONS, profile.toString());
        }
        event.getTable().addPool(new LootPool(entries, NO_CONDITIONS, new RandomValueRange(1),
                new RandomValueRange(0), ADDITIONAL_POOL));
    }
}

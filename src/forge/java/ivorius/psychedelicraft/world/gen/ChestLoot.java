/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraft.world.storage.loot.functions.SetNBT;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class ChestLoot {
    private static final LootCondition[] NO_CONDITIONS = new LootCondition[0];
    private static final String ADDITIONAL_POOL = "psychedelicraft:chest_loot";
    private static final String VILLAGE_PROFILE_POOL = "psychedelicraft:village_profile";
    private static final RandomValueRange ONE_ROLL = new RandomValueRange(1);
    private static final RandomValueRange NO_BONUS = new RandomValueRange(0);

    private static final Roll[] MINESHAFT = {
            item("wine_grapes", 8, 3, 8),
            item("wooden_mug", 5, 2, 16),
            item("juniper_berries", 2, 1, 8),
            item("dried_tobacco", 6, 1, 16),
            item("smoking_pipe", 3, 1, 1),
            item("cigarette", 5, 1, 8)
    };
    private static final Roll[] DUNGEON = {
            item("wine_grapes", 8, 1, 8),
            item("glass_chalice", 5, 2, 4),
            item("wooden_mug", 2, 1, 16),
            item("juniper_berries", 10, 1, 8),
            item("dried_tobacco", 3, 1, 16)
    };
    private static final VillageProfile[] VILLAGE_PROFILES = {
            new VillageProfile("shepherd",
                    item("wooden_mug", 1, 1, 16),
                    item("cigarette", 1, 1, 16),
                    filled("wooden_mug", 3, 4, "coffee", 500)),
            new VillageProfile("tannery",
                    item("cigarette", 4, 1, 11)),
            new VillageProfile("temple",
                    item("wine_grapes", 8, 3, 10),
                    item("wooden_mug", 1, 1, 16),
                    item("cigarette", 2, 1, 16),
                    filled("wooden_mug", 3, 4, "coffee", 500),
                    item("stone_cup", 1, 1, 1),
                    item("hash_muffin", 1, 1, 8)),
            new VillageProfile("toolsmith",
                    item("wooden_mug", 10, 1, 3),
                    item("cigarette", 1, 1, 16),
                    item("cigar", 2, 1, 4),
                    item("joint", 1, 1, 1),
                    filled("wooden_mug", 1, 4, "coffee", 500),
                    filled("stone_cup", 1, 1, "peyote_juice", 50),
                    filled("syringe", 1, 1, "cocaine", 10),
                    filled("syringe", 1, 1, "caffeine", 10),
                    item("hash_muffin", 1, 1, 8)),
            new VillageProfile("weaponsmith",
                    item("wooden_mug", 3, 1, 16),
                    item("cigarette", 1, 1, 16),
                    item("cigar", 2, 1, 2),
                    filled("wooden_mug", 1, 4, "coffee", 500),
                    item("hash_muffin", 3, 1, 8))
    };

    private ChestLoot() {
    }

    public static void register() {
        for (VillageProfile profile : VILLAGE_PROFILES) {
            LootTableList.register(profile.id);
        }
    }

    @SubscribeEvent
    public static void load(LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        PSConfig.Generation config = PSConfig.balancing.worldGeneration;
        if (id.equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)) {
            if (config.dungeonChests) {
                addDungeonPool(event, MINESHAFT);
            }
        } else if (id.equals(LootTableList.CHESTS_SIMPLE_DUNGEON)) {
            if (config.dungeonChests) {
                addDungeonPool(event, DUNGEON);
            }
        } else if (id.equals(LootTableList.CHESTS_VILLAGE_BLACKSMITH)) {
            if (config.villageChests) {
                LootEntry[] entries = new LootEntry[VILLAGE_PROFILES.length];
                for (int index = 0; index < entries.length; index++) {
                    ResourceLocation profile = VILLAGE_PROFILES[index].id;
                    entries[index] = new LootEntryTable(profile, 1, 0, NO_CONDITIONS, profile.toString());
                }
                event.getTable().addPool(new LootPool(entries, NO_CONDITIONS, ONE_ROLL, NO_BONUS, ADDITIONAL_POOL));
            }
        } else {
            for (VillageProfile profile : VILLAGE_PROFILES) {
                if (profile.id.equals(id)) {
                    LootPool pool = event.getTable().getPool(VILLAGE_PROFILE_POOL);
                    for (int index = 0; index < profile.rolls.length; index++) {
                        pool.addEntry(profile.rolls[index].create(index));
                    }
                    return;
                }
            }
        }
    }

    private static void addDungeonPool(LootTableLoadEvent event, Roll[] rolls) {
        LootEntry[] entries = new LootEntry[rolls.length];
        for (int index = 0; index < entries.length; index++) {
            entries[index] = rolls[index].create(index);
        }
        event.getTable().addPool(new LootPool(entries, NO_CONDITIONS, ONE_ROLL, NO_BONUS, ADDITIONAL_POOL));
    }

    private static Roll item(String item, int weight, int min, int max) {
        return new Roll(item, weight, min, max, null);
    }

    private static Roll filled(String item, int weight, int max, String fluid, int amount) {
        NBTTagCompound contents = new NBTTagCompound();
        contents.setString("id", Psychedelicraft.id(fluid).toString());
        contents.setInteger("level", amount);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("fluid", contents);
        return new Roll(item, weight, 1, max, tag);
    }

    private static final class VillageProfile {
        private final ResourceLocation id;
        private final Roll[] rolls;

        private VillageProfile(String role, Roll... rolls) {
            id = Psychedelicraft.id("chests/village/village_" + role);
            this.rolls = rolls;
        }
    }

    private static final class Roll {
        private final ResourceLocation id;
        private final int weight;
        private final RandomValueRange count;
        private final NBTTagCompound tag;

        private Roll(String item, int weight, int min, int max, @Nullable NBTTagCompound tag) {
            id = Psychedelicraft.id(item);
            this.weight = weight;
            count = new RandomValueRange(min, max);
            this.tag = tag;
        }

        private LootEntry create(int index) {
            String name = id + "#" + index;
            Item item = ForgeRegistries.ITEMS.getValue(id);
            // Reserve the donor's probability until the corresponding item has a Forge registration.
            if (item == null || item == Items.AIR) {
                return new LootEntryEmpty(weight, 0, NO_CONDITIONS, name);
            }
            LootFunction setCount = new SetCount(NO_CONDITIONS, count);
            LootFunction[] functions = tag == null
                    ? new LootFunction[]{setCount}
                    : new LootFunction[]{setCount, new SetNBT(NO_CONDITIONS, tag)};
            return new LootEntryItem(item, weight, 0, functions, NO_CONDITIONS, name);
        }
    }
}

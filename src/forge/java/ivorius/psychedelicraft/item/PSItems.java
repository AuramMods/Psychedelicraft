/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSItems {
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final CreativeTabs GENERAL = new CreativeTabs("psychedelicraft.general") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(COFFEE_BEANS);
        }
    };

    public static final Item COFFEE_BEANS = named("coffee_beans", new Item());
    public static final Item BROWN_MAGIC_MUSHROOMS = mushroom("brown_magic_mushrooms", DrugType.BROWN_SHROOMS);
    public static final Item RED_MAGIC_MUSHROOMS = mushroom("red_magic_mushrooms", DrugType.RED_SHROOMS);
    public static final Item CANNABIS_SEEDS = named("cannabis_seeds", new CropSeedItem(PSBlocks.CANNABIS, "cannabis_seeds"));
    public static final Item CANNABIS_LEAF = named("cannabis_leaf", new Item());
    public static final Item CANNABIS_BUDS = named("cannabis_buds", new Item());
    public static final Item DRIED_CANNABIS_LEAF = named("dried_cannabis_leaf", new Item());
    public static final Item DRIED_CANNABIS_BUDS = named("dried_cannabis_buds", new Item());
    public static final Item HOP_SEEDS = named("hop_seeds", new CropSeedItem(PSBlocks.HOP, "hop_seeds"));
    public static final Item HOP_CONES = named("hop_cones", new Item());
    public static final Item COCA_SEEDS = named("coca_seeds", new CropSeedItem(PSBlocks.COCA, "coca_seeds"));
    public static final Item COCA_LEAVES = named("coca_leaves", new Item());
    public static final Item DRIED_COCA_LEAVES = named("dried_coca_leaves", new Item());
    public static final Item TOBACCO_SEEDS = named("tobacco_seeds", new CropSeedItem(PSBlocks.TOBACCO, "tobacco_seeds"));
    public static final Item TOBACCO_LEAVES = named("tobacco", new Item());
    public static final Item DRIED_TOBACCO = named("dried_tobacco", new Item());
    public static final Item COFFEA_CHERRIES = named("coffea_cherries", new CropSeedItem(PSBlocks.COFFEA, "coffea_cherries"));
    public static final Item DRYING_TABLE = named("drying_table", new ItemBlock(PSBlocks.DRYING_TABLE));
    public static final Item IRON_DRYING_TABLE = named("iron_drying_table", new ItemBlock(PSBlocks.IRON_DRYING_TABLE));

    private PSItems() {
    }

    private static Item mushroom(String name, DrugType type) {
        return named(name, new EdibleItem(0, 0.1F,
                () -> new DrugInfluence(type, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5F)));
    }

    private static Item named(String name, Item item) {
        item.setRegistryName(Psychedelicraft.id(name))
                .setUnlocalizedName(Psychedelicraft.MOD_ID + "." + name)
                .setCreativeTab(GENERAL);
        ITEMS.add(item);
        return item;
    }

    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ITEMS) {
            event.getRegistry().register(item);
        }
    }
}

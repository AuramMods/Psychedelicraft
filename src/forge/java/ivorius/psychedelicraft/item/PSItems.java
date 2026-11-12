/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSItems {
    private static final CreativeTabs GENERAL = new CreativeTabs("psychedelicraft.general") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(COFFEE_BEANS);
        }
    };

    public static final Item COFFEE_BEANS = named("coffee_beans", new Item());
    public static final Item BROWN_MAGIC_MUSHROOMS = mushroom("brown_magic_mushrooms", DrugType.BROWN_SHROOMS);
    public static final Item RED_MAGIC_MUSHROOMS = mushroom("red_magic_mushrooms", DrugType.RED_SHROOMS);

    private PSItems() {
    }

    private static Item mushroom(String name, DrugType type) {
        return named(name, new EdibleItem(0, 0.1F,
                () -> new DrugInfluence(type, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5F)));
    }

    private static Item named(String name, Item item) {
        return item.setRegistryName(Psychedelicraft.id(name))
                .setUnlocalizedName(Psychedelicraft.MOD_ID + "." + name)
                .setCreativeTab(GENERAL);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(COFFEE_BEANS, BROWN_MAGIC_MUSHROOMS, RED_MAGIC_MUSHROOMS);
    }
}

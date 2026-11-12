/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
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

    public static final Item COFFEE_BEANS = new Item()
            .setRegistryName(Psychedelicraft.id("coffee_beans"))
            .setUnlocalizedName("psychedelicraft.coffee_beans")
            .setCreativeTab(GENERAL);

    private PSItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(COFFEE_BEANS);
    }
}

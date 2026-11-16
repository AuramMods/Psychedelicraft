package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PSItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Psychedelicraft.MOD_ID);
    public static final RegistryObject<Item> RED_MAGIC_MUSHROOMS = edible("red_magic_mushrooms", 0, DrugType.RED_SHROOMS, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5);
    public static final RegistryObject<Item> BROWN_MAGIC_MUSHROOMS = edible("brown_magic_mushrooms", 0, DrugType.BROWN_SHROOMS, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5);
    public static final RegistryObject<Item> LSD_PILL = edible("lsd_pill", 0, DrugType.LSD, DrugInfluence.INGESTED_DELAY, 0.05, 0.003, 0.6);
    public static final RegistryObject<Item> LSA_SQUARE = edible("lsd_square", 0, DrugType.LSD, DrugInfluence.CONTACT_DELAY, 0.05, 0.003, 0.1);
    public static final RegistryObject<Item> HASH_MUFFIN = edible("hash_muffin", 2, DrugType.CANNABIS, DrugInfluence.METABOLISED_DELAY, 0.004, 0.002, 0.7);
    public static final RegistryObject<Item> JOLLY_RANCHER = edible("jolly_rancher", 0, DrugType.SUGAR, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.05);
    public static final RegistryObject<Item> COCAINE_POWDER = powder("cocaine_powder", DrugType.COCAINE);
    public static final RegistryObject<Item> OBSIDIAN_DUST = powder("obsidian_dust", DrugType.BATH_SALTS);

    private PSItems() {}
    private static RegistryObject<Item> edible(String id, int hunger, DrugType type, int delay, double speed, double speedPlus, double dose) {
        return ITEMS.register(id, () -> new EdibleItem(hunger, 0.1F, new DrugInfluence(type, delay, speed, speedPlus, dose)));
    }
    private static RegistryObject<Item> powder(String id, DrugType type) {
        return ITEMS.register(id, () -> new PowderItem(new DrugInfluence(type, DrugInfluence.IMMEDIATE, 0.002, 0.003, 0.35)));
    }
    @SubscribeEvent public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) ITEMS.getEntries().forEach(event::accept);
    }
}

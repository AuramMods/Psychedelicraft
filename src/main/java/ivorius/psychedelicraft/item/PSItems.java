package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.food.FoodProperties;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;


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

    public static final RegistryObject<Item> CANNABIS_SEEDS = seed("cannabis_seeds", PSBlocks.CANNABIS);
    public static final RegistryObject<Item> HOP_SEEDS = seed("hop_seeds", PSBlocks.HOP);
    public static final RegistryObject<Item> TOBACCO_SEEDS = seed("tobacco_seeds", PSBlocks.TOBACCO);
    public static final RegistryObject<Item> COCA_SEEDS = seed("coca_seeds", PSBlocks.COCA);
    public static final RegistryObject<Item> COFFEA_CHERRIES = seed("coffea_cherries", PSBlocks.COFFEA);
    public static final RegistryObject<Item> AGAVE_LEAF = seed("agave_leaf", PSBlocks.AGAVE_PLANT);
    public static final RegistryObject<Item> JIMSONWEED_SEEDS = seed("jimsonweed_seeds", PSBlocks.JIMSONWEED);
    public static final RegistryObject<Item> BELLADONNA_SEEDS = seed("belladonna_seeds", PSBlocks.BELLADONNA);
    public static final RegistryObject<Item> TOMATO_SEEDS = seed("tomato_seeds", PSBlocks.TOMATOES);
    public static final RegistryObject<Item> CANNABIS_LEAF = ITEMS.register("cannabis_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CANNABIS_BUDS = ITEMS.register("cannabis_buds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HOP_CONES = ITEMS.register("hop_cones", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TOBACCO_LEAVES = ITEMS.register("tobacco", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCA_LEAVES = ITEMS.register("coca_leaves", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COFFEE_BEANS = ITEMS.register("coffee_beans", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JIMSONWEED_SEED_POD = ITEMS.register("jimsonweed_seed_pod", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JIMSONWEED_LEAF = ITEMS.register("jimsonweed_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BELLADONNA_LEAF = ITEMS.register("belladonna_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TOMATO_LEAF = ITEMS.register("tomato_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_CANNABIS_LEAF = ITEMS.register("dried_cannabis_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_CANNABIS_BUDS = ITEMS.register("dried_cannabis_buds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_TOBACCO = ITEMS.register("dried_tobacco", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_COCA_LEAVES = ITEMS.register("dried_coca_leaves", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_JIMSONWEED_LEAF = ITEMS.register("dried_jimsonweed_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_BELLADONNA_LEAF = ITEMS.register("dried_belladonna_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PEYOTE = ITEMS.register("peyote", () -> new BlockItem(PSBlocks.PEYOTE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DRIED_PEYOTE = edible("dried_peyote", 0, DrugType.PEYOTE, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5);
    public static final RegistryObject<Item> BELLADONNA_BERRIES = ITEMS.register("belladonna_berries", () -> new EdibleItem(1, 1.5F, new DrugInfluence(DrugType.ATROPINE, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5)));
    public static final RegistryObject<Item> TOMATO = ITEMS.register("tomato", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(1.9F).alwaysEat().build())));

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Psychedelicraft.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.psychedelicraft"))
            .icon(() -> new ItemStack(COFFEE_BEANS.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get()))).build());

    private PSItems() {}
    private static RegistryObject<Item> seed(String id, Supplier<? extends Block> block) {
        return ITEMS.register(id, () -> new CropSeedItem(block.get()));
    }
    private static RegistryObject<Item> edible(String id, int hunger, DrugType type, int delay, double speed, double speedPlus, double dose) {
        return ITEMS.register(id, () -> new EdibleItem(hunger, 0.1F, new DrugInfluence(type, delay, speed, speedPlus, dose)));
    }
    private static RegistryObject<Item> powder(String id, DrugType type) {
        return ITEMS.register(id, () -> new PowderItem(new DrugInfluence(type, DrugInfluence.IMMEDIATE, 0.002, 0.003, 0.35)));
    }
}

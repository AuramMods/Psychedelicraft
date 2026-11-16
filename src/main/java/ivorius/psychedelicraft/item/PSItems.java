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
import net.minecraft.world.item.DyeColor;
import ivorius.psychedelicraft.config.PSConfig;
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

    public static final RegistryObject<Item> DRYING_TABLE = ITEMS.register("drying_table",()->new BlockItem(PSBlocks.DRYING_TABLE.get(),new Item.Properties()));
    public static final RegistryObject<Item> IRON_DRYING_TABLE = ITEMS.register("iron_drying_table",()->new BlockItem(PSBlocks.IRON_DRYING_TABLE.get(),new Item.Properties()));
    public static final RegistryObject<SmokeableItem> JOINT=ITEMS.register("joint",()->new SmokeableItem(2,20,0xe6e6e6,
            new DrugInfluence(DrugType.CANNABIS,DrugInfluence.INHALED_DELAY,.002,.001,.20)).stackable());
    public static final RegistryObject<SmokeableItem> PEYOTE_JOINT=ITEMS.register("peyote_joint",()->new SmokeableItem(2,20,0x80e666,
            new DrugInfluence(DrugType.PEYOTE,DrugInfluence.INHALED_DELAY,.003,.0015,.4),
            new DrugInfluence(DrugType.TOBACCO,DrugInfluence.IMMEDIATE,.1,.02,.1)).stackable());
    public static final RegistryObject<SmokeableItem> CIGARETTE=ITEMS.register("cigarette",()->new SmokeableItem(1,50,0xffffff,
            new DrugInfluence(DrugType.TOBACCO,DrugInfluence.IMMEDIATE,.1,.02,.7)));
    public static final RegistryObject<SmokeableItem> CIGAR=ITEMS.register("cigar",()->new SmokeableItem(3,80,0x999980,4F,
            new DrugInfluence(DrugType.TOBACCO,DrugInfluence.IMMEDIATE,.1,.02,.7)));
    public static final RegistryObject<HarmoniumItem> HARMONIUM=ITEMS.register("harmonium",HarmoniumItem::new);
    public static final RegistryObject<BongItem> SMOKING_PIPE=ITEMS.register("smoking_pipe",()->smokingTool(false));
    public static final RegistryObject<BongItem> BONG=ITEMS.register("bong",()->smokingTool(true));
    public static final RegistryObject<PortableFluidItem> WOODEN_MUG=vessel("wooden_mug",ivorius.psychedelicraft.fluid.FluidVessel.MUG);
    public static final RegistryObject<PortableFluidItem> STONE_CUP=vessel("stone_cup",ivorius.psychedelicraft.fluid.FluidVessel.CUP);
    public static final RegistryObject<PortableFluidItem> GLASS_CHALICE=vessel("glass_chalice",ivorius.psychedelicraft.fluid.FluidVessel.CHALICE);
    public static final RegistryObject<PortableFluidItem> SHOT_GLASS=vessel("shot_glass",ivorius.psychedelicraft.fluid.FluidVessel.SHOT_GLASS);
    public static final RegistryObject<PortableFluidItem> BOTTLE=vessel("bottle",ivorius.psychedelicraft.fluid.FluidVessel.BOTTLE);
    public static final RegistryObject<PortableFluidItem> SYRINGE=vessel("syringe",ivorius.psychedelicraft.fluid.FluidVessel.SYRINGE);
    public static final RegistryObject<PortableFluidItem> FILLED_BOWL=vessel("filled_bowl",ivorius.psychedelicraft.fluid.FluidVessel.BOWL);
    public static final RegistryObject<PortableFluidItem> FILLED_GLASS_BOTTLE=vessel("filled_glass_bottle",ivorius.psychedelicraft.fluid.FluidVessel.GLASS_BOTTLE);
    public static final RegistryObject<PortableFluidItem> FILLED_BUCKET=vessel("filled_bucket",ivorius.psychedelicraft.fluid.FluidVessel.BUCKET);

    public static final RegistryObject<BlockItem> BOTTLE_RACK=ITEMS.register("bottle_rack",()->new BlockItem(PSBlocks.BOTTLE_RACK.get(),new Item.Properties()));
    public static final RegistryObject<BlockItem> BUNSEN_BURNER=ITEMS.register("bunsen_burner",()->new BlockItem(PSBlocks.BUNSEN_BURNER.get(),new Item.Properties()));
    public static final RegistryObject<BlockItem> TRAY=ITEMS.register("tray",()->new BlockItem(PSBlocks.TRAY.get(),new Item.Properties()));
    public static final RegistryObject<MashTubItem> MASH_TUB=ITEMS.register("mash_tub",MashTubItem::new);
    public static final RegistryObject<FluidMachineItem> FLASK=ITEMS.register("flask",()->new FluidMachineItem(PSBlocks.FLASK.get(),8000));
    public static final RegistryObject<FluidMachineItem> DISTILLERY=ITEMS.register("distillery",()->new FluidMachineItem(PSBlocks.DISTILLERY.get(),8000));
    public static final RegistryObject<FluidMachineItem> OAK_BARREL=ITEMS.register("oak_barrel",()->new FluidMachineItem(PSBlocks.OAK_BARREL.get(),16000));
    public static final RegistryObject<FluidMachineItem> BIRCH_BARREL=ITEMS.register("birch_barrel",()->new FluidMachineItem(PSBlocks.BIRCH_BARREL.get(),16000));
    public static final RegistryObject<FluidMachineItem> SPRUCE_BARREL=ITEMS.register("spruce_barrel",()->new FluidMachineItem(PSBlocks.SPRUCE_BARREL.get(),16000));
    public static final RegistryObject<FluidMachineItem> ACACIA_BARREL=ITEMS.register("acacia_barrel",()->new FluidMachineItem(PSBlocks.ACACIA_BARREL.get(),16000));
    public static final RegistryObject<FluidMachineItem> JUNGLE_BARREL=ITEMS.register("jungle_barrel",()->new FluidMachineItem(PSBlocks.JUNGLE_BARREL.get(),16000));
    public static final RegistryObject<FluidMachineItem> DARK_OAK_BARREL=ITEMS.register("dark_oak_barrel",()->new FluidMachineItem(PSBlocks.DARK_OAK_BARREL.get(),16000));

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Psychedelicraft.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.psychedelicraft"))
            .icon(() -> new ItemStack(COFFEE_BEANS.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> {
                if(item.get() instanceof PortableFluidItem vessel)VesselCreativeItems.add(vessel,output::accept);
                else if(item.get()!=HARMONIUM.get())output.accept(item.get());
                else if(PSConfig.ENABLE_HARMONIUM.get())for(DyeColor dye:DyeColor.values()){
                    ItemStack stack=new ItemStack(item.get());HarmoniumItem.setColor(stack,HarmoniumItem.packColor(dye.getTextureDiffuseColors()));output.accept(stack);
                }
            })).build());

    private PSItems() {}
    private static RegistryObject<PortableFluidItem> vessel(String id,ivorius.psychedelicraft.fluid.FluidVessel vessel){return ITEMS.register(id,()->new PortableFluidItem(vessel));}
    private static RegistryObject<Item> seed(String id, Supplier<? extends Block> block) {
        return ITEMS.register(id, () -> new CropSeedItem(block.get()));
    }
    private static RegistryObject<Item> edible(String id, int hunger, DrugType type, int delay, double speed, double speedPlus, double dose) {
        return ITEMS.register(id, () -> new EdibleItem(hunger, 0.1F, new DrugInfluence(type, delay, speed, speedPlus, dose)));
    }
    private static RegistryObject<Item> powder(String id, DrugType type) {
        return ITEMS.register(id, () -> new PowderItem(new DrugInfluence(type, DrugInfluence.IMMEDIATE, 0.002, 0.003, 0.35)));
    }
    private static BongItem smokingTool(boolean bong){
        int delay=bong?DrugInfluence.IMMEDIATE:DrugInfluence.INHALED_DELAY;
        return new BongItem(bong?128:50,bong?0:200,bong)
                .consumes(DRIED_CANNABIS_BUDS.get(),new DrugInfluence(DrugType.CANNABIS,delay,.002,.001,bong?.2:.25))
                .consumes(DRIED_TOBACCO.get(),new DrugInfluence(DrugType.TOBACCO,delay,.1,.02,bong?.6:.8))
                .consumes(DRIED_BELLADONNA_LEAF.get(),new DrugInfluence(DrugType.ATROPINE,delay,.4,.1,bong?.4:.9))
                .consumes(DRIED_JIMSONWEED_LEAF.get(),new DrugInfluence(DrugType.ATROPINE,delay,.5,.1,bong?.1:.2))
                .consumes(HARMONIUM.get(),stack->DrugInfluence.harmonium(delay,bong?.9:.65,HarmoniumItem.getColor(stack)));
    }
}

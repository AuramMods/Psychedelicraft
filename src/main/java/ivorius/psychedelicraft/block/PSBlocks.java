package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSBlocks {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Psychedelicraft.MOD_ID);
    public static final RegistryObject<CannabisPlantBlock> CANNABIS=BLOCKS.register("cannabis",()->new CannabisPlantBlock(plant()));
    public static final RegistryObject<HopPlantBlock> HOP=BLOCKS.register("hop",()->new HopPlantBlock(plant()));
    public static final RegistryObject<TobaccoPlantBlock> TOBACCO=BLOCKS.register("tobacco",()->new TobaccoPlantBlock(plant()));
    public static final RegistryObject<CocaPlantBlock> COCA=BLOCKS.register("coca",()->new CocaPlantBlock(plant()));
    public static final RegistryObject<CoffeaPlantBlock> COFFEA=BLOCKS.register("coffea",()->new CoffeaPlantBlock(plant()));
    public static final RegistryObject<PeyoteBlock> PEYOTE=BLOCKS.register("peyote",()->new PeyoteBlock(plant()));
    public static final RegistryObject<AgavePlantBlock> AGAVE_PLANT=BLOCKS.register("agave_plant",()->new AgavePlantBlock(plant()));
    public static final RegistryObject<NightshadeBlock> JIMSONWEED=BLOCKS.register("jimsonweed",()->new NightshadeBlock(plant(),()->PSItems.JIMSONWEED_SEED_POD.get(),()->PSItems.JIMSONWEED_LEAF.get()));
    public static final RegistryObject<NightshadeBlock> BELLADONNA=BLOCKS.register("belladonna",()->new NightshadeBlock(plant(),()->PSItems.BELLADONNA_BERRIES.get(),()->PSItems.BELLADONNA_LEAF.get()));
    public static final RegistryObject<NightshadeBlock> TOMATOES=BLOCKS.register("tomatoes",()->new NightshadeBlock(plant(),()->PSItems.TOMATO.get(),()->PSItems.TOMATO_LEAF.get()));
    public static final RegistryObject<DryingTableBlock> DRYING_TABLE=BLOCKS.register("drying_table",()->new DryingTableBlock(
            BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).ignitedByLava()));
    public static final RegistryObject<DryingTableBlock> IRON_DRYING_TABLE=BLOCKS.register("iron_drying_table",()->new DryingTableBlock(
            BlockBehaviour.Properties.of().strength(5).sound(SoundType.METAL)));
    private PSBlocks() {}
    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().randomTicks().sound(SoundType.GRASS);
    }
}

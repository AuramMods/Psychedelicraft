package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,Psychedelicraft.MOD_ID);
    public static final RegistryObject<BlockEntityType<DryingTableBlockEntity>> DRYING_TABLE=TYPES.register("drying_table",()->
            BlockEntityType.Builder.of(DryingTableBlockEntity::new,PSBlocks.DRYING_TABLE.get(),PSBlocks.IRON_DRYING_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<FlaskBlockEntity>> FLASK=TYPES.register("flask",()->BlockEntityType.Builder.of(FlaskBlockEntity::new,PSBlocks.FLASK.get()).build(null));
    public static final RegistryObject<BlockEntityType<DistilleryBlockEntity>> DISTILLERY=TYPES.register("distillery",()->BlockEntityType.Builder.of(DistilleryBlockEntity::new,PSBlocks.DISTILLERY.get()).build(null));
    public static final RegistryObject<BlockEntityType<BarrelBlockEntity>> BARREL=TYPES.register("barrel",()->BlockEntityType.Builder.of(BarrelBlockEntity::new,PSBlocks.OAK_BARREL.get(),PSBlocks.BIRCH_BARREL.get(),PSBlocks.SPRUCE_BARREL.get(),PSBlocks.ACACIA_BARREL.get(),PSBlocks.JUNGLE_BARREL.get(),PSBlocks.DARK_OAK_BARREL.get()).build(null));
    public static final RegistryObject<BlockEntityType<MashTubBlockEntity>> MASH_TUB=TYPES.register("mash_tub",()->BlockEntityType.Builder.of(MashTubBlockEntity::new,PSBlocks.MASH_TUB.get()).build(null));
    public static final RegistryObject<BlockEntityType<MashTubEdgeBlockEntity>> MASH_TUB_EDGE=TYPES.register("mash_tub_edge",()->BlockEntityType.Builder.of(MashTubEdgeBlockEntity::new,PSBlocks.MASH_TUB_EDGE.get()).build(null));
    public static final RegistryObject<BlockEntityType<PlacedDrinkBlockEntity>> PLACED_DRINK=TYPES.register("placed_drink",()->BlockEntityType.Builder.of(PlacedDrinkBlockEntity::new,PSBlocks.PLACED_DRINK.get()).build(null));
    public static final RegistryObject<BlockEntityType<BottleRackBlockEntity>> BOTTLE_RACK=TYPES.register("bottle_rack",()->BlockEntityType.Builder.of(BottleRackBlockEntity::new,PSBlocks.BOTTLE_RACK.get()).build(null));
    public static final RegistryObject<BlockEntityType<JuniperSignBlockEntity>> JUNIPER_SIGN=TYPES.register("juniper_sign",()->BlockEntityType.Builder.of(JuniperSignBlockEntity::new,PSBlocks.JUNIPER_SIGN.get(),PSBlocks.JUNIPER_WALL_SIGN.get()).build(null));
    public static final RegistryObject<BlockEntityType<JuniperHangingSignBlockEntity>> JUNIPER_HANGING_SIGN=TYPES.register("juniper_hanging_sign",()->BlockEntityType.Builder.of(JuniperHangingSignBlockEntity::new,PSBlocks.JUNIPER_HANGING_SIGN.get(),PSBlocks.JUNIPER_WALL_HANGING_SIGN.get()).build(null));
    private PSBlockEntities(){}
}

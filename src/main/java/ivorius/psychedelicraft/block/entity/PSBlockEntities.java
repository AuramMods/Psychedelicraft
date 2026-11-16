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
    private PSBlockEntities(){}
}

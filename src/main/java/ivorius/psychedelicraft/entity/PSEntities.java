package ivorius.psychedelicraft.entity;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.vehicle.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;
public final class PSEntities {
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,Psychedelicraft.MOD_ID);
    public static final RegistryObject<EntityType<JuniperBoat>> JUNIPER_BOAT=TYPES.register("juniper_boat",()->EntityType.Builder.<JuniperBoat>of(JuniperBoat::new,MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(Psychedelicraft.id("juniper_boat").toString()));
    public static final RegistryObject<EntityType<JuniperChestBoat>> JUNIPER_CHEST_BOAT=TYPES.register("juniper_chest_boat",()->EntityType.Builder.<JuniperChestBoat>of(JuniperChestBoat::new,MobCategory.MISC).sized(1.375F,.5625F).clientTrackingRange(10).build(Psychedelicraft.id("juniper_chest_boat").toString()));
    private PSEntities(){}
}

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PSItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Psychedelicraft.MOD_ID);
    public static final RegistryObject<Item> RED_MAGIC_MUSHROOMS = mushroom("red_magic_mushrooms", DrugType.RED_SHROOMS);
    public static final RegistryObject<Item> BROWN_MAGIC_MUSHROOMS = mushroom("brown_magic_mushrooms", DrugType.BROWN_SHROOMS);

    private PSItems() {}

    private static RegistryObject<Item> mushroom(String id, DrugType type) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().food(
                new FoodProperties.Builder().nutrition(0).saturationMod(0.1F).alwaysEat().build())) {
            @Override
            public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
                if (!level.isClientSide && consumer instanceof Player player) {
                    DrugCapability.get(player).addInfluence(new DrugInfluence(type,
                            DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5));
                }
                return super.finishUsingItem(stack, level, consumer);
            }
        });
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(RED_MAGIC_MUSHROOMS);
            event.accept(BROWN_MAGIC_MUSHROOMS);
        }
    }
}

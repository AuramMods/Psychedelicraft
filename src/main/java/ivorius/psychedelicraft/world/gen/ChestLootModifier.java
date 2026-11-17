package ivorius.psychedelicraft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Datapack-selectable supplements; vanilla loot and other modifiers remain intact. */
public final class ChestLootModifier extends LootModifier {
    public static final Codec<ChestLootModifier> CODEC=RecordCodecBuilder.create(instance->codecStart(instance).and(instance.group(
            ResourceLocation.CODEC.fieldOf("table").forGetter(value->value.table),
            Codec.BOOL.fieldOf("village").forGetter(value->value.village))).apply(instance,ChestLootModifier::new));
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> TYPES=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,Psychedelicraft.MOD_ID);
    static {TYPES.register("chest_supplies",()->CODEC);}
    private final ResourceLocation table;
    private final boolean village;
    public ChestLootModifier(LootItemCondition[] conditions,ResourceLocation table,boolean village){super(conditions);this.table=table;this.village=village;}
    public static void register(IEventBus bus){TYPES.register(bus);}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        if((village?PSConfig.VILLAGE_CHESTS:PSConfig.DUNGEON_CHESTS).get()){
            // A supplement is a raw nested table roll, not another top-level loot operation.
            // Reusing the native context preserves luck, RNG, functions and recursion protection.
            context.getResolver().getLootTable(table).getRandomItemsRaw(context,LootTable.createStackSplitter(context.getLevel(),loot::add));
        }
        return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC;}
}

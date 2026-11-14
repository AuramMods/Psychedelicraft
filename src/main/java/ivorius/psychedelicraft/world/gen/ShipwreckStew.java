package ivorius.psychedelicraft.world.gen;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.SuspiciousStewItem;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;

import java.util.Random;

final class ShipwreckStew extends LootFunction {
    private ShipwreckStew(LootCondition[] conditions) {
        super(conditions);
    }

    static void register() {
        LootFunctionManager.registerFunction(new Serializer());
    }

    @Override
    public ItemStack apply(ItemStack stack, Random random, LootContext context) {
        Potion potion;
        int min;
        int max;
        switch (random.nextInt(6)) {
            case 0: potion = MobEffects.NIGHT_VISION; min = 7; max = 10; break;
            case 1: potion = MobEffects.JUMP_BOOST; min = 7; max = 10; break;
            case 2: potion = MobEffects.WEAKNESS; min = 6; max = 8; break;
            case 3: potion = MobEffects.BLINDNESS; min = 5; max = 7; break;
            case 4: potion = MobEffects.POISON; min = 10; max = 20; break;
            default: potion = MobEffects.SATURATION; min = 7; max = 10; break;
        }
        int duration = min + random.nextInt(max - min + 1);
        return SuspiciousStewItem.withEffect(potion, potion.isInstant() ? duration : duration * 20);
    }

    private static final class Serializer extends LootFunction.Serializer<ShipwreckStew> {
        Serializer() {
            super(Psychedelicraft.id("shipwreck_stew"), ShipwreckStew.class);
        }

        @Override
        public void serialize(JsonObject json, ShipwreckStew function, JsonSerializationContext context) {
        }

        @Override
        public ShipwreckStew deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            return new ShipwreckStew(conditions);
        }
    }
}

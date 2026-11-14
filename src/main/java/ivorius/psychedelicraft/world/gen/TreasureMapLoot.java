package ivorius.psychedelicraft.world.gen;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.util.EnumHelper;

import java.util.Random;

public final class TreasureMapLoot extends LootFunction {
    public static final String TITLE = "item.psychedelicraft.buried_treasure_map.name";
    private static final ThreadLocal<BlockPos> CONTAINER_POSITION = new ThreadLocal<>();
    private static MapDecoration.Type redX;

    private TreasureMapLoot(LootCondition[] conditions) {
        super(conditions);
    }

    static void register() {
        redX = EnumHelper.addEnum(MapDecoration.Type.class, "PSYCHEDELICRAFT_RED_X",
                new Class<?>[]{boolean.class}, true);
        LootFunctionManager.registerFunction(new Serializer());
    }

    public static MapDecoration.Type redX() {
        return redX;
    }

    // The legacy loot context cannot carry the origin of a container roll.
    public static void fillFromContainer(LootTable table, IInventory inventory, Random random, LootContext context) {
        BlockPos previous = CONTAINER_POSITION.get();
        CONTAINER_POSITION.set(inventory instanceof TileEntity ? ((TileEntity) inventory).getPos() : null);
        try {
            table.fillInventory(inventory, random, context);
        } finally {
            if (previous == null) CONTAINER_POSITION.remove();
            else CONTAINER_POSITION.set(previous);
        }
    }

    @Override
    public ItemStack apply(ItemStack stack, Random random, LootContext context) {
        if (stack.getItem() != Items.MAP) return stack;
        WorldServer world = context.getWorld();
        BlockPos origin = CONTAINER_POSITION.get();
        BlockPos target = origin == null ? null : BuriedTreasure.find(world, origin);
        if (target != null) {
            stack = ItemMap.setupNewMap(world, target.getX(), target.getZ(), (byte) 1, true, true);
            ItemMap.renderBiomePreviewMap(world, stack);
            MapData.addTargetDecoration(stack, target, "+", redX);
        }
        return stack.setTranslatableName(TITLE);
    }

    private static final class Serializer extends LootFunction.Serializer<TreasureMapLoot> {
        Serializer() {
            super(Psychedelicraft.id("buried_treasure_map"), TreasureMapLoot.class);
        }

        @Override
        public void serialize(JsonObject json, TreasureMapLoot function, JsonSerializationContext context) {
        }

        @Override
        public TreasureMapLoot deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            return new TreasureMapLoot(conditions);
        }
    }
}

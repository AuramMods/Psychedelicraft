/* Copyright (c) 2014, Lukas Tenbrink. Native display design by Sollace. */
package ivorius.psychedelicraft.block.entity;
import ivorius.psychedelicraft.block.PlacedDrinksBlock;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;
public final class PlacedDrinkBlockEntity extends DrinkDisplayBlockEntity {
    public static final int GRID_SIZE = 16;
    public static final int MAX_HEIGHT = 5;
    private static final int PICKUP_RADIUS = 2;
    private static final Direction[] PICKUP_ORDER = {Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH};

    private final Map<Integer, List<Drink>> columns = new LinkedHashMap<>();

    public PlacedDrinkBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.PLACED_DRINK.get(),pos,state);}

    public static int cell(float hitX, float hitZ) {
        int x = Mth.clamp(Mth.floor(hitX * GRID_SIZE), 0, GRID_SIZE - 1);
        int z = Mth.clamp(Mth.floor(hitZ * GRID_SIZE), 0, GRID_SIZE - 1);
        return x * GRID_SIZE + z;
    }

    public static float x(int cell) {
        return (cell / GRID_SIZE) / (float) GRID_SIZE;
    }

    public static float z(int cell) {
        return (cell % GRID_SIZE) / (float) GRID_SIZE;
    }

    public boolean canPlace(int cell) {
        List<Drink> drinks = columns.get(cell);
        return drinks == null || drinks.size() < MAX_HEIGHT;
    }

    public boolean place(int cell, ItemStack held, float yaw) {
        if (!PlacedDrinksBlock.canPlace(held) || !canPlace(cell)) return false;
        ItemStack placed = held.copy();
        placed.setCount(1);
        columns.computeIfAbsent(cell, key -> new ArrayList<>(MAX_HEIGHT)).add(new Drink(placed, -yaw % 360));
        contentsChanged();
        return true;
    }

    public int findColumn(int cell) {
        int x = cell / GRID_SIZE;
        int z = cell % GRID_SIZE;
        int width = 2 * PICKUP_RADIUS + 1;
        int stepsRemaining = width * width;
        for (int leg = 0; stepsRemaining > 0; leg++) {
            Direction direction = PICKUP_ORDER[leg % PICKUP_ORDER.length];
            int length = leg / 2 + 1;
            for (int step = 0; step < length && stepsRemaining > 0; step++, stepsRemaining--) {
                if (x >= 0 && x < GRID_SIZE && z >= 0 && z < GRID_SIZE) {
                    int candidate = x * GRID_SIZE + z;
                    if (columns.containsKey(candidate)) return candidate;
                }
                x += direction.getStepX();
                z += direction.getStepZ();
            }
        }
        return -1;
    }

    public ItemStack peek(int cell) {
        List<Drink> drinks = columns.get(findColumn(cell));
        return drinks == null ? ItemStack.EMPTY : drinks.get(drinks.size() - 1).stack.copy();
    }

    public ItemStack take(int cell) {
        int column = findColumn(cell);
        List<Drink> drinks = columns.get(column);
        if (drinks == null) return ItemStack.EMPTY;
        ItemStack stack = drinks.remove(drinks.size() - 1).stack;
        if (drinks.isEmpty()) columns.remove(column);
        if (columns.isEmpty() && level != null) level.removeBlock(worldPosition,false);
        else contentsChanged();
        return stack;
    }

    public boolean isEmpty(){return columns.isEmpty();}

    public List<ItemStack> takeAll(){List<ItemStack> drops=new ArrayList<>();addDrops(drops);columns.clear();contentsChanged();return drops;}

    public void addDrops(List<ItemStack> drops) {
        for (List<Drink> drinks : columns.values()) {
            for (Drink drink : drinks) drops.add(drink.stack.copy());
        }
    }

    public void forEachDrink(DrinkConsumer consumer) {
        columns.forEach((cell, drinks) -> {
            float height = 0;
            for (Drink drink : drinks) {
                height += consumer.render(x(cell), height, z(cell), drink.rotation, drink.stack);
            }
        });
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(.2,0,.2).expandTowards(0,1.5,0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag entries = new ListTag();
        columns.forEach((cell, drinks) -> {
            for (Drink drink : drinks) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("cell", cell);
                entry.putFloat("rotation", drink.rotation);
                entry.put("stack", drink.stack.save(new CompoundTag()));
                entries.add(entry);
            }
        });
        tag.put("entries", entries);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        columns.clear();
        ListTag entries = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int cell = entry.getInt("cell");
            ItemStack stack = ItemStack.of(entry.getCompound("stack"));
            float rotation = entry.getFloat("rotation");
            if (cell >= 0 && cell < GRID_SIZE * GRID_SIZE && !stack.isEmpty() && canPlace(cell)) {
                stack.setCount(1);
                columns.computeIfAbsent(cell, key -> new ArrayList<>(MAX_HEIGHT))
                        .add(new Drink(stack, Float.isFinite(rotation) ? rotation : 0));
            }
        }
    }

    public interface DrinkConsumer {
        float render(float x, float y, float z, float rotation, ItemStack stack);
    }

    private static final class Drink {
        private final ItemStack stack;
        private final float rotation;

        private Drink(ItemStack stack, float rotation) {
            this.stack = stack;
            this.rotation = rotation;
        }
    }
}

/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.PlacedDrinksBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlacedDrinkBlockEntity extends DrinkDisplayBlockEntity {
    private static final int GRID_SIZE = 16;
    private static final int MAX_HEIGHT = 5;
    private static final int PICKUP_RADIUS = 2;

    private final Map<Integer, List<Drink>> columns = new LinkedHashMap<>();

    public static int cell(float hitX, float hitZ) {
        int x = MathHelper.clamp(MathHelper.floor(hitX * GRID_SIZE), 0, GRID_SIZE - 1);
        int z = MathHelper.clamp(MathHelper.floor(hitZ * GRID_SIZE), 0, GRID_SIZE - 1);
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
        int result = -1;
        int closest = 2 * PICKUP_RADIUS * PICKUP_RADIUS + 1;
        for (int dx = -PICKUP_RADIUS; dx <= PICKUP_RADIUS; dx++) {
            for (int dz = -PICKUP_RADIUS; dz <= PICKUP_RADIUS; dz++) {
                int candidateX = x + dx;
                int candidateZ = z + dz;
                int distance = dx * dx + dz * dz;
                if (distance < closest && candidateX >= 0 && candidateX < GRID_SIZE
                        && candidateZ >= 0 && candidateZ < GRID_SIZE) {
                    int candidate = candidateX * GRID_SIZE + candidateZ;
                    if (columns.containsKey(candidate)) {
                        closest = distance;
                        result = candidate;
                    }
                }
            }
        }
        return result;
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
        if (columns.isEmpty() && world != null) world.setBlockToAir(pos);
        else contentsChanged();
        return stack;
    }

    public void dropContents() {
        for (List<Drink> drinks : columns.values()) {
            for (Drink drink : drinks) Block.spawnAsEntity(world, pos, drink.stack);
        }
        columns.clear();
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
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos).expand(0.2, 1.5, 0.2).expand(-0.2, 0, -0.2);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList entries = new NBTTagList();
        columns.forEach((cell, drinks) -> {
            for (Drink drink : drinks) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setInteger("cell", cell);
                entry.setFloat("rotation", drink.rotation);
                entry.setTag("stack", drink.stack.writeToNBT(new NBTTagCompound()));
                entries.appendTag(entry);
            }
        });
        tag.setTag("entries", entries);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        columns.clear();
        NBTTagList entries = tag.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            int cell = entry.getInteger("cell");
            ItemStack stack = new ItemStack(entry.getCompoundTag("stack"));
            float rotation = entry.getFloat("rotation");
            if (cell >= 0 && cell < GRID_SIZE * GRID_SIZE && PlacedDrinksBlock.canPlace(stack) && canPlace(cell)) {
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

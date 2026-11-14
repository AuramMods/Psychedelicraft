package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.CampfireBlock;
import ivorius.psychedelicraft.client.particle.CampfireSmoke;
import ivorius.psychedelicraft.recipe.CampfireRecipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;

public final class CampfireBlockEntity extends WaterloggedBlockEntity implements ITickable {
    public static final int SLOT_COUNT = 4;
    private final NonNullList<ItemStack> food = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] progress = new int[SLOT_COUNT];

    public ItemStack getFood(int slot) {
        return food.get(slot);
    }

    public boolean placeFood(ItemStack held, boolean creative) {
        if (CampfireRecipe.cook(held).isEmpty()) return false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!food.get(slot).isEmpty()) continue;
            if (!world.isRemote) {
                food.set(slot, (creative ? held.copy() : held).splitStack(1));
                progress[slot] = 0;
                inventoryChanged(world.getBlockState(pos));
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onWaterloggedChanged(boolean wet) {
        IBlockState state = world.getBlockState(pos);
        if (wet && state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            CampfireBlock.dowse(world, pos, state);
        }
    }

    @Override
    public void update() {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CampfireBlock)) return;
        boolean lit = state.getValue(CampfireBlock.LIT) && !isWaterlogged();
        if (world.isRemote) {
            if (lit) {
                if (world.rand.nextFloat() < 0.11F) {
                    CampfireSmoke.spawn(world, pos, CampfireBlock.isSignalFire(world, pos), 2 + world.rand.nextInt(2), false);
                }
                smokeFood(state.getValue(CampfireBlock.FACING));
            }
            return;
        }
        boolean advanced = false;
        boolean cooked = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = food.get(slot);
            if (stack.isEmpty()) continue;
            if (lit) {
                advanced = true;
                if (++progress[slot] >= CampfireRecipe.COOK_TICKS) {
                    ItemStack output = CampfireRecipe.cook(stack);
                    food.set(slot, ItemStack.EMPTY);
                    progress[slot] = 0;
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(),
                            output.isEmpty() ? stack : output);
                    cooked = true;
                }
            } else if (progress[slot] > 0) {
                advanced = true;
                progress[slot] = Math.max(0, progress[slot] - 2);
            }
        }
        if (cooked) inventoryChanged(state);
        else if (advanced) world.markChunkDirty(pos, this);
    }

    private void smokeFood(EnumFacing facing) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (food.get(slot).isEmpty() || world.rand.nextFloat() >= 0.2F) continue;
            EnumFacing direction = EnumFacing.getHorizontal(facing.getHorizontalIndex() + slot);
            EnumFacing clockwise = direction.rotateY();
            double x = pos.getX() + 0.5 + (clockwise.getFrontOffsetX() - direction.getFrontOffsetX()) * 0.3125;
            double z = pos.getZ() + 0.5 + (clockwise.getFrontOffsetZ() - direction.getFrontOffsetZ()) * 0.3125;
            for (int puff = 0; puff < 4; puff++) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, pos.getY() + 0.5, z, 0, 0.0005, 0);
            }
        }
    }

    public void dropFood() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = food.set(slot, ItemStack.EMPTY);
            progress[slot] = 0;
            if (!stack.isEmpty()) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    private void inventoryChanged(IBlockState state) {
        world.markChunkDirty(pos, this);
        world.notifyBlockUpdate(pos, state, state, 2);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readFood(tag);
        int[] saved = tag.getIntArray("CookingTimes");
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            progress[slot] = food.get(slot).isEmpty() || slot >= saved.length ? 0
                    : MathHelper.clamp(saved[slot], 0, CampfireRecipe.COOK_TICKS);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setIntArray("CookingTimes", progress);
        return ItemStackHelper.saveAllItems(tag, food);
    }

    private void readFood(NBTTagCompound tag) {
        food.clear();
        ItemStackHelper.loadAllItems(tag, food);
        for (ItemStack stack : food) {
            if (stack.getCount() > 1) stack.setCount(1);
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.removeTag("CookingTimes");
        return tag;
    }
}

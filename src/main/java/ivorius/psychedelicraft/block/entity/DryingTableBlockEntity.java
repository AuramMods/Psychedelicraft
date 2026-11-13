/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import javax.annotation.Nullable;
import java.util.UUID;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.recipe.DryingRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import static ivorius.psychedelicraft.recipe.DryingRecipe.INPUT_COUNT;
import static ivorius.psychedelicraft.recipe.DryingRecipe.OUTPUT;

public final class DryingTableBlockEntity extends TileEntity implements ITickable, ISidedInventory {
    private static final int[] INPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] OUTPUT_SLOTS = {OUTPUT};
    private static final int[] ALL_SLOTS = {OUTPUT, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private final NonNullList<ItemStack> items = NonNullList.withSize(INPUT_COUNT + 1, ItemStack.EMPTY);
    private final IItemHandler[] sidedHandlers = new IItemHandler[EnumFacing.VALUES.length];
    private final IItemHandler unsidedHandler = new SidedInvWrapper(this, null);
    private DryingRecipe recipe;
    private int ticks;
    private float heat;
    private float progress;
    private UUID dealer;

    public DryingTableBlockEntity() {
        for (EnumFacing facing : EnumFacing.VALUES) {
            sidedHandlers[facing.getIndex()] = new SidedInvWrapper(this, facing);
        }
    }

    @Override
    public void onLoad() {
        if (world != null && !world.isRemote) {
            recipe = DryingRecipe.match(this);
        }
    }

    @Override
    public void update() {
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        Block block = getBlockType();
        float oldHeat = heat;
        float oldProgress = progress;
        if (++ticks % 30 == 0) {
            heat = heatAt(server.getLight(pos, false), server.getBiome(pos).getDefaultTemperature());
            if (server.getRainStrength(1F) > 0 && server.getPrecipitationHeight(pos).getY() == pos.getY() + 1) {
                progress = 0;
            }
        }
        if (recipe != null) {
            int duration = block == PSBlocks.IRON_DRYING_TABLE
                    ? PSConfig.balancing.ironDryingTableTickDuration : PSConfig.balancing.dryingTableTickDuration;
            progress += heat / Math.max(1, duration);
            int interval = (int) ((1 - MathHelper.clamp(progress, 0, 1)) * 100);
            if (interval == 0 || server.getTotalWorldTime() % interval == 0) {
                smoke(server);
            }
            if (progress >= 1) {
                ItemStack result = recipe.getOutput();
                items.clear();
                items.set(OUTPUT, result);
                inventoryChanged();
            }
        }
        if (heat != oldHeat || progress != oldProgress) {
            server.markChunkDirty(pos, this);
            if ((int) (oldHeat * 15) != (int) (heat * 15)) {
                server.updateComparatorOutputLevel(pos, block);
            }
            if ((int) (oldProgress * 15) != (int) (progress * 15)) {
                server.notifyNeighborsOfStateChange(pos, block, false);
            }
        }
    }

    static float heatAt(int light, float biomeTemperature) {
        float brightness = light / 15F;
        float temperature = biomeTemperature * 0.75F + 0.25F;
        float warmth = brightness * brightness * temperature;
        return MathHelper.clamp(warmth * warmth, 0, 1);
    }

    private void smoke(WorldServer server) {
        for (int i = 0; i < 5; i++) {
            double x = pos.getX() + (server.rand.nextDouble() + server.rand.nextDouble()) * 0.5;
            double z = pos.getZ() + (server.rand.nextDouble() + server.rand.nextDouble()) * 0.5;
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, pos.getY() + 0.6, z, 2, 0D, 0D, 0D, 0D);
        }
    }

    private void inventoryChanged() {
        progress = 0;
        markDirty();
        // Structure preparation and NBT loading may populate the inventory before it has a world.
        if (world != null && !world.isRemote) {
            recipe = DryingRecipe.match(this);
            IBlockState state = world.getBlockState(pos);
            world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    public float getHeatRatio() {
        return heat;
    }

    public float getDryingProgress() {
        return progress;
    }

    public boolean availableTo(UUID villager) {
        return dealer == null || dealer.equals(villager);
    }

    public boolean claim(UUID villager) {
        if (!availableTo(villager)) return false;
        if (dealer == null) {
            dealer = villager;
            markDirty();
        }
        return true;
    }

    public void release(UUID villager) {
        if (villager.equals(dealer)) {
            dealer = null;
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        ItemStackHelper.saveAllItems(tag, items);
        tag.setFloat("heatRatio", heat);
        tag.setFloat("dryingProgress", progress);
        if (dealer != null) tag.setUniqueId("Dealer", dealer);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        items.clear();
        ItemStackHelper.loadAllItems(tag, items);
        heat = MathHelper.clamp(tag.getFloat("heatRatio"), 0, 1);
        progress = MathHelper.clamp(tag.getFloat("dryingProgress"), 0, 1);
        dealer = tag.hasUniqueId("Dealer") ? tag.getUniqueId("Dealer") : null;
        if (world != null && !world.isRemote) {
            recipe = DryingRecipe.match(this);
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }

    @Override
    public int getSizeInventory() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack removed = ItemStackHelper.getAndSplit(items, slot, count);
        if (!removed.isEmpty()) {
            inventoryChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack removed = ItemStackHelper.getAndRemove(items, slot);
        if (!removed.isEmpty()) {
            inventoryChanged();
        }
        return removed;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot != OUTPUT && stack.getCount() > getInventoryStackLimit()) {
            stack = stack.copy();
            stack.setCount(getInventoryStackLimit());
        }
        if (!ItemStack.areItemStacksEqual(items.get(slot), stack)) {
            items.set(slot, stack);
            inventoryChanged();
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSqToCenter(pos) <= 64;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot > OUTPUT && slot < items.size();
    }

    @Override
    public int[] getSlotsForFace(@Nullable EnumFacing facing) {
        return facing == null ? ALL_SLOTS : facing == EnumFacing.UP ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, @Nullable EnumFacing facing) {
        // The 1.12 hopper's merge path uses the item's stack limit instead of the inventory limit.
        return facing != EnumFacing.UP && isItemValidForSlot(slot, stack) && items.get(slot).isEmpty();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, EnumFacing facing) {
        return true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(
                    facing == null ? unsidedHandler : sidedHandlers[facing.getIndex()]);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void clear() {
        items.clear();
        inventoryChanged();
    }

    @Override
    public String getName() {
        return getBlockType().getUnlocalizedName() + ".name";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public int getField(int field) {
        return (int) ((field == 0 ? heat : progress) * 1000);
    }

    @Override
    public void setField(int field, int value) {
        if (field == 0) {
            heat = value / 1000F;
        } else if (field == 1) {
            progress = value / 1000F;
        }
    }

    @Override
    public int getFieldCount() {
        return 2;
    }
}

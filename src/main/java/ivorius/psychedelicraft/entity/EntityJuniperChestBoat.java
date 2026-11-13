package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.ILootContainer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.Random;

public final class EntityJuniperChestBoat extends EntityJuniperBoat implements IInventory, ILootContainer {
    private final NonNullList<ItemStack> cargo = NonNullList.withSize(27, ItemStack.EMPTY);
    private final IItemHandler itemHandler = new InvWrapper(this);
    private ResourceLocation lootTable;
    private long lootSeed;
    private boolean dropCargo = true;
    private boolean changingDimension;

    public EntityJuniperChestBoat(World world) {
        super(world);
    }

    @Override
    public Item getItemBoat() {
        return PSItems.JUNIPER_CHEST_BOAT;
    }

    @Override
    public ITextComponent getDisplayName() {
        return hasCustomName() ? super.getDisplayName()
                : new TextComponentTranslation("entity.psychedelicraft.juniper_chest_boat.name");
    }

    @Override
    protected int passengerLimit() {
        return 1;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        super.updatePassenger(passenger);
        if (isPassenger(passenger)) {
            Vec3d seat = new Vec3d(0.15D, 0, 0).rotateYaw((float) Math.toRadians(-rotationYaw - 90));
            passenger.setPosition(posX + seat.x, passenger.posY, posZ + seat.z);
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (canFitPassenger(player) && !player.isSneaking()) {
            return super.processInitialInteract(player, hand);
        }
        openCargo(player);
        return true;
    }

    public void openCargo(EntityPlayer player) {
        if (!world.isRemote && isUsableByPlayer(player)) {
            player.displayGUIChest(this);
        }
    }

    @Override
    public void setDead() {
        boolean drop = !isDead && !world.isRemote && dropCargo && !changingDimension
                && world.getGameRules().getBoolean("doEntityDrops");
        super.setDead();
        if (drop) {
            InventoryHelper.dropInventoryItems(world, this, this);
        }
    }

    @Override
    public void setDropItemsWhenDead(boolean dropWhenDead) {
        dropCargo = dropWhenDead;
    }

    @Override
    @Nullable
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        changingDimension = true;
        try {
            return super.changeDimension(dimension, teleporter);
        } finally {
            changingDimension = false;
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (lootTable != null) {
            tag.setString("LootTable", lootTable.toString());
            tag.setLong("LootTableSeed", lootSeed);
        } else {
            ItemStackHelper.saveAllItems(tag, cargo);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        cargo.clear();
        lootTable = tag.hasKey("LootTable", 8) ? new ResourceLocation(tag.getString("LootTable")) : null;
        lootSeed = tag.getLong("LootTableSeed");
        if (lootTable == null) {
            ItemStackHelper.loadAllItems(tag, cargo);
        }
    }

    @Override
    @Nullable
    public ResourceLocation getLootTable() {
        return lootTable;
    }

    private void unpackLoot(@Nullable EntityPlayer player) {
        if (lootTable == null || !(world instanceof WorldServer)) {
            return;
        }
        LootTable table = world.getLootTableManager().getLootTableFromLocation(lootTable);
        lootTable = null;
        LootContext.Builder context = new LootContext.Builder((WorldServer) world).withLootedEntity(this);
        if (player != null) {
            context.withPlayer(player).withLuck(player.getLuck());
        }
        table.fillInventory(this, lootSeed == 0 ? new Random() : new Random(lootSeed), context.build());
    }

    @Override
    public int getSizeInventory() {
        return cargo.size();
    }

    @Override
    public boolean isEmpty() {
        return cargo.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        unpackLoot(null);
        return cargo.get(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        unpackLoot(null);
        return ItemStackHelper.getAndSplit(cargo, slot, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        unpackLoot(null);
        return ItemStackHelper.getAndRemove(cargo, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        unpackLoot(null);
        cargo.set(slot, stack);
        if (stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
    }

    @Override
    public boolean replaceItemInInventory(int slot, ItemStack stack) {
        if (slot < 0 || slot >= cargo.size()) return false;
        setInventorySlotContents(slot, stack);
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return isEntityAlive() && player.world == world && player.getDistanceSq(this) <= 64;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        unpackLoot(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return isEntityAlive();
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        unpackLoot(null);
        cargo.clear();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return (isEntityAlive() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                || super.hasCapability(capability, side);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (isEntityAlive() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        return super.getCapability(capability, side);
    }
}

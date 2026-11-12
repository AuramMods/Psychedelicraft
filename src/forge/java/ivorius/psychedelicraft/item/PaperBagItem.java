/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockFlower;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;

public final class PaperBagItem extends Item {
    private static final int MAX_CONTENTS = 64_000;

    public PaperBagItem() {
        addPropertyOverride(Psychedelicraft.id("filled"), (stack, world, entity) -> {
            int count = getContents(stack).count;
            return count > 16_000 ? 1 : count > 0 ? 0.5F : 0;
        });
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return getContents(stack).isEmpty() ? super.getItemStackLimit(stack) : 1;
    }

    public static void registerDispenserBehavior() {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(PSItems.PAPER_BAG, new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack bag) {
                Contents contents = getContents(bag);
                if (bag.getCount() != 1 || contents.isEmpty()) return super.dispenseStack(source, bag);
                ItemStack output = contents.take(1);
                setContents(bag, contents);
                doDispense(source.getWorld(), output, 6, source.getBlockState().getValue(BlockDispenser.FACING),
                        BlockDispenser.getDispensePosition(source));
                return bag;
            }
        });
    }

    @Nullable
    public static ItemStack handleClick(Container container, int index, int button, ClickType type, EntityPlayer player) {
        if (button != 1 || type != ClickType.PICKUP || index < 0 || index >= container.inventorySlots.size()) return null;
        Slot slot = container.inventorySlots.get(index);
        if (slot == null) return null;
        ItemStack stored = slot.getStack();
        ItemStack cursor = player.inventory.getItemStack();
        if (cursor.getItem() == PSItems.PAPER_BAG && cursor.getCount() == 1) {
            Contents contents = getContents(cursor);
            if (stored.isEmpty() && !contents.isEmpty() && slot.isItemValid(contents.stack)) {
                int limit = Math.min(slot.getItemStackLimit(contents.stack), contents.stack.getMaxStackSize());
                if (limit <= 0) return null;
                slot.putStack(contents.take(limit));
                setContents(cursor, contents);
                playRemove(player);
                return ItemStack.EMPTY;
            }
            if (!slot.canTakeStack(player) || contents.roomFor(stored) <= 0) return null;
            ItemStack previous = stored.copy();
            ItemStack taken = slot.decrStackSize(Math.min(stored.getCount(), contents.roomFor(stored)));
            if (taken.isEmpty()) return null;
            contents.insert(taken.copy());
            slot.onTake(player, taken);
            setContents(cursor, contents);
            playInsert(player);
            return previous;
        }
        if (stored.getItem() == PSItems.PAPER_BAG && stored.getCount() == 1
                && slot.canTakeStack(player) && slot.isItemValid(stored)) {
            Contents contents = getContents(stored);
            ItemStack previous = stored.copy();
            if (cursor.isEmpty() && !contents.isEmpty()) {
                player.inventory.setItemStack(contents.take(contents.stack.getMaxStackSize()));
                playRemove(player);
            } else if (contents.insert(cursor)) {
                if (cursor.isEmpty()) player.inventory.setItemStack(ItemStack.EMPTY);
                playInsert(player);
            } else {
                return null;
            }
            setContents(stored, contents);
            slot.onSlotChanged();
            return previous;
        }
        return null;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing face, float hitX, float hitY, float hitZ) {
        ItemStack bag = player.getHeldItem(hand);
        Contents contents = getContents(bag);
        if (!world.isBlockModifiable(player, pos)) return EnumActionResult.PASS;
        double x = pos.getX() + hitX;
        double y = pos.getY() + hitY;
        double z = pos.getZ() + hitZ;
        AxisAlignedBB pickup = new AxisAlignedBB(x, y, z, x, y, z).grow(0.25);
        for (EntityItem drop : world.getEntitiesWithinAABB(EntityItem.class, pickup)) {
            if (contents.roomFor(drop.getItem()) <= 0) continue;
            if (!world.isRemote) {
                ItemStack remaining = drop.getItem().copy();
                contents.insert(remaining);
                if (remaining.isEmpty()) drop.setDead();
                else drop.setItem(remaining);
                if (bag.getCount() == 1) {
                    setContents(bag, contents);
                } else {
                    ItemStack filled = bag.splitStack(1);
                    setContents(filled, contents);
                    ItemHandlerHelper.giveItemToPlayer(player, filled);
                }
                playInsert(player);
            }
            return EnumActionResult.SUCCESS;
        }
        return empty(player, world, bag, contents);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack bag = player.getHeldItem(hand);
        return new ActionResult<>(empty(player, world, bag, getContents(bag)), bag);
    }

    private EnumActionResult empty(EntityPlayer player, World world, ItemStack bag, Contents contents) {
        if (contents.isEmpty() || bag.getCount() != 1) return EnumActionResult.PASS;
        if (!world.isRemote) {
            player.dropItem(contents.take(player.isSneaking() ? 1 : contents.stack.getMaxStackSize()), false, false);
            setContents(bag, contents);
            playRemove(player);
            player.addStat(StatList.getObjectUseStats(this));
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void onUpdate(ItemStack bag, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !selected || bag.getCount() != 1 || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (slot < 0 || slot >= player.inventory.mainInventory.size() || player.inventory.getStackInSlot(slot) != bag) return;
        Contents contents = getContents(bag);
        if (contents.isEmpty()) return;
        boolean changed = slot + 1 < player.inventory.mainInventory.size() && contents.insert(player.inventory.getStackInSlot(slot + 1));
        changed |= slot > 0 && contents.insert(player.inventory.getStackInSlot(slot - 1));
        if (changed) {
            setContents(bag, contents);
            player.inventory.markDirty();
            playInsert(player);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack bag) {
        Contents contents = getContents(bag);
        return contents.isEmpty() ? super.getItemStackDisplayName(bag)
                : I18n.translateToLocalFormatted(getUnlocalizedName(bag) + ".filled", contents.stack.getDisplayName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack bag, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        Contents contents = getContents(bag);
        if (!contents.isEmpty()) tooltip.add(contents.count + " × " + contents.stack.getDisplayName());
    }

    private static void playInsert(EntityPlayer player) {
        if (!player.world.isRemote) player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, player.getSoundCategory(), 0.55F, 1.25F);
    }

    private static void playRemove(EntityPlayer player) {
        if (!player.world.isRemote) player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, player.getSoundCategory(), 0.55F, 0.85F);
    }

    private static boolean canStore(ItemStack stack) {
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        return !stack.isEmpty() && (block instanceof BlockFlower || block instanceof BlockButton
                || block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM
                || item == Items.COOKIE || item == Items.NETHER_WART || item == Items.WHEAT_SEEDS
                || item == Items.BEETROOT_SEEDS || item == Items.CARROT || item == Items.POTATO
                || item == Items.SPIDER_EYE || item == Items.FISH || item == Items.COOKED_FISH
                || item == Items.GOLDEN_CARROT || item == Items.MELON || item == Items.POISONOUS_POTATO
                || item == Items.BAKED_POTATO || item == Items.BEETROOT || item == Items.CHORUS_FRUIT
                || item == Items.ROTTEN_FLESH || item == Items.APPLE || item == Items.GOLDEN_APPLE
                || item == Items.DYE || item == PSItems.IRON_CHAIN || item == PSItems.BOTTLE
                || item == PSItems.BROWN_MAGIC_MUSHROOMS || item == PSItems.RED_MAGIC_MUSHROOMS
                || item == PSItems.TOMATO || item == PSItems.BELLADONNA_BERRIES || item == PSItems.AGAVE_LEAF
                || item == PSItems.SYRINGE || item == PSItems.MORNING_GLORY || item == PSItems.HASH_MUFFIN
                || item == PSItems.COCAINE_POWDER || item == PSItems.OBSIDIAN_DUST || item == PSItems.JOINT
                || item == PSItems.CIGAR || item == PSItems.CIGARETTE || item == PSItems.SMOKING_PIPE);
    }

    private static int capacity(ItemStack stack) {
        return stack.getItem() == PSItems.BOTTLE ? 1 : MAX_CONTENTS;
    }

    public static Contents getContents(ItemStack bag) {
        NBTTagCompound tag = bag.getSubCompound("contents");
        if (tag == null) return new Contents(ItemStack.EMPTY, 0);
        ItemStack prototype = new ItemStack(tag.getCompoundTag("stack"));
        if (prototype.isEmpty()) return new Contents(ItemStack.EMPTY, 0);
        prototype.setCount(1);
        return new Contents(prototype, Math.min(capacity(prototype), Math.max(0, tag.getInteger("count"))));
    }

    static void setContents(ItemStack bag, Contents contents) {
        if (contents.isEmpty()) {
            if (bag.hasTagCompound()) {
                bag.getTagCompound().removeTag("contents");
                if (bag.getTagCompound().hasNoTags()) bag.setTagCompound(null);
            }
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("stack", contents.stack.writeToNBT(new NBTTagCompound()));
        tag.setInteger("count", contents.count);
        bag.setTagInfo("contents", tag);
    }

    public static final class Contents {
        private ItemStack stack;
        private int count;

        Contents(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }

        public boolean isEmpty() {
            return count == 0 || stack.isEmpty();
        }

        public int getCount() {
            return count;
        }

        int roomFor(ItemStack incoming) {
            return canStore(incoming) && (isEmpty() || ItemHandlerHelper.canItemStacksStack(stack, incoming))
                    ? capacity(incoming) - count : 0;
        }

        boolean insert(ItemStack incoming) {
            int amount = Math.min(incoming.getCount(), roomFor(incoming));
            if (amount <= 0) return false;
            if (isEmpty()) {
                stack = incoming.copy();
                stack.setCount(1);
            }
            incoming.shrink(amount);
            count += amount;
            return true;
        }

        ItemStack take(int amount) {
            int taken = Math.min(amount, count);
            if (taken <= 0) return ItemStack.EMPTY;
            ItemStack result = stack.copy();
            result.setCount(taken);
            count -= taken;
            return result;
        }
    }
}

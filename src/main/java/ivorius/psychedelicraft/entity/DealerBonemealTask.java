package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.TallCropBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

final class DealerBonemealTask extends EntityAIBase {
    private static final int SEARCH_INTERVAL = 10;
    private static final int SESSION_LENGTH = 60;
    private static final int SESSION_COOLDOWN = 160;
    private static final int APPLICATION_INTERVAL = 40;
    private static final int BONE_MEAL = EnumDyeColor.WHITE.getDyeDamage();

    private final EntityVillager villager;
    private final VillagerTradePreview display;
    @Nullable
    private BlockPos crop;
    private int nextSearch;
    private long endTime;
    private long nextApplication;
    private boolean working;

    DealerBonemealTask(EntityVillager villager, VillagerTradePreview display) {
        this.villager = villager;
        this.display = display;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (villager.ticksExisted < nextSearch || !canWork()) return false;
        nextSearch = villager.ticksExisted + SEARCH_INTERVAL;
        if (findBonemeal().isEmpty() || !ForgeEventFactory.getMobGriefingEvent(villager.world, villager)) return false;
        crop = findCrop();
        return crop != null;
    }

    private boolean canWork() {
        return villager.isEntityAlive() && villager.getProfessionForge() == VillagerCareers.DEALER
                && VillagerWork.canWork(villager) && hasWorksite();
    }

    private boolean hasWorksite() {
        World world = villager.world;
        BlockPos site = VillagerWork.worksite(villager);
        if (site == null || VillagerWork.worksiteDimension(villager) != world.provider.getDimension()
                || !world.isBlockLoaded(site)) return false;
        DryingTableBlockEntity table = VillagerWork.table(world, site);
        return table != null && table.availableTo(villager.getUniqueID());
    }

    @Override
    public void startExecuting() {
        working = true;
        nextApplication = villager.world.getTotalWorldTime();
        endTime = nextApplication + SESSION_LENGTH;
        display.showWorkItem(new ItemStack(Items.DYE, 1, BONE_MEAL));
        approach(crop);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return crop != null && villager.world.getTotalWorldTime() <= endTime && canWork();
    }

    @Override
    public void updateTask() {
        if (crop == null) return;
        if (!isUnripeCrop(crop)) {
            approach(findCrop());
            if (crop == null) return;
        }
        villager.getLookHelper().setLookPosition(crop.getX() + 0.5D, crop.getY() + 0.5D, crop.getZ() + 0.5D,
                20, villager.getVerticalFaceSpeed());
        long now = villager.world.getTotalWorldTime();
        if (now < nextApplication || villager.getDistanceSqToCenter(crop) >= 1D) return;
        ItemStack meal = findBonemeal();
        if (meal.isEmpty() || !ForgeEventFactory.getMobGriefingEvent(villager.world, villager)) return;
        if (ItemDye.applyBonemeal(meal, villager.world, crop)) {
            villager.getVillagerInventory().markDirty();
            villager.world.playEvent(2005, crop, 0);
            nextApplication = now + APPLICATION_INTERVAL;
            approach(findCrop());
        }
    }

    @Override
    public void resetTask() {
        if (!working) return;
        working = false;
        crop = null;
        nextSearch = villager.ticksExisted + SESSION_COOLDOWN;
        villager.getNavigator().clearPath();
        display.showWorkItem(ItemStack.EMPTY);
    }

    private void approach(@Nullable BlockPos target) {
        crop = target;
        if (target == null) return;
        villager.getNavigator().tryMoveToXYZ(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.5D);
    }

    @Nullable
    private BlockPos findCrop() {
        BlockPos origin = villager.getPosition();
        BlockPos selected = null;
        int candidates = 0;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-1, -1, -1), origin.add(1, 1, 1))) {
            if (isUnripeCrop(pos) && villager.world.rand.nextInt(++candidates) == 0) selected = pos.toImmutable();
        }
        return selected;
    }

    private boolean isUnripeCrop(BlockPos pos) {
        World world = villager.world;
        if (!world.isBlockLoaded(pos)) return false;
        IBlockState state = world.getBlockState(pos);
        return (state.getBlock() instanceof TallCropBlock || state.getBlock() instanceof BlockCrops)
                && !VillagerWork.mature(state) && ((IGrowable) state.getBlock()).canGrow(world, pos, state, false);
    }

    private ItemStack findBonemeal() {
        IInventory inventory = villager.getVillagerInventory();
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() == Items.DYE && stack.getMetadata() == BONE_MEAL) return stack;
        }
        return ItemStack.EMPTY;
    }
}

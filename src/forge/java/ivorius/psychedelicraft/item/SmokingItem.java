package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public abstract class SmokingItem extends Item {
    private final int drawTicks;
    private final int burnTime;

    SmokingItem(int uses, int drawTicks, int burnTime) {
        this.drawTicks = drawTicks;
        this.burnTime = burnTime;
        setMaxStackSize(1);
        setMaxDamage(uses);
        addPropertyOverride(Psychedelicraft.id("using"), (stack, world, entity) -> PowderItem.useModel(stack, entity));
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return burnTime;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return drawTicks;
    }

    boolean canDraw(EntityPlayer player) {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        DrugProperties properties = DrugCapability.get(player);
        if (properties == null || properties.isBreathingSmoke() || !canDraw(player)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    static void recordDraw(EntityPlayer player, ItemStack stack) {
        if (player instanceof EntityPlayerMP) CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
        player.addStat(StatList.getObjectUseStats(stack.getItem()));
    }

    static ItemStack wear(ItemStack stack, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode) return stack;
        if (!stack.isItemStackDamageable()) {
            stack.shrink(1);
            return stack;
        }
        stack.damageItem(1, entity);
        // The donor breaks at max damage. Vanilla 1.12 only breaks above it.
        if (!stack.isEmpty() && stack.getItemDamage() == stack.getMaxDamage()) {
            entity.renderBrokenItemStack(stack);
            Item item = stack.getItem();
            stack.shrink(1);
            stack.setItemDamage(0);
            if (entity instanceof EntityPlayer) ((EntityPlayer) entity).addStat(StatList.getObjectBreakStats(item));
        }
        return stack;
    }
}

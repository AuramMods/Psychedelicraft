package ivorius.psychedelicraft.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.entity.ThrownTridentEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TridentItem extends Item {
    private static final int USE_DURATION = 72000;
    private static final int MIN_CHARGE = 10;
    private final Multimap<String, AttributeModifier> attributes = ImmutableMultimap.of(
            SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", 8, 0),
            SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.9, 0));

    public TridentItem() {
        setMaxStackSize(1);
        setMaxDamage(250);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItemDamage() >= stack.getMaxDamage() - 1) return new ActionResult<>(EnumActionResult.FAIL, stack);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase user, int timeLeft) {
        if (world.isRemote || !(user instanceof EntityPlayer) || USE_DURATION - timeLeft < MIN_CHARGE) return;
        EntityPlayer player = (EntityPlayer) user;
        ItemStack launched = stack.copy();
        launched.damageItem(1, player);
        ThrownTridentEntity projectile = new ThrownTridentEntity(world, player, launched);
        projectile.shoot(player, player.rotationPitch, player.rotationYaw, 0, 2.5F, 1);
        if (player.capabilities.isCreativeMode) projectile.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
        if (world.spawnEntity(projectile)) {
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            world.playSound(null, projectile.posX, projectile.posY, projectile.posZ, PSSounds.TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1);
            player.addStat(StatList.getObjectUseStats(this));
        }
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase user) {
        if (state.getBlockHardness(world, pos) != 0) stack.damageItem(2, user);
        return true;
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        return slot == EntityEquipmentSlot.MAINHAND ? attributes : super.getAttributeModifiers(slot, stack);
    }
}

package ivorius.psychedelicraft.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SuspiciousStewItem extends ItemFood {
    SuspiciousStewItem() {
        super(6, 0.6F, false);
        setAlwaysEdible();
        setMaxStackSize(1);
    }

    public static ItemStack withEffect(Potion potion, int duration) {
        return withEffect(new PotionEffect(potion, duration));
    }

    public static ItemStack withEffect(PotionEffect effect) {
        return PotionUtils.appendEffects(new ItemStack(PSItems.SUSPICIOUS_STEW), Collections.singletonList(effect));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!player.isCreative()) return super.onItemRightClick(world, player, hand);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        boolean creative = user instanceof EntityPlayer && ((EntityPlayer) user).isCreative();
        ItemStack consumed = creative ? stack.copy() : stack;
        List<PotionEffect> effects = world.isRemote ? Collections.emptyList() : PotionUtils.getFullEffectsFromItem(consumed);
        super.onItemUseFinish(consumed, world, user);
        effects.forEach(user::addPotionEffect);
        return creative ? stack : new ItemStack(Items.BOWL);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (!isInCreativeTab(tab)) return;
        Set<PotionEffect> added = new HashSet<>();
        for (StewFlower flower : StewFlower.values()) {
            PotionEffect effect = flower.createEffect();
            if (added.add(effect)) stacks.add(withEffect(effect));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null && player.isCreative()) PotionUtils.addPotionTooltip(stack, tooltip, 1F);
    }
}

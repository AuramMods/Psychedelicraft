package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.FluidVessel;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.NonNullList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class HoneyBottleItem extends PortableFluidItem {
    HoneyBottleItem() {
        super(FluidVessel.GLASS_BOTTLE);
        setMaxStackSize(16);
        setContainerItem(Items.GLASS_BOTTLE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (isInCreativeTab(tab)) stacks.add(new ItemStack(this));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 40;
    }

    @SubscribeEvent
    public static void replaceDrinkSound(PlaySoundAtEntityEvent event) {
        if (event.getSound() == SoundEvents.ENTITY_GENERIC_DRINK && event.getEntity() instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase) event.getEntity();
            if (entity.getActiveItemStack().getItem() instanceof HoneyBottleItem) event.setSound(PSSounds.HONEY_DRINK);
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        if (!world.isRemote) {
            if (player != null) {
                player.getFoodStats().addStats(6, 0.1F);
                player.addStat(StatList.getObjectUseStats(this));
                world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_BURP,
                        SoundCategory.PLAYERS, 0.5F, 0.9F + world.rand.nextFloat() * 0.1F);
                if (player instanceof EntityPlayerMP) CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ, PSSounds.HONEY_DRINK,
                    SoundCategory.NEUTRAL, 1F, 1F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.4F);
            entity.removePotionEffect(MobEffects.POISON);
        }
        if (player != null && player.capabilities.isCreativeMode) return stack;
        stack.shrink(1);
        ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) return emptyBottle;
        if (!world.isRemote && player != null) ItemHandlerHelper.giveItemToPlayer(player, emptyBottle);
        return stack;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(getUnlocalizedName(stack) + ".name");
    }
}

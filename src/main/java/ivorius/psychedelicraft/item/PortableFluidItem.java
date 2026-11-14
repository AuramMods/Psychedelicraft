package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.PlacedDrinksBlock;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidVessel;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class PortableFluidItem extends Item {
    private final FluidVessel vessel;

    public PortableFluidItem(FluidVessel vessel) {
        this.vessel = vessel;
        setMaxStackSize(vessel == FluidVessel.BOTTLE ? 16 : 1);
        addPropertyOverride(Psychedelicraft.id("filled"), (stack, world, entity) -> contents(stack) == null ? 0F : 1F);
    }

    public FluidVessel getVessel() {
        return vessel;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (isInCreativeTab(tab)) PSItemGroups.addVesselStacks(this, tab, stacks);
    }

    @Nullable
    public FluidStack contents(ItemStack stack) {
        return FluidVesselHandler.contents(stack, vessel);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound tag) {
        return new FluidVesselHandler(stack, vessel);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return vessel == FluidVessel.BOTTLE && contents(stack) != null ? 1 : super.getItemStackLimit(stack);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing face,
                                          float x, float y, float z, EnumHand hand) {
        if (world.isRemote || !world.isBlockModifiable(player, pos)) return EnumActionResult.PASS;
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BarrelBlock || block instanceof MashTubBlock) return EnumActionResult.PASS;
        boolean transferred = FluidUtil.interactWithFluidHandler(player, hand, world, pos, face);
        return transferred ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing side, float hitX, float hitY, float hitZ) {
        return PlacedDrinksBlock.placeOnBlock(player, world, pos, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (serving(stack) != null) {
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        FluidStack portion = serving(stack);
        if (portion == null || !(entity instanceof EntityPlayer)) return stack;
        EntityPlayer player = (EntityPlayer) entity;
        if (!world.isRemote) {
            PSFluids.consume(player, portion);
            PSCriteria.drankBrew(player, portion);
            player.addStat(StatList.getObjectUseStats(this));
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
            }
        }
        if (player.capabilities.isCreativeMode) return stack;
        FluidVesselHandler handler = new FluidVesselHandler(stack, vessel);
        handler.drain(portion, true);
        return handler.getContainer();
    }

    @Nullable
    private FluidStack serving(ItemStack stack) {
        FluidStack fluid = contents(stack);
        if (stack.getCount() != 1 || vessel.serving == 0 || fluid == null || fluid.amount < vessel.serving
                || !PSFluids.canConsume(fluid.getFluid(), vessel == FluidVessel.SYRINGE)) {
            return null;
        }
        fluid.amount = vessel.serving;
        return fluid;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        if (serving(stack) == null) return EnumAction.NONE;
        return vessel == FluidVessel.SYRINGE ? EnumAction.BOW : EnumAction.DRINK;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return vessel.useTicks;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid == null ? super.getItemStackDisplayName(stack)
                : I18n.translateToLocalFormatted(getUnlocalizedName(stack) + ".filled", PSFluids.displayName(fluid));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        FluidStack fluid = contents(stack);
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted("psychedelicraft.drink.levels",
                fluid == null ? 0 : fluid.amount, vessel.capacity));
        if (fluid != null && fluid.getFluid() instanceof BrewFluid) BrewFluid.appendAttributes(fluid, tooltip);
        if (flag.isAdvanced() && fluid != null) {
            tooltip.add(TextFormatting.DARK_GRAY + PSFluids.id(fluid.getFluid()));
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid != null && fluid.amount < vessel.capacity;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid == null ? 1 : 1 - fluid.amount / (double) vessel.capacity;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0xAAAAFF;
    }
}

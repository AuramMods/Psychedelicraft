/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.MolotovCocktailEntity;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidVessel;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class MolotovCocktailItem extends PortableFluidItem {
    public static final String FLYING = "flying";
    private static final int USE_TICKS = 7200;

    MolotovCocktailItem() {
        super(FluidVessel.BOTTLE);
        setMaxStackSize(1);
        addPropertyOverride(Psychedelicraft.id(FLYING),
                (stack, world, entity) -> stack.hasTagCompound() && stack.getTagCompound().getBoolean(FLYING) ? 1 : 0);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        return stack;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase user, int remainingUseTicks) {
        if (world.isRemote || stack.isEmpty()) return;
        MolotovCocktailEntity projectile = new MolotovCocktailEntity(world, user, stack);
        float speed = 0.5F * MathHelper.clamp(remainingUseTicks, 0, USE_TICKS) / USE_TICKS;
        projectile.shoot(user, user.rotationPitch, user.rotationYaw, 0, speed, 1);
        if (!world.spawnEntity(projectile)) return;

        world.playSound(null, user.posX, user.posY, user.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL,
                0.5F, 0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            player.addStat(StatList.getObjectUseStats(this));
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        FluidStack fluid = contents(stack);
        String key = getUnlocalizedName(stack);
        if (fluid == null) return I18n.translateToLocal(key + ".empty");
        float strength = BrewFluid.projectileStrength(fluid);
        int quality = MathHelper.clamp(MathHelper.floor(strength * (0.6F * 0.8F + 2F * 0.6F) + 0.5F), 0, 7);
        return I18n.translateToLocal(key + ".quality." + quality);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (!isInCreativeTab(tab) || PSConfig.balancing.disableMolotovs) return;
        stacks.add(new ItemStack(this));
        int amount = getVessel().capacity;
        for (Fluid fluid : PSFluids.all()) {
            if (PSFluids.isInjectable(fluid) || fluid == PSFluids.COFFEE) continue;
            if (fluid instanceof BrewFluid) {
                ((BrewFluid) fluid).addCreativeVariants(amount, false, contents -> stacks.add(filled(contents)));
            } else {
                stacks.add(filled(new FluidStack(fluid, amount)));
            }
        }
    }

    private ItemStack filled(FluidStack contents) {
        return new FluidVesselHandler(new ItemStack(this), getVessel())
                .craftContents(contents.getFluid(), contents.amount, contents.tag);
    }
}

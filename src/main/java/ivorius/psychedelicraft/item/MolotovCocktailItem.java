/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;
import ivorius.psychedelicraft.entity.MolotovCocktailEntity;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

public final class MolotovCocktailItem extends PortableFluidItem {
    public static final String FLYING="flying";
    public static final int USE_TICKS=7200;
    public MolotovCocktailItem(){super(FluidVessel.BOTTLE,new Properties().stacksTo(1));}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public int getUseDuration(ItemStack stack){return USE_TICKS;}
    @Override public boolean useOnRelease(ItemStack stack){return true;}
    @Override public FluidStack serving(ItemStack stack){return FluidStack.EMPTY;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){return stack;}
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
        if(level.isClientSide||stack.isEmpty())return;
        var projectile=new MolotovCocktailEntity(level,user,stack);
        projectile.shootFromRotation(user,user.getXRot(),user.getYRot(),0,.5F*Mth.clamp(remaining,0,USE_TICKS)/USE_TICKS,1);
        if(!level.addFreshEntity(projectile))return;
        level.playSound(null,user.getX(),user.getY(),user.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.NEUTRAL,.5F,.4F/(level.random.nextFloat()*.4F+.8F));
        if(user instanceof Player player){if(!player.getAbilities().instabuild)stack.shrink(1);player.awardStat(Stats.ITEM_USED.get(this));}
    }
    public static int quality(FluidStack fluid){return Mth.clamp(Mth.floor(BrewProfile.projectileStrength(fluid)*(.6F*.8F+2*.6F)+.5F),0,7);}
    @Override public Component getName(ItemStack stack){var fluid=contents(stack);return Component.translatable(getDescriptionId()+(fluid.isEmpty()?".empty":".quality."+quality(fluid)));}
}

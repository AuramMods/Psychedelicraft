/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.IntSupplier;

public final class BagOfVomitItem extends Item {
    public static final Item[] FORMS={Items.COOKIE,Items.MUSHROOM_STEW,Items.GOLDEN_APPLE,Items.COOKED_BEEF,Items.COOKED_CHICKEN};
    private static IntSupplier disguise=()->0;
    public BagOfVomitItem(){super(new Properties().food(new FoodProperties.Builder().nutrition(8).saturationMod(.8F).meat().alwaysEat().build()));}
    public static void disguise(IntSupplier supplier){disguise=supplier;}
    public static int form(){return disguise.getAsInt();}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){if(user instanceof Player player&&player.getAbilities().instabuild){super.finishUsingItem(stack.copy(),level,user);return stack;}return super.finishUsingItem(stack,level,user);}
    @Override public Component getName(ItemStack stack){int form=form();return form==0?super.getName(stack):new ItemStack(FORMS[form-1]).getHoverName();}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){int form=form();if(form>0)FORMS[form-1].appendHoverText(new ItemStack(FORMS[form-1]),level,tooltip,flag);}
}

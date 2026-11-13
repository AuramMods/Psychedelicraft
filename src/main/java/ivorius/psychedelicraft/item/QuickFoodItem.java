package ivorius.psychedelicraft.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

class QuickFoodItem extends ItemFood {
    QuickFoodItem(int hunger, float saturation, boolean wolfFood) {
        super(hunger, saturation, wolfFood);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 15;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        if (user instanceof EntityPlayer && ((EntityPlayer) user).isCreative()) {
            super.onItemUseFinish(stack.copy(), world, user);
            return stack;
        }
        return super.onItemUseFinish(stack, world, user);
    }
}

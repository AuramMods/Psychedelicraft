/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class WineGrapesItem extends Item {
    public WineGrapesItem() { super(new Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(.5F).meat().build())); }
    @Override public int getUseDuration(ItemStack stack) { return 15; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        if (consumer instanceof Player player && player.getAbilities().instabuild) { super.finishUsingItem(stack.copy(), level, consumer); return stack; }
        return super.finishUsingItem(stack, level, consumer);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos();
        if (player == null || player.isShiftKeyDown() || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())
                || !level.mayInteract(player, pos) || !level.getBlockState(pos).is(PSBlocks.LATTICE.get())) return InteractionResult.PASS;
        if (!level.isClientSide && LatticeBlock.growOnto(level, pos, PSBlocks.WINE_GRAPE_LATTICE.get()))
            level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_HIT, SoundSource.BLOCKS, 1, 1);
        // Both reference versions deliberately keep the planting grapes.
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

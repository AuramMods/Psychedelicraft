/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class RiftJarItem extends BlockItem {
    public static final String FILL="riftFraction";
    public RiftJarItem(){super(PSBlocks.RIFT_JAR.get(),new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new ivorius.psychedelicraft.client.render.blocks.RiftJarItemRenderer();return renderer;}});}
    public ItemStack filled(float amount){var stack=new ItemStack(this);if(Float.isFinite(amount)&&amount>0)stack.getOrCreateTag().putFloat(FILL,amount);return stack;}
    public static float getRiftFraction(ItemStack stack){float value=stack.hasTag()?stack.getTag().getFloat(FILL):0;return Float.isFinite(value)?Math.max(0,value):0;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){super.appendHoverText(stack,level,lines,flags);float value=getRiftFraction(stack);String fill=value<=0?"empty":value<.4F?"slightly_filled":value<.6F?"half_filled":value<.8F?"filled":"over_filled";lines.add(Component.translatable(getDescriptionId()+"."+fill).withStyle(ChatFormatting.GRAY));}
}

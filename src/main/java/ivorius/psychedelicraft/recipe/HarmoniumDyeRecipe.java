/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.item.HarmoniumItem;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class HarmoniumDyeRecipe extends CustomRecipe {
    public HarmoniumDyeRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    @Override public boolean matches(CraftingContainer inventory,Level level){return !assemble(inventory,RegistryAccess.EMPTY).isEmpty();}
    @Override public ItemStack assemble(CraftingContainer inventory,RegistryAccess registries){
        ItemStack harmonium=ItemStack.EMPTY;int[] channels=new int[3];int brightness=0,samples=0;boolean hasDye=false;
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            ItemStack stack=inventory.getItem(slot);if(stack.isEmpty())continue;
            int color;
            if(stack.is(PSItems.HARMONIUM.get())){
                if(!harmonium.isEmpty())return ItemStack.EMPTY;
                harmonium=stack;if(!HarmoniumItem.hasColor(stack))continue;color=HarmoniumItem.getColor(stack);
            }else{
                DyeColor dye=DyeColor.getColor(stack);if(dye==null)return ItemStack.EMPTY;
                color=HarmoniumItem.packColor(dye.getTextureDiffuseColors());hasDye=true;
            }
            int red=color>>16&255,green=color>>8&255,blue=color&255;
            brightness+=Math.max(red,Math.max(green,blue));channels[0]+=red;channels[1]+=green;channels[2]+=blue;samples++;
        }
        if(harmonium.isEmpty()||!hasDye)return ItemStack.EMPTY;
        int red=channels[0]/samples,green=channels[1]/samples,blue=channels[2]/samples,peak=Math.max(red,Math.max(green,blue));
        float scale=peak==0?0:(float)brightness/samples/peak;
        ItemStack result=harmonium.copy();result.setCount(1);HarmoniumItem.setColor(result,(int)(red*scale)<<16|(int)(green*scale)<<8|(int)(blue*scale));return result;
    }
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.HARMONIUM_DYE.get();}
}

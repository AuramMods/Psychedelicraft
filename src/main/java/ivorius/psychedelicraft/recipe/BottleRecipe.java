/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import ivorius.psychedelicraft.item.HarmoniumItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.StainedGlassBlock;

public final class BottleRecipe extends ShapedRecipe {
    BottleRecipe(ShapedRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),recipe.getWidth(),recipe.getHeight(),recipe.getIngredients(),recipe.getResultItem(RegistryAccess.EMPTY),recipe.showNotification());}
    @Override public ItemStack assemble(CraftingContainer inventory,RegistryAccess registries){
        ItemStack result=super.assemble(inventory,registries);
        for(int slot=0;slot<inventory.getContainerSize();slot++)if(inventory.getItem(slot).getItem() instanceof BlockItem block&&block.getBlock() instanceof StainedGlassBlock glass){HarmoniumItem.setColor(result,glass.getColor().getTextColor());break;}return result;
    }
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.BOTTLE.get();}
    public static final class Serializer implements RecipeSerializer<BottleRecipe>{
        private final ShapedRecipe.Serializer delegate=new ShapedRecipe.Serializer();
        @Override public BottleRecipe fromJson(ResourceLocation id,JsonObject json){return new BottleRecipe(delegate.fromJson(id,json));}
        @Override public BottleRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new BottleRecipe(delegate.fromNetwork(id,buffer));}
        @Override public void toNetwork(FriendlyByteBuf buffer,BottleRecipe recipe){delegate.toNetwork(buffer,recipe);}
    }
}

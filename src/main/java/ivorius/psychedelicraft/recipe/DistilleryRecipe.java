/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

public final class DistilleryRecipe extends ShapedRecipe {
    DistilleryRecipe(ShapedRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),recipe.getWidth(),recipe.getHeight(),recipe.getIngredients(),recipe.getResultItem(RegistryAccess.EMPTY),recipe.showNotification());}
    @Override public ItemStack assemble(CraftingContainer inventory,RegistryAccess registries){
        ItemStack result=super.assemble(inventory,registries);
        for(int slot=0;slot<inventory.getContainerSize();slot++)if(inventory.getItem(slot).is(ivorius.psychedelicraft.item.PSItems.FLASK.get())){
            if(inventory.getItem(slot).getTag()!=null)result.setTag(inventory.getItem(slot).getTag().copy());break;
        }return result;
    }
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.DISTILLERY.get();}
    public static final class Serializer implements RecipeSerializer<DistilleryRecipe>{
        private final ShapedRecipe.Serializer delegate=new ShapedRecipe.Serializer();
        @Override public DistilleryRecipe fromJson(ResourceLocation id,JsonObject json){return new DistilleryRecipe(delegate.fromJson(id,json));}
        @Override public DistilleryRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new DistilleryRecipe(delegate.fromNetwork(id,buffer));}
        @Override public void toNetwork(FriendlyByteBuf buffer,DistilleryRecipe recipe){delegate.toNetwork(buffer,recipe);}
    }
}

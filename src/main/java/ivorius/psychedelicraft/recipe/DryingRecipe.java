/* Copyright (c) 2014, Lukas Tenbrink. Target-version serializer by Sollace. */
package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

public final class DryingRecipe implements Recipe<Container> {
    public static final int INPUT_COUNT=9;
    public static final int OUTPUT=0;
    private final ResourceLocation id;
    private final String group;
    private final Ingredient input;
    private final ItemStack output;
    private final float experience;
    private final int cookTime;

    public DryingRecipe(ResourceLocation id,String group,Ingredient input,ItemStack output,float experience,int cookTime){
        if(output.isEmpty()||output.getCount()>output.getMaxStackSize())throw new JsonSyntaxException("Drying output must fit in one stack");
        this.id=id;this.group=group;this.input=input;this.output=output.copy();this.experience=experience;this.cookTime=cookTime;
    }
    @Override public boolean matches(Container inventory,Level level){
        if(inventory.getContainerSize()!=INPUT_COUNT+1||!inventory.getItem(OUTPUT).isEmpty())return false;
        for(int slot=1;slot<=INPUT_COUNT;slot++)if(inventory.getItem(slot).getCount()!=1||!input.test(inventory.getItem(slot)))return false;
        return true;
    }
    public Ingredient getInput(){return input;}
    public ItemStack getOutput(){return output.copy();}
    public float getExperience(){return experience;}
    public int getCookTime(){return cookTime;}
    @Override public ItemStack assemble(Container inventory,RegistryAccess registries){return getOutput();}
    @Override public ItemStack getResultItem(RegistryAccess registries){return getOutput();}
    @Override public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,input);}
    @Override public boolean canCraftInDimensions(int width,int height){return false;}
    @Override public boolean isSpecial(){return true;}
    @Override public String getGroup(){return group;}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeType<?> getType(){return PSRecipes.DRYING_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.DRYING.get();}
    @Override public ItemStack getToastSymbol(){return new ItemStack(PSItems.DRYING_TABLE.get());}

    public static final class Serializer implements RecipeSerializer<DryingRecipe>{
        @Override public DryingRecipe fromJson(ResourceLocation id,JsonObject json){
            return new DryingRecipe(id,GsonHelper.getAsString(json,"group",""),Ingredient.fromJson(json.get("ingredient")),
                    CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json,"result"),true,true),
                    GsonHelper.getAsFloat(json,"experience",0),GsonHelper.getAsInt(json,"cookingtime",200));
        }
        @Override public DryingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){
            return new DryingRecipe(id,buffer.readUtf(),Ingredient.fromNetwork(buffer),buffer.readItem(),buffer.readFloat(),buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,DryingRecipe recipe){
            buffer.writeUtf(recipe.group);recipe.input.toNetwork(buffer);buffer.writeItem(recipe.output);
            buffer.writeFloat(recipe.experience);buffer.writeVarInt(recipe.cookTime);
        }
    }
}

/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.recipe;

import com.google.gson.*;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.RecipeMatcher;
import java.util.*;

public final class FillReceptacleRecipe extends CustomRecipe {
    private final String group;
    private final Ingredient receptacle;
    private final NonNullList<Ingredient> ingredients;
    private final Fluid fluid;
    private final int amount;
    private final CompoundTag attributes;
    public FillReceptacleRecipe(ResourceLocation id,String group,Ingredient receptacle,NonNullList<Ingredient> ingredients,Fluid fluid,int amount,CompoundTag attributes){
        super(id,CraftingBookCategory.MISC);this.group=group;this.receptacle=receptacle;this.ingredients=ingredients;this.fluid=fluid;this.amount=amount;this.attributes=attributes.copy();
    }
    @Override public boolean matches(CraftingContainer inventory,Level level){
        List<ItemStack> inputs=new ArrayList<>();for(int slot=0;slot<inventory.getContainerSize();slot++)if(!inventory.getItem(slot).isEmpty())inputs.add(inventory.getItem(slot));
        return inputs.size()==ingredients.size()&&RecipeMatcher.findMatches(inputs,ingredients)!=null&&!assemble(inventory,RegistryAccess.EMPTY).isEmpty();
    }
    private int receptacleSlot(CraftingContainer inventory){
        int result=-1;for(int slot=0;slot<inventory.getContainerSize();slot++)if(!inventory.getItem(slot).isEmpty()&&receptacle.test(inventory.getItem(slot))){if(result>=0)return -1;result=slot;}return result;
    }
    @Override public ItemStack assemble(CraftingContainer inventory,RegistryAccess registries){int slot=receptacleSlot(inventory);return slot<0?ItemStack.EMPTY:fill(inventory.getItem(slot));}
    private ItemStack fill(ItemStack stack){FluidVesselHandler handler=FluidVesselHandler.forCrafting(stack);return handler==null?ItemStack.EMPTY:handler.craftContents(fluid,amount,attributes);}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory){
        NonNullList<ItemStack> result=NonNullList.withSize(inventory.getContainerSize(),ItemStack.EMPTY);int used=receptacleSlot(inventory);
        if(used>=0)for(int slot=0;slot<result.size();slot++)if(slot!=used)result.set(slot,inventory.getItem(slot).getCraftingRemainingItem());return result;
    }
    @Override public ItemStack getResultItem(RegistryAccess registries){for(ItemStack stack:receptacle.getItems()){ItemStack result=fill(stack);if(!result.isEmpty())return result;}return ItemStack.EMPTY;}
    /** Matched vessel/output pairs for recipe browsers, assembled through the actual recipe. */
    public List<ItemStack> getDisplayOutputs(){return Arrays.stream(receptacle.getItems()).map(this::fill).toList();}
    @Override public String getGroup(){return group;}
    @Override public NonNullList<Ingredient> getIngredients(){return ingredients;}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=ingredients.size();}
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.FILL_RECEPTACLE.get();}
    public static final class Serializer implements RecipeSerializer<FillReceptacleRecipe>{
        @Override public FillReceptacleRecipe fromJson(ResourceLocation id,JsonObject json){
            Ingredient vessel=Ingredient.fromJson(json.get("receptical"));NonNullList<Ingredient> ingredients=NonNullList.create();ingredients.add(vessel);
            for(JsonElement value:GsonHelper.getAsJsonArray(json,"ingredients"))ingredients.add(Ingredient.fromJson(value));
            if(ingredients.size()<2||ingredients.size()>9)throw new JsonSyntaxException("Expected one receptacle and one to eight ingredients");
            JsonObject output=GsonHelper.getAsJsonObject(json,"result");Fluid fluid=PSFluids.resolve(GsonHelper.getAsString(output,"fluid"));if(fluid==null)throw new JsonSyntaxException("Unknown result fluid");
            CompoundTag attributes=new CompoundTag();if(output.has("attributes"))try{attributes=TagParser.parseTag(output.get("attributes").toString());}catch(Exception error){throw new JsonSyntaxException("Invalid fluid attributes",error);}
            return new FillReceptacleRecipe(id,GsonHelper.getAsString(json,"group",""),vessel,ingredients,fluid,GsonHelper.getAsInt(output,"level",-1),attributes);
        }
        @Override public FillReceptacleRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){
            String group=buffer.readUtf();Ingredient vessel=Ingredient.fromNetwork(buffer);NonNullList<Ingredient> ingredients=NonNullList.create();ingredients.add(vessel);
            int count=buffer.readVarInt();for(int i=0;i<count;i++)ingredients.add(Ingredient.fromNetwork(buffer));
            Fluid fluid=Objects.requireNonNull(PSFluids.resolve(buffer.readResourceLocation().toString()));int amount=buffer.readVarInt();CompoundTag attributes=buffer.readNbt();
            return new FillReceptacleRecipe(id,group,vessel,ingredients,fluid,amount,attributes==null?new CompoundTag():attributes);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,FillReceptacleRecipe recipe){
            buffer.writeUtf(recipe.group);recipe.receptacle.toNetwork(buffer);buffer.writeVarInt(recipe.ingredients.size()-1);
            for(int i=1;i<recipe.ingredients.size();i++)recipe.ingredients.get(i).toNetwork(buffer);
            buffer.writeResourceLocation(new ResourceLocation(PSFluids.id(recipe.fluid)));buffer.writeVarInt(recipe.amount);buffer.writeNbt(recipe.attributes);
        }
    }
}

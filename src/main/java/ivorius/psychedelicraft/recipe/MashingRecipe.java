/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on 7 Feb 2023. */
package ivorius.psychedelicraft.recipe;
import com.google.gson.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

public final class MashingRecipe implements Recipe<Container> {
    public static final int MAX_INGREDIENTS=8;
    private final ResourceLocation id;
    private final String group;
    private final Fluid pool,result;
    private final int minimum,stewTime;
    private final CompoundTag poolAttributes,resultAttributes;
    private final NonNullList<Ingredient> ingredients;
    public MashingRecipe(ResourceLocation id,String group,Fluid pool,int minimum,CompoundTag poolAttributes,Fluid result,CompoundTag resultAttributes,NonNullList<Ingredient> ingredients,int stewTime){
        this.id=id;this.group=group;this.pool=pool;this.minimum=minimum;this.poolAttributes=poolAttributes.copy();this.result=result;this.resultAttributes=resultAttributes.copy();this.ingredients=ingredients;this.stewTime=stewTime;
    }
    public boolean matchesPool(FluidStack contents){return !contents.isEmpty()&&contents.getFluid()==pool&&contents.getAmount()>=minimum&&(poolAttributes.isEmpty()||NbtUtils.compareNbt(poolAttributes,contents.getTag(),true));}
    public boolean acceptsIngredient(ItemStack stack){return ingredients.stream().anyMatch(ingredient->ingredient.test(stack));}
    public FluidStack getMinimumPool(){return new FluidStack(pool,minimum,poolAttributes.isEmpty()?null:poolAttributes.copy());}
    public boolean matchesPartial(List<ItemStack> supplied){return supplied.size()<=ingredients.size()&&assign(supplied,0,0);}
    public boolean matchesIngredients(List<ItemStack> supplied){return supplied.size()==ingredients.size()&&matchesPartial(supplied);}
    private boolean assign(List<ItemStack> supplied,int index,int occupied){
        if(index==supplied.size())return true;
        for(int i=0;i<ingredients.size();i++){int bit=1<<i;if((occupied&bit)==0&&ingredients.get(i).test(supplied.get(index))&&assign(supplied,index+1,occupied|bit))return true;}return false;
    }
    public FluidStack finish(int amount){return new FluidStack(result,amount,resultAttributes.isEmpty()?null:resultAttributes.copy());}
    public int getStewTime(){return stewTime;}
    @Override public NonNullList<Ingredient> getIngredients(){return ingredients;}
    @Override public ResourceLocation getId(){return id;}
    @Override public String getGroup(){return group;}
    @Override public boolean matches(Container input,Level level){return false;}
    @Override public ItemStack assemble(Container input,RegistryAccess lookup){return ItemStack.EMPTY;}
    @Override public boolean canCraftInDimensions(int width,int height){return false;}
    @Override public ItemStack getResultItem(RegistryAccess lookup){return FluidVesselHandler.forCrafting(new ItemStack(PSItems.MASH_TUB.get())).replaceContents(finish(32000));}
    @Override public boolean isSpecial(){return true;}
    @Override public RecipeType<?> getType(){return PSRecipes.MASHING_TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.MASHING.get();}
    public static final class Serializer implements RecipeSerializer<MashingRecipe>{
        @Override public MashingRecipe fromJson(ResourceLocation id,JsonObject json){
            NonNullList<Ingredient> ingredients=NonNullList.create();for(JsonElement value:GsonHelper.getAsJsonArray(json,"ingredients"))ingredients.add(Ingredient.fromJson(value));
            if(ingredients.isEmpty()||ingredients.size()>MAX_INGREDIENTS)throw new JsonSyntaxException("Expected one to eight mashing ingredients");
            JsonObject pool=GsonHelper.getAsJsonObject(json,"base_fluid"),result=GsonHelper.getAsJsonObject(json,"result");int stew=Math.max(0,GsonHelper.getAsInt(json,"stew_time",0));
            if(stew>Integer.MAX_VALUE-5)throw new JsonSyntaxException("Mashing stew time is too large");
            return new MashingRecipe(id,GsonHelper.getAsString(json,"group",""),fluid(pool),Math.max(1,GsonHelper.getAsInt(pool,"level",1)),attributes(pool),fluid(result),attributes(result),ingredients,stew);
        }
        private static Fluid fluid(JsonObject object){String name=GsonHelper.getAsString(object,"fluid");Fluid fluid=PSFluids.resolve(name);if(fluid==null)throw new JsonSyntaxException("Unknown fluid "+name);return fluid;}
        private static CompoundTag attributes(JsonObject object){
            if(!object.has("attributes"))return new CompoundTag();JsonElement tag=object.get("attributes");
            try{return TagParser.parseTag(tag.isJsonPrimitive()?tag.getAsString():tag.toString());}catch(Exception error){throw new JsonSyntaxException("Invalid mashing fluid attributes",error);}
        }
        @Override public MashingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){
            String group=buffer.readUtf();Fluid pool=Objects.requireNonNull(PSFluids.resolve(buffer.readResourceLocation().toString()));int minimum=buffer.readVarInt();CompoundTag poolAttributes=buffer.readNbt();
            Fluid result=Objects.requireNonNull(PSFluids.resolve(buffer.readResourceLocation().toString()));CompoundTag resultAttributes=buffer.readNbt();int count=buffer.readVarInt();NonNullList<Ingredient> ingredients=NonNullList.create();
            for(int i=0;i<count;i++)ingredients.add(Ingredient.fromNetwork(buffer));return new MashingRecipe(id,group,pool,minimum,poolAttributes==null?new CompoundTag():poolAttributes,result,resultAttributes==null?new CompoundTag():resultAttributes,ingredients,buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,MashingRecipe recipe){
            buffer.writeUtf(recipe.group);buffer.writeResourceLocation(new ResourceLocation(PSFluids.id(recipe.pool)));buffer.writeVarInt(recipe.minimum);buffer.writeNbt(recipe.poolAttributes);
            buffer.writeResourceLocation(new ResourceLocation(PSFluids.id(recipe.result)));buffer.writeNbt(recipe.resultAttributes);buffer.writeVarInt(recipe.ingredients.size());for(var ingredient:recipe.ingredients)ingredient.toNetwork(buffer);buffer.writeVarInt(recipe.stewTime);
        }
    }
}

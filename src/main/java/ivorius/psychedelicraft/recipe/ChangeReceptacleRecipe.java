/* Copyright (c) 2014, Lukas Tenbrink. Native recipe adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.recipe;
import com.google.gson.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.util.RecipeMatcher;
import java.util.*;

/** One consumed receptacle becomes one result; previews never drain the source or return it as a remainder. */
public final class ChangeReceptacleRecipe extends CustomRecipe {
    private final String group;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack output;
    public ChangeReceptacleRecipe(ResourceLocation id,String group,NonNullList<Ingredient> ingredients,ItemStack output){super(id,CraftingBookCategory.MISC);this.group=group;this.ingredients=ingredients;this.output=output.copy();}
    private int source(CraftingContainer grid){int result=-1;for(int slot=0;slot<grid.getContainerSize();slot++)if(FluidVesselHandler.forCrafting(grid.getItem(slot))!=null){if(result>=0)return -1;result=slot;}return result;}
    @Override public boolean matches(CraftingContainer grid,Level level){var stacks=new ArrayList<ItemStack>();for(int slot=0;slot<grid.getContainerSize();slot++)if(!grid.getItem(slot).isEmpty())stacks.add(grid.getItem(slot));return stacks.size()==ingredients.size()&&RecipeMatcher.findMatches(stacks,ingredients)!=null&&!assemble(grid,level.registryAccess()).isEmpty();}
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registries){
        int slot=source(grid);if(slot<0)return ItemStack.EMPTY;var input=grid.getItem(slot);var fluid=FluidVesselHandler.forCrafting(input).getFluid();var result=output.copy();var destination=FluidVesselHandler.forCrafting(result);
        int existing=destination==null?0:destination.getFluid().getAmount();
        if(!fluid.isEmpty()&&(destination==null||result.getCount()!=1||fluid.getAmount()>destination.getTankCapacity(0)-existing||!destination.isFluidValid(0,fluid)||existing>0&&!destination.getFluid().isFluidEqual(fluid)))return ItemStack.EMPTY;
        // The explicit port goal preserves names/custom item fields as well as fluid and glass color.
        if(input.getItem() instanceof PortableFluidItem&&result.getItem() instanceof PortableFluidItem){
            CompoundTag attributes=input.hasTag()?input.getTag().copy():new CompoundTag();attributes.remove("fluid");attributes.remove("flying");if(result.hasTag())attributes.merge(result.getTag());result.setTag(attributes.isEmpty()?null:attributes);
        }
        if(!fluid.isEmpty()){var combined=fluid.copy();combined.setAmount(fluid.getAmount()+existing);result=FluidVesselHandler.forCrafting(result).replaceContents(combined);}
        return result;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){var result=NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);int used=source(grid);for(int slot=0;slot<grid.getContainerSize();slot++)if(slot!=used)result.set(slot,ForgeHooks.getCraftingRemainingItem(grid.getItem(slot)));return result;}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=ingredients.size();}
    @Override public NonNullList<Ingredient> getIngredients(){return ingredients;}
    @Override public ItemStack getResultItem(RegistryAccess registries){return output.copy();}
    @Override public String getGroup(){return group;}
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.CHANGE_RECEPTACLE.get();}
    public static final class Serializer implements RecipeSerializer<ChangeReceptacleRecipe> {
        @Override public ChangeReceptacleRecipe fromJson(ResourceLocation id,JsonObject json){var ingredients=NonNullList.<Ingredient>create();for(var element:json.getAsJsonArray("ingredients"))ingredients.add(Ingredient.fromJson(element));var output=CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json,"result"),true);if(ingredients.isEmpty()||ingredients.size()>9||output.isEmpty())throw new JsonSyntaxException("Invalid receptacle conversion ingredients/result");return new ChangeReceptacleRecipe(id,GsonHelper.getAsString(json,"group",""),ingredients,output);}
        @Override public ChangeReceptacleRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){String group=buffer.readUtf();int count=buffer.readVarInt();if(count<1||count>9)throw new IllegalArgumentException("Invalid receptacle conversion ingredient count");var ingredients=NonNullList.withSize(count,Ingredient.EMPTY);for(int i=0;i<count;i++)ingredients.set(i,Ingredient.fromNetwork(buffer));return new ChangeReceptacleRecipe(id,group,ingredients,buffer.readItem());}
        @Override public void toNetwork(FriendlyByteBuf buffer,ChangeReceptacleRecipe recipe){buffer.writeUtf(recipe.group);buffer.writeVarInt(recipe.ingredients.size());recipe.ingredients.forEach(i->i.toNetwork(buffer));buffer.writeItem(recipe.output);}
    }
}

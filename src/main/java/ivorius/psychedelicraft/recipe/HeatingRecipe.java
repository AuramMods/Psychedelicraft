/* Copyright (c) 2014, Lukas Tenbrink. Based on the fluid smelting recipe by Sollace. */
package ivorius.psychedelicraft.recipe;

import com.google.gson.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Native furnaces call assemble for feasibility and completion, retaining arbitrary item data. */
public final class HeatingRecipe extends SmeltingRecipe {
    private final Fluid fluid;
    private final int minimum;
    private final CompoundTag required;
    private final Map<String,Change> changes;
    private final ItemStack replacement;

    private HeatingRecipe(ResourceLocation id,Ingredient ingredient,Fluid fluid,int minimum,CompoundTag required,
                          ItemStack replacement,Map<String,Change> changes,float experience,int time){
        super(id,"",CookingBookCategory.MISC,ingredient,new ItemStack(PSItems.STONE_CUP.get()),experience,time);
        this.fluid=fluid;this.minimum=minimum;this.required=required.copy();this.replacement=replacement.copy();this.changes=Map.copyOf(changes);
    }
    @Override public boolean matches(Container inventory,Level level){return !assemble(inventory,RegistryAccess.EMPTY).isEmpty();}
    @Override public ItemStack assemble(Container inventory,RegistryAccess registries){
        ItemStack input=inventory.getItem(0);
        if(!ingredient.isEmpty()&&!ingredient.test(input))return ItemStack.EMPTY;
        var source=FluidVesselHandler.forTransfer(input);
        if(source==null)return ItemStack.EMPTY;
        FluidStack contents=FluidContents.normalize(source.getFluidInTank(0));
        if(contents.isEmpty()||contents.getFluid()!=fluid||contents.getAmount()<minimum||!NbtUtils.compareNbt(required,contents.getTag()==null?new CompoundTag():contents.getTag(),true))return ItemStack.EMPTY;
        FluidStack cooked=contents.copy();CompoundTag attributes=cooked.getOrCreateTag();
        changes.forEach((key,change)->{
            int value=change.apply(attributes.getInt(key));
            if(fluid==PSFluids.COFFEE.get()&&key.equals("warmth")){
                if(value<=0)attributes.remove(key);else attributes.putInt(key,Math.min(2,value));
            }else attributes.putInt(key,value);
        });
        cooked=FluidContents.normalize(cooked);
        if(replacement.isEmpty()&&cooked.isFluidEqual(contents))return ItemStack.EMPTY;
        var output=FluidVesselHandler.forCrafting(replacement.isEmpty()?input:replacement);
        // Native item identity changes must go through the container's own handler.
        return output==null?ItemStack.EMPTY:output.replaceContents(cooked);
    }
    @Override public ItemStack getResultItem(RegistryAccess registries){
        ItemStack sample=new ItemStack(PSItems.STONE_CUP.get());
        var vessel=new FluidVesselHandler(sample,FluidVessel.CUP);
        return assemble(new net.minecraft.world.SimpleContainer(vessel.replaceContents(new FluidStack(fluid,Math.max(minimum,250),required))),registries);
    }
    @Override public boolean isSpecial(){return true;}
    /** Full vessels and successive heating stages, without changing any registered recipe. */
    public List<ItemStack> getDisplayInputs(){
        List<ItemStack> result=new ArrayList<>();
        List<ItemStack> candidates=new ArrayList<>();
        PSItems.ITEMS.getEntries().forEach(item->candidates.add(new ItemStack(item.get())));
        for(var item:List.of(net.minecraft.world.item.Items.BUCKET,net.minecraft.world.item.Items.BOWL,net.minecraft.world.item.Items.GLASS_BOTTLE))candidates.add(new ItemStack(item));
        for(ItemStack candidate:candidates){
            if(!ingredient.isEmpty()&&!ingredient.test(candidate))continue;
            var handler=FluidVesselHandler.forCrafting(candidate);
            if(handler==null||handler.getTankCapacity(0)<minimum||!handler.canFillFluidType(new FluidStack(fluid,minimum,required)))continue;
            ItemStack input=handler.replaceContents(new FluidStack(fluid,handler.getTankCapacity(0),required));
            Set<String> seen=new HashSet<>();
            // Datapacks may define an unbounded increment: show representative stages only.
            while(seen.size()<32&&seen.add(input.save(new CompoundTag()).toString())){
                ItemStack output=assemble(new net.minecraft.world.SimpleContainer(input),RegistryAccess.EMPTY);
                if(output.isEmpty())break;
                result.add(input.copy());input=output;
            }
        }
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.HEATING.get();}

    private record Change(String type,int value){
        int apply(int current){return switch(type){case "set"->value;case "subtract"->current-value;case "multiply"->current*value;case "divide"->current/value;default->current+value;};}
        static Change read(JsonObject json){
            String type=GsonHelper.getAsString(json,"type","add").toLowerCase(Locale.ROOT);int value=GsonHelper.getAsInt(json,"value");
            if(!Set.of("set","subtract","multiply","divide","add").contains(type)||type.equals("divide")&&value==0)throw new JsonSyntaxException("Invalid furnace attribute operation");
            return new Change(type,value);
        }
    }
    public static final class Serializer implements RecipeSerializer<HeatingRecipe>{
        @Override public HeatingRecipe fromJson(ResourceLocation id,JsonObject json){
            JsonObject input=GsonHelper.getAsJsonObject(json,"input"),output=GsonHelper.getAsJsonObject(json,"result");
            Fluid fluid=PSFluids.resolve(GsonHelper.getAsString(input,"fluid"));
            if(fluid==null)throw new JsonSyntaxException("Unknown furnace fluid input");
            Ingredient ingredient=json.has("item")?Ingredient.fromJson(json.get("item")):Ingredient.EMPTY;
            int minimum=Math.max(1,GsonHelper.getAsInt(input,"level",1));
            CompoundTag required=new CompoundTag();
            if(input.has("attributes"))try{
                JsonElement tag=input.get("attributes");required=TagParser.parseTag(tag.isJsonPrimitive()?tag.getAsString():tag.toString());
            }catch(Exception error){throw new JsonSyntaxException("Invalid furnace fluid attributes",error);}
            ItemStack replacement=output.has("item")?CraftingHelper.getItemStack(output,true):ItemStack.EMPTY;
            Map<String,Change> changes=new LinkedHashMap<>();
            GsonHelper.getAsJsonObject(output,"attributes",new JsonObject()).entrySet().forEach(entry->{
                String key=fluid==PSFluids.COFFEE.get()&&entry.getKey().equals("temperature")?"warmth":entry.getKey();
                changes.put(key,Change.read(GsonHelper.convertToJsonObject(entry.getValue(),entry.getKey())));
            });
            int time=GsonHelper.getAsInt(json,"cookingtime",GsonHelper.getAsInt(json,"cookingTime",200));float experience=GsonHelper.getAsFloat(json,"experience",0);
            if(time<=0||time>Short.MAX_VALUE||!Float.isFinite(experience)||experience<0||replacement.getCount()>1)throw new JsonSyntaxException("Invalid furnace output, experience or cooking time");
            return new HeatingRecipe(id,ingredient,fluid,minimum,required,replacement,changes,experience,time);
        }
        @Override public HeatingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){
            Ingredient ingredient=Ingredient.fromNetwork(buffer);Fluid fluid=Objects.requireNonNull(PSFluids.resolve(buffer.readResourceLocation().toString()));int minimum=buffer.readVarInt();CompoundTag required=buffer.readNbt();ItemStack replacement=buffer.readItem();
            int count=buffer.readVarInt();Map<String,Change> changes=new LinkedHashMap<>();
            for(int i=0;i<count;i++){String key=buffer.readUtf();changes.put(key,new Change(buffer.readUtf(),buffer.readInt()));}
            return new HeatingRecipe(id,ingredient,fluid,minimum,required==null?new CompoundTag():required,replacement,changes,buffer.readFloat(),buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,HeatingRecipe recipe){
            recipe.ingredient.toNetwork(buffer);buffer.writeResourceLocation(new ResourceLocation(PSFluids.id(recipe.fluid)));buffer.writeVarInt(recipe.minimum);buffer.writeNbt(recipe.required);buffer.writeItem(recipe.replacement);
            buffer.writeVarInt(recipe.changes.size());recipe.changes.forEach((key,value)->{buffer.writeUtf(key);buffer.writeUtf(value.type);buffer.writeInt(value.value);});buffer.writeFloat(recipe.experience);buffer.writeVarInt(recipe.cookingTime);
        }
    }
}

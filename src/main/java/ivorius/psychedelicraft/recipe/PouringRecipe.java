package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public final class PouringRecipe extends CustomRecipe {
    public PouringRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    private record Pour(int sourceSlot,ItemStack source,ItemStack target){}
    private static Pour pour(CraftingContainer inventory){
        IFluidHandlerItem source=null,target=null;int sourceSlot=-1;
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            ItemStack input=inventory.getItem(slot);if(input.isEmpty())continue;IFluidHandlerItem vessel=FluidVesselHandler.forTransfer(input);
            if(vessel==null||target!=null)return null;if(source==null){source=vessel;sourceSlot=slot;}else target=vessel;
        }
        if(target==null||FluidUtil.tryFluidTransfer(target,source,Integer.MAX_VALUE,true).isEmpty())return null;
        return new Pour(sourceSlot,source.getContainer(),target.getContainer());
    }
    @Override public boolean matches(CraftingContainer inventory,Level level){return pour(inventory)!=null;}
    @Override public ItemStack assemble(CraftingContainer inventory,RegistryAccess registries){Pour result=pour(inventory);return result==null?ItemStack.EMPTY:result.target;}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory){NonNullList<ItemStack> remaining=NonNullList.withSize(inventory.getContainerSize(),ItemStack.EMPTY);Pour result=pour(inventory);if(result!=null)remaining.set(result.sourceSlot,result.source);return remaining;}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return PSRecipes.POURING.get();}
}

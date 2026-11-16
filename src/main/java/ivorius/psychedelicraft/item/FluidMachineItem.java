/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class FluidMachineItem extends BlockItem {
    private final int capacity;
    public FluidMachineItem(Block block,int capacity){super(block,new Item.Properties().stacksTo(16));this.capacity=capacity;}
    public int getCapacity(){return capacity;}
    public FluidStack contents(ItemStack stack){return stack.isEmpty()||stack.getTag()==null?FluidStack.EMPTY:FluidContents.read(stack.getTag().getCompound("fluid"),capacity);}
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,@Nullable CompoundTag tag){return new FluidVesselHandler(stack,capacity);}
    @Override public int getMaxStackSize(ItemStack stack){return contents(stack).isEmpty()?16:1;}
    @Override public boolean isBarVisible(ItemStack stack){return !contents(stack).isEmpty();}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13*contents(stack).getAmount()/(float)capacity);}
    @Override public int getBarColor(ItemStack stack){return 0xaaaaff;}
    @Override public Component getName(ItemStack stack){var contents=contents(stack);return contents.isEmpty()?super.getName(stack):Component.translatable(getDescriptionId()+".filled",PSFluids.displayName(contents));}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){
        var contents=contents(stack);if(!contents.isEmpty()||flag.isAdvanced())tooltip.add(Component.translatable("psychedelicraft.container.levels",contents.getAmount(),capacity).withStyle(ChatFormatting.GRAY));
        if(PSFluids.brew(contents.getFluid())!=null)BrewProfile.appendAttributes(contents,tooltip);
    }
}

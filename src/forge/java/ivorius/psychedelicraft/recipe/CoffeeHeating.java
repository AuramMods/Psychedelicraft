/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.FluidMachineItem;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public final class CoffeeHeating {
    private CoffeeHeating() {
    }

    // A null result delegates to the native furnace registry.
    @Nullable
    public static ItemStack getResult(ItemStack input) {
        FluidStack coffee = coffee(input);
        if (coffee == null || PSFluids.warmth(coffee) >= 2) return null;
        if (coffee.tag == null) coffee.tag = new NBTTagCompound();
        coffee.tag.setInteger("warmth", PSFluids.warmth(coffee) + 1);
        ItemStack output = input.copy();
        output.setCount(1);
        output.setTagInfo("fluid", FluidContents.write(coffee));
        return output;
    }

    public static float experience(ItemStack output) {
        FluidStack coffee = coffee(output);
        return coffee != null && PSFluids.warmth(coffee) > 0 ? 0.2F : -1;
    }

    @Nullable
    private static FluidStack coffee(ItemStack stack) {
        FluidStack contents;
        if (stack.getItem() instanceof PortableFluidItem) {
            contents = ((PortableFluidItem) stack.getItem()).contents(stack);
        } else if (stack.getItem() instanceof FluidMachineItem) {
            contents = ((FluidMachineItem) stack.getItem()).contents(stack);
        } else {
            return null;
        }
        return contents != null && contents.getFluid() == PSFluids.COFFEE ? contents : null;
    }
}

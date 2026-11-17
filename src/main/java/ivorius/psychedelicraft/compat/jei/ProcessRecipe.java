/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.List;

/** Browser information only. Processing continues to use the native machine logic. */
record ProcessRecipe(List<List<ItemStack>> itemsIn, List<ItemStack> itemsOut,
                     List<FluidStack> fluidsIn, List<FluidStack> fluidsOut, Component detail) {}

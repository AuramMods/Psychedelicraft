/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public final class FluidStackIngredient extends Ingredient {
    private final Ingredient vessel;
    private final Fluid fluid;

    private FluidStackIngredient(Ingredient vessel, Fluid fluid) {
        super(Arrays.stream(vessel.getMatchingStacks())
                .map(FluidVesselHandler::forCrafting)
                .filter(Objects::nonNull)
                .map(handler -> handler.craftContents(fluid, -1, null))
                .toArray(ItemStack[]::new));
        this.vessel = vessel;
        this.fluid = fluid;
    }

    @Override
    public boolean apply(@Nullable ItemStack stack) {
        if (stack == null || !vessel.apply(stack)) return false;
        FluidVesselHandler handler = FluidVesselHandler.forCrafting(stack);
        FluidStack contents = handler == null ? null : handler.getFluid();
        return contents != null && contents.getFluid() == fluid;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    static Fluid readFluid(JsonObject json) {
        String name = JsonUtils.getString(json, "fluid");
        Fluid fluid = PSFluids.resolve(name);
        if (fluid == null) throw new JsonSyntaxException("Unknown fluid " + name);
        return fluid;
    }

    public static final class Factory implements IIngredientFactory {
        @Override
        public Ingredient parse(JsonContext context, JsonObject json) {
            return new FluidStackIngredient(CraftingHelper.getIngredient(json.get("vessel"), context), readFluid(json));
        }
    }
}

/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

/** Fluid identity/appearance data, independent of Forge source and flowing registrations. */
public class FluidProfile {
    public final String path;
    public final int viscosity;
    private final String appearance;
    private final int color;
    public FluidProfile(String path,String appearance,int color,int viscosity){this.path=path;this.appearance=appearance;this.color=color;this.viscosity=viscosity;}
    public Component getDisplayName(FluidStack stack){return Component.translatable("fluid.psychedelicraft."+path);}
    public int getColor(FluidStack stack){return color;}
    public ResourceLocation getStill(FluidStack stack){return Psychedelicraft.id("block/fluid/"+appearance+"_still");}
    public ResourceLocation getFlowing(FluidStack stack){return Psychedelicraft.id("block/fluid/"+appearance+"_flow");}
    public ResourceLocation getSymbol(FluidStack stack){return Psychedelicraft.id("fluid/"+path);}
}

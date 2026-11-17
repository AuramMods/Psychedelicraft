/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.fluid.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.function.Consumer;

/** The accepted catalog, using native creative entries and independent NBT copies. */
public final class VesselCreativeItems {
    private VesselCreativeItems(){}
    public static void add(PortableFluidItem item,Consumer<ItemStack> output){
        if(item instanceof MolotovCocktailItem&&ivorius.psychedelicraft.config.PSConfig.DISABLE_MOLOTOVS.get())return;
        FluidVessel vessel=item.getVessel();
        output.accept(new ItemStack(vessel.emptyItem()==null?item:vessel.emptyItem()));
        Consumer<FluidStack> filled=fluid->{
            ItemStack stack=new ItemStack(item);
            stack.getOrCreateTag().put(FluidVesselHandler.CONTENTS,FluidContents.write(fluid));
            output.accept(stack);
        };
        if(vessel==FluidVessel.SYRINGE){
            for(var fluid:java.util.List.of(PSFluids.COCAINE,PSFluids.CAFFEINE,PSFluids.BATH_SALTS))filled.accept(new FluidStack(fluid.get(),vessel.capacity));
            CompoundTag extract=new CompoundTag();extract.putInt(BrewProfile.DISTILLATION,2);
            filled.accept(new FluidStack(PSFluids.MORNING_GLORY_EXTRACT.get(),vessel.capacity,extract));
        }else if(vessel==FluidVessel.SHOT_GLASS){
            ((BrewProfile)PSFluids.AGAVE.profile).addCreativeVariants(vessel.capacity,true,filled);
        }else for(var fluid:PSFluids.all()){
            if(fluid==PSFluids.COFFEE){
                if(vessel==FluidVessel.CUP)for(int warmth=0;warmth<=2;warmth++){
                    FluidStack coffee=new FluidStack(fluid.get(),vessel.capacity);
                    if(warmth>0)coffee.getOrCreateTag().putInt("warmth",warmth);
                    filled.accept(coffee);
                }
            }else if(fluid.profile instanceof BrewProfile brew){
                if(vessel!=FluidVessel.CUP)brew.addCreativeVariants(vessel.capacity,false,filled);
            }else if(fluid==PSFluids.SLURRY||(vessel!=FluidVessel.MUG&&!PSFluids.isInjectable(fluid.get())))filled.accept(new FluidStack(fluid.get(),vessel.capacity));
        }
    }
}

package ivorius.psychedelicraft.fluid;

import net.minecraftforge.fluids.ForgeFlowingFluid;

/** Native Forge flow behavior; physical block/attribute integration is ported separately. */
public final class WorldFluid extends ForgeFlowingFluid.Source {
    public final PSFluids.Family family;
    public WorldFluid(PSFluids.Family family){super(family.properties());this.family=family;}
}

package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.block.MashTubBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public final class FluidBodyContact {
    private FluidBodyContact() {}

    public static boolean inWater(World world, AxisAlignedBB box, Material material, Entity entity) {
        return world.handleMaterialAcceleration(box, material, entity) || MashTubBlock.containsBody(world, entity, material);
    }

    public static boolean inLava(World world, AxisAlignedBB box, Material material, Entity entity) {
        return world.isMaterialInBB(box, material) || MashTubBlock.containsBody(world, entity, material);
    }
}

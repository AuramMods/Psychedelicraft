package ivorius.psychedelicraft.fluid.physical;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class WorldFluid extends Fluid {
    public WorldFluid(String name, ResourceLocation still, ResourceLocation flowing, int color) {
        super(name, still, flowing, color);
    }

    @Override
    public boolean doesVaporize(FluidStack stack) {
        return true;
    }

    @Override
    public void vaporize(@Nullable EntityPlayer player, World world, BlockPos pos, FluidStack stack) {
        world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + 0.5,
                    pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.35, 0.35, 0.35, 0);
        }
    }
}

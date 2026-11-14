package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.LatticeCropBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class LatticePiston {
    private static final String AGE = "psychedelicraft:lattice_age";
    private static final String PERSISTENT = "psychedelicraft:lattice_persistent";

    private LatticePiston() {
    }

    public static NBTTagCompound write(NBTTagCompound tag, IBlockState moving) {
        if (moving.getBlock() instanceof LatticeCropBlock) {
            // The moving state already contains the actual crop properties, beyond its four metadata bits.
            tag.setInteger(AGE, moving.getValue(LatticeCropBlock.AGE));
            tag.setBoolean(PERSISTENT, moving.getValue(LatticeCropBlock.PERSISTENT));
        }
        return tag;
    }

    public static IBlockState read(IBlockState moving, NBTTagCompound tag) {
        if (!(moving.getBlock() instanceof LatticeCropBlock)) return moving;
        int age = MathHelper.clamp(tag.getInteger(AGE), 0, LatticeCropBlock.MAX_AGE);
        return moving.withProperty(LatticeCropBlock.AGE, age)
                .withProperty(LatticeCropBlock.PERSISTENT, tag.getBoolean(PERSISTENT));
    }

    public static void restoreGrowth(TileEntityPiston piston) {
        IBlockState moving = piston.getPistonState();
        if (!(moving.getBlock() instanceof LatticeCropBlock)) return;
        World world = piston.getWorld();
        BlockPos pos = piston.getPos();
        if (world.getBlockState(pos).getBlock() != moving.getBlock()) return;
        LatticeCropBlockEntity crop = LatticeCropBlock.getCrop(world, pos);
        if (crop != null) crop.setGrowth(moving.getValue(LatticeCropBlock.AGE), moving.getValue(LatticeCropBlock.PERSISTENT));
    }
}

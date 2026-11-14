package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

final class OceanTemplateWater {
    private static final EnumFacing[] SOURCE_DIRECTIONS = {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST,
            EnumFacing.SOUTH, EnumFacing.WEST};
    private final World world;
    private final Map<BlockPos, IBlockState> source = new LinkedHashMap<>();
    private final Map<BlockPos, IBlockState> flowing = new LinkedHashMap<>();

    OceanTemplateWater(World world) {
        this.world = world;
    }

    void remember(BlockPos pos, Template.BlockInfo info) {
        if (!Waterlogging.supportsOceanBlock(info.blockState)) return;
        IBlockState replaced = world.getBlockState(pos);
        if (Waterlogging.isWet(world, pos, replaced) || Waterlogging.isWaterSource(replaced)) {
            source.put(pos, info.blockState);
        } else if (Waterlogging.isWater(world, pos, replaced)) {
            flowing.put(pos, info.blockState);
        }
    }

    void restore() {
        source.forEach(this::fillIfPlaced);
        boolean changed;
        do {
            changed = false;
            Iterator<Map.Entry<BlockPos, IBlockState>> iterator = flowing.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, IBlockState> entry = iterator.next();
                if (nearSource(entry.getKey())) {
                    changed |= fillIfPlaced(entry.getKey(), entry.getValue());
                    iterator.remove();
                }
            }
        } while (changed && !flowing.isEmpty());
    }

    private boolean fillIfPlaced(BlockPos pos, IBlockState intended) {
        if (world.getBlockState(pos).getBlock() != intended.getBlock()) return false;
        Waterlogging.fillSource(world, pos);
        return Waterlogging.isWet(world, pos);
    }

    private boolean nearSource(BlockPos pos) {
        for (EnumFacing direction : SOURCE_DIRECTIONS) {
            BlockPos neighbor = pos.offset(direction);
            if (world.isBlockLoaded(neighbor)
                    && (Waterlogging.isWet(world, neighbor) || Waterlogging.isWaterSource(world.getBlockState(neighbor)))) {
                return true;
            }
        }
        return false;
    }
}

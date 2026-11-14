package ivorius.psychedelicraft.client.render.block;

import net.minecraft.util.math.BlockPos;

final class BlockRenderSeed {
    private BlockRenderSeed() {
    }

    static long pack(BlockPos pos) {
        // The 1.20.1 renderers seed their positions with X/Z/Y, where 1.12 uses X/Y/Z.
        return ((long) pos.getX() & 0x3ffffffL) << 38
                | ((long) pos.getZ() & 0x3ffffffL) << 12
                | ((long) pos.getY() & 0xfffL);
    }
}

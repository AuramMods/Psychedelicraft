package ivorius.psychedelicraft.client.render.block;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BlockRenderSeedTest {
    @Test
    public void packsCoordinatesInDonorOrder() {
        assertEquals(0xffffff800000203dL, BlockRenderSeed.pack(new BlockPos(-2, 61, 2)));
        assertEquals(0x000003ffffff03dL, BlockRenderSeed.pack(new BlockPos(0, 61, -1)));
        assertEquals(0x7270e02363c800ffL, BlockRenderSeed.pack(new BlockPos(30_000_000, 255, -30_000_000)));
    }
}

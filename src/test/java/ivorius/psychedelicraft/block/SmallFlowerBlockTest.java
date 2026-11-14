package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmallFlowerBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void onlyWitherRosesAcceptNetherSoil() {
        SmallFlowerBlock flower = new SmallFlowerBlock();
        WitherRoseBlock rose = PSBlocks.WITHER_ROSE;
        for (Block soil : new Block[]{Blocks.NETHERRACK, Blocks.SOUL_SAND, PSBlocks.SOUL_SOIL}) {
            IBlockState state = soil.getDefaultState();
            assertTrue(rose.canSustainBush(state));
            assertFalse(flower.canSustainBush(state));
        }
        assertTrue(rose.canSustainBush(Blocks.DIRT.getDefaultState()));
        assertFalse(rose.canSustainBush(Blocks.STONE.getDefaultState()));
    }
}

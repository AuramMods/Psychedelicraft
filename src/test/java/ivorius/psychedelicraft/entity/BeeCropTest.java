package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class BeeCropTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void eligibleCropsAdvanceExactlyOneAgeUntilMature() {
        for (Block block : new Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
                Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, PSBlocks.SWEET_BERRY_BUSH}) {
            int max = block == PSBlocks.SWEET_BERRY_BUSH ? PSBlocks.SWEET_BERRY_BUSH.maxAge()
                    : block instanceof BlockCrops ? ((BlockCrops) block).getMaxAge() : 7;
            for (int age = 0; age < max; age++) {
                assertSame(block.getStateFromMeta(age + 1), BeeEntity.nextCropAge(block.getStateFromMeta(age)));
            }
            IBlockState mature = block.getStateFromMeta(max);
            assertSame(mature, BeeEntity.nextCropAge(mature));
        }
    }

    @Test
    public void otherGrowableBlocksAreNotPollinated() {
        for (Block block : new Block[]{Blocks.SAPLING, Blocks.NETHER_WART, Blocks.COCOA, Blocks.REEDS,
                PSBlocks.CANNABIS, PSBlocks.WINE_GRAPE_LATTICE, PSBlocks.FRUITING_JUNIPER_LEAVES, PSBlocks.PITCHER_CROP}) {
            IBlockState state = block.getDefaultState();
            assertSame(state, BeeEntity.nextCropAge(state));
        }
    }

    @Test
    public void torchflowerRipensIntoItsSeparateFlowerBlock() {
        assertSame(PSBlocks.TORCHFLOWER_CROP.getStateFromMeta(1),
                BeeEntity.nextCropAge(PSBlocks.TORCHFLOWER_CROP.getStateFromMeta(0)));
        assertSame(PSBlocks.TORCHFLOWER.getDefaultState(),
                BeeEntity.nextCropAge(PSBlocks.TORCHFLOWER_CROP.getStateFromMeta(1)));
    }
}

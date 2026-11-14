package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class SnifferCropsTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void bothCropsAndThePitcherFlowerKeepTheirMetadata() {
        for (Block block : new Block[]{PSBlocks.TORCHFLOWER_CROP, PSBlocks.PITCHER_CROP, PSBlocks.PITCHER_PLANT}) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                assertSame(state, block.getStateFromMeta(block.getMetaFromState(state)));
            }
        }
    }

    @Test
    public void pitcherCropDropsOnlyFromItsLowerHalfAndBothSeedsUseFarmland() {
        for (int age = 0; age <= 4; age++) {
            IBlockState bottom = PSBlocks.PITCHER_CROP.getStateFromMeta(age);
            IBlockState top = bottom.withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.UPPER);
            assertSame(Items.AIR, PSBlocks.PITCHER_CROP.getItemDropped(top, null, 3));
            assertSame(age == 4 ? PSItems.PITCHER_PLANT : PSItems.PITCHER_POD,
                    PSBlocks.PITCHER_CROP.getItemDropped(bottom, null, 3));
        }
        assertSame(EnumPlantType.Crop, ((IPlantable) PSItems.TORCHFLOWER_SEEDS).getPlantType(null, BlockPos.ORIGIN));
        assertSame(EnumPlantType.Crop, ((IPlantable) PSItems.PITCHER_POD).getPlantType(null, BlockPos.ORIGIN));
    }

    @Test
    public void ripePitcherKeepsItsStateWithoutBecomingAForgeFarmerHarvestTarget() {
        assertFalse(PSBlocks.PITCHER_CROP instanceof BlockCrops);
        assertSame(PSBlocks.PITCHER_CROP, PSBlocks.PITCHER_CROP.getStateFromMeta(4).getBlock());
    }
}

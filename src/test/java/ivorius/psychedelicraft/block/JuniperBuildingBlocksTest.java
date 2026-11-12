package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class JuniperBuildingBlocksTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void buttonsUseAllSixteenMetadataValues() {
        JuniperButtonBlock button = new JuniperButtonBlock();
        for (int meta = 0; meta < 16; meta++) {
            assertEquals(meta, button.getMetaFromState(button.getStateFromMeta(meta)));
        }
    }

    @Test
    public void rotatingAHorizontalButtonChangesItsOutlineAndKeepsItsAttachment() {
        JuniperButtonBlock button = new JuniperButtonBlock();
        IBlockState floor = button.getStateFromMeta(5);
        IBlockState turned = floor.withRotation(Rotation.CLOCKWISE_90);
        assertEquals(7, button.getMetaFromState(turned));
        assertEquals(EnumFacing.UP, turned.getValue(BlockDirectional.FACING));

        AxisAlignedBB oldBox = button.getBoundingBox(floor, null, BlockPos.ORIGIN);
        AxisAlignedBB newBox = button.getBoundingBox(turned, null, BlockPos.ORIGIN);
        assertEquals(oldBox.minX, newBox.minZ, 0);
        assertEquals(oldBox.maxX, newBox.maxZ, 0);
        assertEquals(oldBox.minZ, newBox.minX, 0);
        assertEquals(oldBox.maxZ, newBox.maxX, 0);
    }

    @Test
    public void singleSlabHalvesAndDoubleSlabRoundTripSeparately() {
        JuniperSlabBlock slab = new JuniperSlabBlock.Single();
        for (BlockSlab.EnumBlockHalf half : BlockSlab.EnumBlockHalf.values()) {
            IBlockState state = slab.getDefaultState().withProperty(BlockSlab.HALF, half);
            assertSame(state, slab.getStateFromMeta(slab.getMetaFromState(state)));
        }
        JuniperSlabBlock doubleSlab = new JuniperSlabBlock.Double();
        assertSame(doubleSlab.getDefaultState(), doubleSlab.getStateFromMeta(0));
        assertEquals(2, doubleSlab.quantityDropped(null));
    }
}

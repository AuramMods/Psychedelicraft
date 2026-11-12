package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.RiftJarItem;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class RiftJarTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void allFacingsRoundTripAndRotate() {
        RiftJarBlock jar = new RiftJarBlock();
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            IBlockState state = jar.getDefaultState().withProperty(RiftJarBlock.FACING, facing);
            assertSame(state, jar.getStateFromMeta(jar.getMetaFromState(state)));
            assertSame(facing.rotateY(), jar.withRotation(state, Rotation.CLOCKWISE_90).getValue(RiftJarBlock.FACING));
            assertEquals(EnumPushReaction.DESTROY, jar.getMobilityFlag(state));
        }
    }

    @Test
    public void filledItemsKeepExactAmountsAndEmptyItemsStackNormally() {
        assertFalse(PSItems.RIFT_JAR.filled(0).hasTagCompound());
        ItemStack filled = PSItems.RIFT_JAR.filled(0.77425F);
        assertEquals(0.77425F, RiftJarItem.getRiftFraction(filled.copy()), 0);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("riftFraction", Float.NaN);
        filled.setTagCompound(tag);
        RiftJarBlockEntity jar = new RiftJarBlockEntity();
        jar.setAmount(RiftJarItem.getRiftFraction(filled));
        assertEquals(0, jar.getAmount(), 0);
    }
}

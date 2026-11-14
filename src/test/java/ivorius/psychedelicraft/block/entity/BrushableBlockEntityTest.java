package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.SuspiciousSandBlock;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BrushableBlockEntityTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        TileEntity.register("psychedelicraft:brushable_block", BrushableBlockEntity.class);
    }

    @Test
    public void dustThresholdsRoundTripThroughLegacyMetadata() {
        int[] stages = {0, 1, 1, 2, 2, 2, 3, 3, 3, 3};
        for (int strokes = 0; strokes < stages.length; strokes++) {
            int dust = BrushableBlockEntity.dustStage(strokes);
            assertEquals(stages[strokes], dust);
            assertEquals(dust, PSBlocks.SUSPICIOUS_SAND.getMetaFromState(
                    PSBlocks.SUSPICIOUS_SAND.getDefaultState().withProperty(SuspiciousSandBlock.DUSTED, dust)));
            assertEquals(dust, PSBlocks.SUSPICIOUS_SAND.getStateFromMeta(dust).getValue(SuspiciousSandBlock.DUSTED).intValue());
        }
    }

    @Test
    public void syncRevealsOnlyTheExposedItemAndKeepsItsTags() {
        BrushableBlockEntity tile = new BrushableBlockEntity();
        NBTTagCompound save = new NBTTagCompound();
        save.setInteger("Strokes", 4);
        save.setLong("NextStroke", 123);
        save.setLong("ResetAt", 153);
        save.setByte("BrushFace", (byte) EnumFacing.EAST.getIndex());
        save.setTag("Item", new ItemStack(Items.EMERALD).setStackDisplayName("Buried").writeToNBT(new NBTTagCompound()));
        tile.readFromNBT(save);
        assertEquals("Buried", tile.getExposedItem().getDisplayName());
        assertEquals(EnumFacing.EAST, tile.getBrushFace());
        assertEquals(153, tile.writeToNBT(new NBTTagCompound()).getLong("ResetAt"));
        assertFalse(tile.getUpdateTag().hasKey("Strokes"));
        tile.setLootTable(Psychedelicraft.id("archaeology/ocean_ruin_warm"), 17);
        assertEquals(17, tile.writeToNBT(new NBTTagCompound()).getLong("LootTableSeed"));
        assertFalse(tile.getUpdateTag().hasKey("LootTable"));
        assertFalse(tile.getUpdateTag().hasKey("Item"));
    }
}

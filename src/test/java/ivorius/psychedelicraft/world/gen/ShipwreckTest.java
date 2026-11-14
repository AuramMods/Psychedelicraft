package ivorius.psychedelicraft.world.gen;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShipwreckTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void regionSpacingLeavesFourChunksBetweenCandidateBands() {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                ChunkPos candidate = Shipwreck.candidate(23842L, x, z);
                assertEquals(x, Math.floorDiv(candidate.x, 24));
                assertEquals(z, Math.floorDiv(candidate.z, 24));
                assertTrue(Math.floorMod(candidate.x, 24) < 20);
                assertTrue(Math.floorMod(candidate.z, 24) < 20);
            }
        }
    }

    @Test
    public void eachWoodPalettePreservesWreckSizeAndLootMarkers() {
        for (int palette = 0; palette < 8; palette++) {
            assertLayout("with_mast", palette, new BlockPos(9, 21, 28), 3, true);
            assertLayout("rightsideup_fronthalf_degraded", palette, new BlockPos(9, 9, 24), 1, true);
            assertLayout("upsidedown_backhalf", palette, new BlockPos(9, 9, 16), 2, false);
        }
    }

    private static void assertLayout(String name, int palette, BlockPos size, int count, boolean supply) {
        Template template = ShipwreckPiece.loadTemplate(name, palette);
        assertEquals(size, template.getSize());
        Collection<String> markers = template.getDataBlocks(BlockPos.ORIGIN, new PlacementSettings()).values();
        assertEquals(count, markers.size());
        assertTrue(ShipwreckPiece.LOOT.keySet().containsAll(markers));
        assertEquals(supply, markers.contains("supply_chest"));
    }
}

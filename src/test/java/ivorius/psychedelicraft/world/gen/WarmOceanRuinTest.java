package ivorius.psychedelicraft.world.gen;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WarmOceanRuinTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void reservedGapIsPreservedInNegativeAndPositiveRegions() {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                ChunkPos chunk = WarmOceanRuin.candidate(23842L, x, z);
                assertEquals(x, Math.floorDiv(chunk.x, 20));
                assertEquals(z, Math.floorDiv(chunk.z, 20));
                assertTrue(Math.floorMod(chunk.x, 20) < 12);
                assertTrue(Math.floorMod(chunk.z, 20) < 12);
            }
        }
    }

    @Test
    public void aReloadDoesNotRepopulateASectionWhoseLootHasBeenDisturbed() {
        WarmOceanRuin.Start start = new WarmOceanRuin.Start();
        ChunkPos populated = new ChunkPos(-9, 5);
        start.notifyPostProcessAt(populated);
        NBTTagCompound tag = new NBTTagCompound();
        start.writeToNBT(tag);

        WarmOceanRuin.Start reopened = new WarmOceanRuin.Start();
        reopened.readFromNBT(tag);
        assertFalse(reopened.isValidForPostProcess(populated));
        assertTrue(reopened.isValidForPostProcess(new ChunkPos(-8, 5)));
    }

    @Test
    public void allTwelveTemplatesKeepTheirFootprintsAndMarkers() throws IOException {
        int[] smallGuardians = {0, 2, 1, 1, 1, 0, 0, 1};
        int[] largeGuardians = {2, 4, 2, 2};
        for (int i = 1; i <= 8; i++) assertTemplate("warm_" + i, new BlockPos(6, 7, 7), smallGuardians[i - 1]);
        for (int i = 4; i <= 7; i++) assertTemplate("big_warm_" + i, new BlockPos(16, 16, 16), largeGuardians[i - 4]);
    }

    private static void assertTemplate(String name, BlockPos size, int guardians) throws IOException {
        String path = "/assets/psychedelicraft/structures/underwater_ruin/" + name + ".nbt";
        Template template = new Template();
        try (InputStream stream = WarmOceanRuinTest.class.getResourceAsStream(path)) {
            assertNotNull(path, stream);
            template.read(CompressedStreamTools.readCompressed(stream));
        }
        assertEquals(size, template.getSize());
        Collection<String> markers = template.getDataBlocks(BlockPos.ORIGIN, new PlacementSettings()).values();
        assertEquals(name, 1, Collections.frequency(markers, "chest"));
        assertEquals(name, guardians, Collections.frequency(markers, "drowned"));
    }
}

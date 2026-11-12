package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.world.gen.VillageGarden.Plot;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VillageGardenTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void everyRotationFacesTheRoadAndStaysWithinItsPieceBounds() {
        for (Plot plot : Plot.values()) {
            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                StructureBoundingBox bounds = plot.bounds(-17, 64, 31, facing);
                PlacementSettings placement = new PlacementSettings().setRotation(plot.rotation(facing));
                BlockPos origin = plot.origin(bounds, placement);
                BlockPos corner = new BlockPos(plot.width - 1, 8, plot.depth - 1);
                assertTrue(bounds.isVecInside(origin));
                assertTrue(bounds.isVecInside(origin.add(Template.transformedBlockPos(placement, corner))));

                BlockPos entrance = plot == Plot.LATTICE_WALL ? new BlockPos(6, 1, 6) : new BlockPos(4, 2, 0);
                BlockPos entranceInWorld = origin.add(Template.transformedBlockPos(placement, entrance));
                assertTrue(bounds.isVecInside(entranceInWorld));
                assertFalse(bounds.isVecInside(entranceInWorld.offset(facing.getOpposite())));
                assertEquals(64 + plot.entranceHeight, entranceInWorld.getY());
            }
        }
    }

    @Test
    public void packagedTemplatesUseValidLegacyVanillaPaletteProperties() throws IOException {
        for (Plot plot : Plot.values()) {
            NBTTagCompound data = read(plot);
            assertEquals(1343, data.getInteger("DataVersion"));
            NBTTagList size = data.getTagList("size", 3);
            assertEquals(plot.width, size.getIntAt(0));
            assertEquals(9, size.getIntAt(1));
            assertEquals(plot.depth, size.getIntAt(2));
            NBTTagList palette = data.getTagList("palette", 10);
            for (int index = 0; index < palette.tagCount(); index++) {
                NBTTagCompound state = palette.getCompoundTagAt(index);
                String id = state.getString("Name");
                if (id.startsWith("minecraft:")) {
                    Block block = Block.getBlockFromName(id);
                    assertNotNull(id, block);
                    NBTTagCompound properties = state.getCompoundTag("Properties");
                    for (String name : properties.getKeySet()) {
                        IProperty<?> property = block.getBlockState().getProperty(name);
                        assertNotNull(id + "." + name, property);
                        assertTrue(id + "." + name, property.parseValue(properties.getString(name)).isPresent());
                    }
                }
            }
        }
    }

    @Test
    public void vineyardRetainsTheDonorChestAndMatureCropData() throws IOException {
        NBTTagList blocks = read(Plot.VINEYARD).getTagList("blocks", 10);
        int mature = 0;
        int chests = 0;
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound tile = blocks.getCompoundTagAt(index).getCompoundTag("nbt");
            if ("psychedelicraft:lattice_crop".equals(tile.getString("id")) && tile.getInteger("age") == 3) {
                assertFalse(tile.getBoolean("persistent"));
                mature++;
            }
            if ("minecraft:chest".equals(tile.getString("id"))) {
                NBTTagList items = tile.getTagList("Items", 10);
                assertEquals(3, items.tagCount());
                assertEquals("psychedelicraft:wine_grapes", items.getCompoundTagAt(0).getString("id"));
                assertEquals(1, items.getCompoundTagAt(0).getByte("Count"));
                assertEquals("psychedelicraft:juniper_berries", items.getCompoundTagAt(1).getString("id"));
                assertEquals(30, items.getCompoundTagAt(1).getByte("Count"));
                assertEquals("psychedelicraft:cannabis_seeds", items.getCompoundTagAt(2).getString("id"));
                assertEquals(3, items.getCompoundTagAt(2).getByte("Count"));
                chests++;
            }
        }
        assertEquals(19, mature);
        assertEquals(1, chests);
    }

    private static NBTTagCompound read(Plot plot) throws IOException {
        String path = "/assets/" + plot.template.getResourceDomain() + "/structures/" + plot.template.getResourcePath() + ".nbt";
        try (InputStream input = VillageGardenTest.class.getResourceAsStream(path)) {
            assertNotNull(path, input);
            return CompressedStreamTools.readCompressed(input);
        }
    }
}

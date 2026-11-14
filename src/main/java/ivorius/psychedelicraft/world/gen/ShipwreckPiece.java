package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.common.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class ShipwreckPiece extends StructureComponent {
    private static final BlockPos PIVOT = new BlockPos(4, 0, 15);
    private static final Map<String, NBTTagCompound> LAYOUTS = new HashMap<>();
    static final Map<String, ResourceLocation> LOOT;

    static {
        Map<String, ResourceLocation> tables = new HashMap<>();
        for (String type : new String[]{"supply", "map", "treasure"}) {
            tables.put(type + "_chest", Psychedelicraft.id("chests/shipwreck_" + type));
        }
        LOOT = Collections.unmodifiableMap(tables);
    }

    private BlockPos anchor;
    private Rotation rotation;
    private String layout;
    private int palette;

    public ShipwreckPiece() {
    }

    ShipwreckPiece(World world, BlockPos anchor, Rotation rotation, boolean beach, String layout, int palette, int burial) {
        super(0);
        this.rotation = rotation;
        this.layout = layout;
        this.palette = palette;
        BlockPos size = template().getSize();
        this.anchor = anchor.up(placementHeight((WorldServer) world, anchor, size, beach, burial));
        PlacementSettings placement = new PlacementSettings().setRotation(rotation);
        BlockPos origin = origin(placement);
        BlockPos far = origin.add(Template.transformedBlockPos(placement, size.add(-1, -1, -1)));
        boundingBox = StructureBoundingBox.createProper(origin.getX(), origin.getY(), origin.getZ(), far.getX(), far.getY(), far.getZ());
    }

    private BlockPos origin(PlacementSettings placement) {
        return anchor.add(PIVOT).subtract(Template.transformedBlockPos(placement, PIVOT));
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        tag.setLong("Anchor", anchor.toLong());
        tag.setString("Rotation", rotation.name());
        tag.setString("Layout", layout);
        tag.setInteger("Palette", palette);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager templates) {
        anchor = BlockPos.fromLong(tag.getLong("Anchor"));
        rotation = Rotation.valueOf(tag.getString("Rotation"));
        layout = tag.getString("Layout");
        palette = tag.getInteger("Palette");
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox area) {
        Template template = template();
        PlacementSettings placement = new PlacementSettings().setRotation(rotation).setIgnoreEntities(true)
                .setIgnoreStructureBlock(true).setReplacedBlock(Blocks.AIR).setBoundingBox(area);
        BlockPos origin = origin(placement);
        template.addBlocksToWorld(world, origin, placement, 2);
        template.getDataBlocks(origin, placement).forEach((marker, name) -> {
            ResourceLocation table = LOOT.get(name);
            if (table == null) return;
            BlockPos pos = marker.down();
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityChest) {
                long seed = world.getSeed() ^ MathHelper.getCoordinateRandom(pos.getX(), pos.getY(), pos.getZ());
                ((TileEntityChest) tile).setLootTable(table, seed == 0L ? 1L : seed);
                tile.markDirty();
            }
        });
        return true;
    }

    private Template template() {
        return loadTemplate(layout, palette);
    }

    static Template loadTemplate(String name, int palette) {
        NBTTagCompound raw;
        synchronized (LAYOUTS) {
            raw = LAYOUTS.computeIfAbsent(name, ShipwreckPiece::readLayout);
        }
        NBTTagCompound selected = new NBTTagCompound();
        selected.setTag("size", raw.getTag("size"));
        selected.setTag("blocks", raw.getTag("blocks"));
        selected.setString("author", raw.getString("author"));
        selected.setTag("palette", raw.getTagList("palettes", Constants.NBT.TAG_LIST).get(palette));
        Template template = new Template();
        template.read(selected);
        return template;
    }

    private static NBTTagCompound readLayout(String name) {
        String path = "/assets/psychedelicraft/structures/shipwreck/" + name + ".nbt";
        try (InputStream stream = ShipwreckPiece.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing shipwreck template " + name);
            return CompressedStreamTools.readCompressed(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read shipwreck template " + name, exception);
        }
    }

    private static int placementHeight(WorldServer world, BlockPos origin, BlockPos size, boolean beach, int burial) {
        ChunkGeneratorOverworld generator = (ChunkGeneratorOverworld) world.getChunkProvider().chunkGenerator;
        Map<Long, ChunkPrimer> terrain = new HashMap<>();
        int min = world.getHeight();
        int total = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                int px = origin.getX() + x;
                int pz = origin.getZ() + z;
                int cx = px >> 4;
                int cz = pz >> 4;
                ChunkPrimer column = terrain.computeIfAbsent(ChunkPos.asLong(cx, cz), key -> {
                    ChunkPrimer primer = new ChunkPrimer();
                    generator.setBlocksInChunk(cx, cz, primer);
                    return primer;
                });
                int height = column.findGroundBlockIdx(px & 15, pz & 15) + 1;
                if (!beach) {
                    while (height > 1 && !column.getBlockState(px & 15, height - 1, pz & 15).getMaterial().blocksMovement()) height--;
                }
                if (beach) height = Math.max(world.getSeaLevel(), height);
                min = Math.min(min, height);
                total += height;
            }
        }
        return beach ? min - size.getY() / 2 - burial : total / (size.getX() * size.getZ());
    }
}

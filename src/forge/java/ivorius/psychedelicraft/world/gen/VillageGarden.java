/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDesert;
import net.minecraft.world.biome.BiomePlains;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class VillageGarden extends StructureVillagePieces.Village {
    private static final Plot[] PLAINS = {Plot.PLAINS_HOT_HOUSE_1, Plot.PLAINS_HOT_HOUSE_2, Plot.VINEYARD, Plot.LATTICE_WALL};
    private static final Plot[] DESERT = {Plot.DESERT_HOT_HOUSE_1, Plot.DESERT_HOT_HOUSE_2};
    private Plot plot;

    public VillageGarden() {
    }

    private VillageGarden(StructureVillagePieces.Start start, int type, EnumFacing facing, StructureBoundingBox bounds, Plot plot) {
        super(start, type);
        setCoordBaseMode(facing);
        boundingBox = bounds;
        this.plot = plot;
    }

    public static void register() {
        MapGenStructureIO.registerStructureComponent(VillageGarden.class, Psychedelicraft.id("village_garden").toString());
        VillagerRegistry.instance().registerVillageCreationHandler(new Handler());
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        super.writeStructureToNBT(tag);
        tag.setString("Plot", plot.name());
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager templates) {
        super.readStructureFromNBT(tag, templates);
        plot = Plot.valueOf(tag.getString("Plot"));
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunkBounds) {
        if (averageGroundLvl < 0) {
            averageGroundLvl = getAverageGroundLevel(world, chunkBounds);
            if (averageGroundLvl < 0) {
                return true;
            }
            boundingBox.offset(0, averageGroundLvl - plot.entranceHeight - boundingBox.minY, 0);
        }

        Template template = world.getSaveHandler().getStructureTemplateManager().get(world.getMinecraftServer(), plot.template);
        if (template == null) {
            throw new IllegalStateException("Missing village structure " + plot.template);
        }
        PlacementSettings placement = new PlacementSettings()
                .setRotation(plot.rotation(getCoordBaseMode())).setBoundingBox(chunkBounds).setIgnoreEntities(true);
        BlockPos origin = plot.origin(boundingBox, placement);
        template.addBlocksToWorld(world, origin, (level, pos, info) -> info.tileentityData == null ? info
                : new Template.BlockInfo(info.pos, info.blockState, info.tileentityData.copy()), placement, 2);

        Block foundation = structureType == 1 ? Blocks.SANDSTONE : Blocks.DIRT;
        for (int x = 0; x < plot.width; x++) {
            for (int z = 0; z < plot.depth; z++) {
                replaceAirAndLiquidDownwards(world, foundation.getDefaultState(), x, -1, z, chunkBounds);
            }
        }
        if (plot != Plot.LATTICE_WALL) {
            spawnVillagers(world, chunkBounds, 4, 2, 10, 1);
        }
        return true;
    }

    enum Plot {
        PLAINS_HOT_HOUSE_1("plains/houses/plains_hot_house_1", 50),
        PLAINS_HOT_HOUSE_2("plains/houses/plains_hot_house_2", 50),
        VINEYARD("plains/houses/plains_vineyard", 25),
        LATTICE_WALL("plains/houses/plains_lattice_wall_1", 15, 12, 7, 1, Rotation.CLOCKWISE_180),
        DESERT_HOT_HOUSE_1("desert/houses/desert_hot_house_1", 25),
        DESERT_HOT_HOUSE_2("desert/houses/desert_hot_house_2", 50);

        final ResourceLocation template;
        final int weight;
        final int width;
        final int depth;
        final int entranceHeight;
        private final Rotation entranceRotation;

        Plot(String path, int weight) {
            this(path, weight, 9, 12, 2, Rotation.NONE);
        }

        Plot(String path, int weight, int width, int depth, int entranceHeight, Rotation entranceRotation) {
            this.template = Psychedelicraft.id("village/" + path);
            this.weight = weight;
            this.width = width;
            this.depth = depth;
            this.entranceHeight = entranceHeight;
            this.entranceRotation = entranceRotation;
        }

        Rotation rotation(EnumFacing facing) {
            switch (facing) {
                case NORTH:
                    return entranceRotation.add(Rotation.CLOCKWISE_180);
                case EAST:
                    return entranceRotation.add(Rotation.COUNTERCLOCKWISE_90);
                case WEST:
                    return entranceRotation.add(Rotation.CLOCKWISE_90);
                default:
                    return entranceRotation;
            }
        }

        StructureBoundingBox bounds(int x, int y, int z, EnumFacing facing) {
            return StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, width, 9, depth, facing);
        }

        BlockPos origin(StructureBoundingBox bounds, PlacementSettings placement) {
            BlockPos corner = Template.transformedBlockPos(placement, new BlockPos(width - 1, 0, depth - 1));
            return new BlockPos(bounds.minX - Math.min(0, corner.getX()), bounds.minY,
                    bounds.minZ - Math.min(0, corner.getZ()));
        }
    }

    private static final class Handler implements VillagerRegistry.IVillageCreationHandler {
        @Override
        public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int size) {
            return new StructureVillagePieces.PieceWeight(VillageGarden.class, 22, 1 + random.nextInt(2 + size));
        }

        @Override
        public Class<?> getComponentClass() {
            return VillageGarden.class;
        }

        @Override
        @Nullable
        public StructureVillagePieces.Village buildComponent(StructureVillagePieces.PieceWeight weight,
                StructureVillagePieces.Start start, List<StructureComponent> pieces, Random random,
                int x, int y, int z, EnumFacing facing, int type) {
            Plot[] choices;
            if (start.biome instanceof BiomePlains) {
                choices = PLAINS;
            } else if (start.biome instanceof BiomeDesert) {
                choices = DESERT;
            } else {
                return null;
            }

            int selection = random.nextInt(Arrays.stream(choices).mapToInt(plot -> plot.weight).sum());
            for (Plot plot : choices) {
                selection -= plot.weight;
                if (selection < 0) {
                    StructureBoundingBox bounds = plot.bounds(x, y, z, facing);
                    return canVillageGoDeeper(bounds) && StructureComponent.findIntersecting(pieces, bounds) == null
                            ? new VillageGarden(start, type, facing, bounds, plot) : null;
                }
            }
            throw new IllegalStateException("Village garden weights did not select a plot");
        }
    }
}

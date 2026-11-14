package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;

import java.util.List;

import static net.minecraft.util.EnumFacing.DOWN;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.SOUTH;
import static net.minecraft.util.EnumFacing.UP;
import static net.minecraft.util.EnumFacing.WEST;

public final class GlitchLighting {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final EnumFacing[] FACES = EnumFacing.values();
    // Neighbor order and baked-vertex order for the six faces of Minecraft's cube model.
    private static final EnumFacing[][] FACE_EDGES = {
            {WEST, EAST, NORTH, SOUTH}, {EAST, WEST, NORTH, SOUTH}, {UP, DOWN, EAST, WEST},
            {WEST, EAST, DOWN, UP}, {UP, DOWN, NORTH, SOUTH}, {DOWN, UP, NORTH, SOUTH}
    };
    private static final int[] FIRST_CORNER = {0, 2, 3, 0, 3, 1};

    private GlitchLighting() {}

    public static boolean renderSmooth(BlockModelRenderer renderer, IBlockAccess world, IBakedModel model,
                                       IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long seed) {
        return state.getBlock() == PSBlocks.GLITCH && model.getQuads(state, null, seed).isEmpty()
                ? renderCube(world, model, state, pos, buffer, checkSides, seed, true)
                : renderer.renderModelSmooth(world, model, state, pos, buffer, checkSides, seed);
    }

    public static boolean renderFlat(BlockModelRenderer renderer, IBlockAccess world, IBakedModel model,
                                     IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long seed) {
        return state.getBlock() == PSBlocks.GLITCH && model.getQuads(state, null, seed).isEmpty()
                ? renderCube(world, model, state, pos, buffer, checkSides, seed, false)
                : renderer.renderModelFlat(world, model, state, pos, buffer, checkSides, seed);
    }

    private static boolean renderCube(IBlockAccess world, IBakedModel model, IBlockState state, BlockPos pos,
                                      BufferBuilder buffer, boolean checkSides, long seed, boolean smooth) {
        World clientWorld = world instanceof World ? (World) world : Minecraft.getMinecraft().world;
        boolean end = clientWorld != null && clientWorld.provider.getDimensionType() == DimensionType.THE_END;
        boolean hasSkylight = clientWorld == null || clientWorld.provider.hasSkyLight();
        FaceLighting lighting = smooth ? new FaceLighting() : null;
        boolean rendered = false;
        for (EnumFacing face : FACES) {
            List<BakedQuad> quads = model.getQuads(state, face, seed);
            if (quads.isEmpty() || checkSides && !state.shouldSideBeRendered(world, pos, face)) continue;
            if (smooth) lighting.sample(world, pos, face, hasSkylight);
            for (BakedQuad quad : quads) {
                buffer.addVertexData(quad.getVertexData());
                float diffuse = shade(face, quad.shouldApplyDiffuseLighting(), end);
                if (smooth) {
                    buffer.putBrightness4(lighting.light[0], lighting.light[1], lighting.light[2], lighting.light[3]);
                    for (int vertex = 0; vertex < 4; vertex++) {
                        float shade = diffuse * lighting.shade[vertex];
                        buffer.putColorMultiplier(shade, shade, shade, 4 - vertex);
                    }
                } else {
                    buffer.putBrightness4(FULL_BRIGHT, FULL_BRIGHT, FULL_BRIGHT, FULL_BRIGHT);
                    for (int vertex = 1; vertex <= 4; vertex++) buffer.putColorMultiplier(diffuse, diffuse, diffuse, vertex);
                }
                buffer.putPosition(pos.getX(), pos.getY(), pos.getZ());
                rendered = true;
            }
        }
        return rendered;
    }

    private static float shade(EnumFacing face, boolean diffuse, boolean end) {
        if (end && (!diffuse || face.getAxis() == EnumFacing.Axis.Y)) return 0.9F;
        return diffuse ? LightUtil.diffuseLight(face) : 1;
    }

    static final class FaceLighting {
        final int[] light = new int[4];
        final float[] shade = new float[4];
        private final int[] edgeLight = new int[4];
        private final float[] edgeShade = new float[4];
        private final boolean[] open = new boolean[4];
        private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();

        void sample(IBlockAccess world, BlockPos pos, EnumFacing face, boolean hasSkylight) {
            BlockPos front = pos.offset(face);
            IBlockState center = world.getBlockState(front);
            int centerLight = packedLight(world, front, center, hasSkylight);
            float centerShade = center.getAmbientOcclusionLightValue();
            int faceIndex = face.getIndex();
            EnumFacing[] edges = FACE_EDGES[faceIndex];
            for (int i = 0; i < edges.length; i++) {
                samplePos.setPos(front).move(edges[i]);
                IBlockState edge = world.getBlockState(samplePos);
                edgeLight[i] = packedLight(world, samplePos, edge, hasSkylight);
                edgeShade[i] = edge.getAmbientOcclusionLightValue();
                samplePos.move(face);
                IBlockState farther = world.getBlockState(samplePos);
                open[i] = farther.getLightOpacity(world, samplePos) == 0 || !farther.causesSuffocation()
                        || !Block.FULL_BLOCK_AABB.equals(farther.getCollisionBoundingBox(world, samplePos));
            }
            for (int corner = 0; corner < 4; corner++) {
                int first = corner / 2;
                int second = corner == 1 || corner == 2 ? 2 : 3;
                int diagonalLight = edgeLight[0];
                float diagonalShade = edgeShade[0];
                if (open[first] || open[second]) {
                    samplePos.setPos(front).move(edges[first]).move(edges[second]);
                    IBlockState diagonal = world.getBlockState(samplePos);
                    diagonalLight = packedLight(world, samplePos, diagonal, hasSkylight);
                    diagonalShade = diagonal.getAmbientOcclusionLightValue();
                }
                int vertex = (FIRST_CORNER[faceIndex] + corner) & 3;
                light[vertex] = blend(edgeLight[first], edgeLight[second], diagonalLight, centerLight);
                shade[vertex] = (edgeShade[first] + edgeShade[second] + diagonalShade + centerShade) * 0.25F;
            }
        }
    }

    private static int packedLight(IBlockAccess world, BlockPos pos, IBlockState state, boolean hasSkylight) {
        if (state.getBlock() == PSBlocks.GLITCH) return FULL_BRIGHT;
        int sky;
        int block;
        // Modern ambient occlusion reads the sampled cell's light directly.
        if (world instanceof ChunkCache) {
            ChunkCache chunk = (ChunkCache) world;
            sky = hasSkylight ? chunk.getLightFor(EnumSkyBlock.SKY, pos) : 0;
            block = chunk.getLightFor(EnumSkyBlock.BLOCK, pos);
        } else if (world instanceof World) {
            World level = (World) world;
            sky = hasSkylight ? level.getLightFor(EnumSkyBlock.SKY, pos) : 0;
            block = level.getLightFor(EnumSkyBlock.BLOCK, pos);
        } else {
            return world.getCombinedLight(pos, state.getLightValue(world, pos));
        }
        return (sky << 20) | (Math.max(block, state.getLightValue(world, pos)) << 4);
    }

    private static int blend(int side, int otherSide, int diagonal, int center) {
        if (side == 0) side = center;
        if (otherSide == 0) otherSide = center;
        if (diagonal == 0) diagonal = center;
        return ((side + otherSide + diagonal + center) >> 2) & 0xFF00FF;
    }
}

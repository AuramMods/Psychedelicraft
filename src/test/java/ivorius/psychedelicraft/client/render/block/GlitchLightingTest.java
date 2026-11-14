package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;

public class GlitchLightingTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void smoothLightingUsesTheFaceAndItsDiagonalNeighbors() {
        Map<BlockPos, IBlockState> states = new HashMap<>();
        Map<BlockPos, Integer> blockLight = new HashMap<>();
        IBlockAccess region = (IBlockAccess) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IBlockAccess.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBlockState":
                            return states.getOrDefault(args[0], Blocks.AIR.getDefaultState());
                        case "getCombinedLight":
                            return Math.max(blockLight.getOrDefault(args[0], 0), (int) args[1]) << 4;
                        default:
                            throw new AssertionError(method.getName());
                    }
                });
        BlockPos pos = new BlockPos(0, 64, 0);
        BlockPos front = pos.north();
        IBlockState glitch = PSBlocks.GLITCH.getDefaultState();
        states.put(pos, glitch);
        GlitchLighting.FaceLighting face = new GlitchLighting.FaceLighting();

        face.sample(region, pos, EnumFacing.NORTH, true);
        assertArrayEquals(new int[]{0, 0, 0, 0}, face.light);
        states.put(front.up().east(), glitch);
        face.sample(region, pos, EnumFacing.NORTH, true);
        assertArrayEquals(new int[]{0x3C003C, 0, 0, 0}, face.light);
        assertArrayEquals(new float[]{0.8F, 1, 1, 1}, face.shade, 0.0001F);

        states.put(front.up().north(), Blocks.STONE.getDefaultState());
        states.put(front.east().north(), Blocks.STONE.getDefaultState());
        blockLight.put(front.up(), 12);
        blockLight.put(front.east(), 8);
        face.sample(region, pos, EnumFacing.NORTH, true);
        assertArrayEquals(new int[]{128, 32, 0, 48}, face.light);
    }
}

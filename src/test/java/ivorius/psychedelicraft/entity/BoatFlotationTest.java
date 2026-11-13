package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BoatFlotationTest {
    private static final BlockPos CELL = new BlockPos(5, 64, 5);
    private static PhysicalFluidBlock coffee;

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
        coffee = new PhysicalFluidBlock(PSFluids.COFFEE);
    }

    @Test
    public void stackedCoffeeUsesTheTopCellsFractionalHeight() {
        TestWorld world = new TestWorld();
        world.blocks.put(CELL, coffee.getStateFromMeta(3));
        EntityBoat boat = new EntityJuniperBoat(world);
        place(boat, -0.2);
        assertEquals(64 + 5 / 9F, BoatFlotation.waterLevelAbove(boat, -1.2), 0.00001);

        world.blocks.put(CELL.up(), coffee.getStateFromMeta(2));
        assertEquals(65 + 6 / 9F, BoatFlotation.waterLevelAbove(boat, -1.2), 0.00001);
    }

    @Test
    public void submergedCoffeeDistinguishesSourcesFromFlowingLiquid() {
        TestWorld world = new TestWorld();
        EntityBoat boat = new EntityJuniperChestBoat(world);
        world.blocks.put(CELL, coffee.getDefaultState());
        place(boat, 0.15);
        assertEquals(EntityBoat.Status.UNDER_WATER, BoatFlotation.underwaterStatus(boat));

        world.blocks.put(CELL, coffee.getStateFromMeta(3));
        place(boat, -0.12);
        assertEquals(EntityBoat.Status.UNDER_FLOWING_WATER, BoatFlotation.underwaterStatus(boat));
        place(boat, 0.3);
        assertNull(BoatFlotation.underwaterStatus(boat));
        assertEquals(64 + 5 / 9D, BoatFlotation.waterLevel(boat), 0.00001);
    }

    private static void place(EntityBoat boat, double relativeY) {
        boat.setPosition(CELL.getX() + 0.5, CELL.getY() + relativeY, CELL.getZ() + 0.5);
    }

    private static final class TestWorld extends World {
        private final Map<BlockPos, IBlockState> blocks = new HashMap<>();

        private TestWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.getDefaultState());
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }
    }
}

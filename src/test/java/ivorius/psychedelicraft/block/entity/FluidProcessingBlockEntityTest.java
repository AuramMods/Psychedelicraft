package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FluidProcessingBlockEntityTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void distilleryDurationRoundsDownAndKeepsTheCompletionTick() {
        assertEquals(102, DistilleryBlockEntity.distillationTicks(101, 1));
        assertEquals(51, DistilleryBlockEntity.distillationTicks(101, 2));
        assertEquals(34, DistilleryBlockEntity.distillationTicks(101, 3));
        assertEquals(15, DistilleryBlockEntity.distillationTicks(101, 7));
        assertEquals(2, DistilleryBlockEntity.distillationTicks(3, 7));
    }

    @Test
    public void processingBetweenSyncUpdatesMarksTheChunkDirty() {
        ProcessingWorld world = new ProcessingWorld();
        Processor machine = new Processor();
        machine.setWorld(world);
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("warmth", 2);
        machine.getTank().replace(new FluidStack(PSFluids.COFFEE, 1000, attributes));
        machine.tickProcessing();
        world.dirty = false;

        machine.tickProcessing();

        assertTrue(world.dirty);
        assertEquals(298, machine.getRemainingTicks());
    }

    @Test
    public void partialVesselProgressStartsAtTheAmountAvailableToTransfer() {
        FlaskBlockEntity flask = new FlaskBlockEntity();
        flask.setWorld(new ProcessingWorld());
        flask.getTank().replace(new FluidStack(PSFluids.WHEAT, 100));
        flask.getVessels().setStackInSlot(0, FluidVesselHandler.forCrafting(new ItemStack(PSItems.BOTTLE))
                .craftContents(PSFluids.WHEAT, 75, null));
        flask.getVessels().setStackInSlot(1, FluidVesselHandler.forCrafting(new ItemStack(PSItems.BOTTLE))
                .craftContents(PSFluids.WHEAT, 1900, null));

        assertEquals(0F, flask.getVesselProgress(0), 0F);
        assertEquals(0F, flask.getVesselProgress(1), 0F);
        flask.update();
        assertEquals(2F / 3F, flask.getVesselProgress(0), 0.00001F);
        assertEquals(0.5F, flask.getVesselProgress(1), 0F);
        flask.update();
        assertEquals(1F, flask.getVesselProgress(0), 0F);
        assertEquals(1F, flask.getVesselProgress(1), 0F);
        assertEquals(75, flask.getTank().getFluidAmount());
    }

    private static final class Processor extends FluidProcessingBlockEntity {
        private Processor() {
            super(1000);
        }

        @Override
        protected void tickProcessing() {
            processFluid(false);
        }
    }

    private static final class ProcessingWorld extends World {
        private boolean dirty;

        private ProcessingWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public void markChunkDirty(BlockPos pos, TileEntity tile) {
            dirty = true;
        }

        @Override
        public long getTotalWorldTime() {
            return 1;
        }
    }
}

package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PhysicalFluidStateTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void allFlowLevelsRoundTripIndependentlyOfTheAttributeTileBit() {
        PhysicalFluidBlock block = new PhysicalFluidBlock(PSFluids.COFFEE);
        for (int metadata = 0; metadata < 16; metadata++) {
            IBlockState state = block.getStateFromMeta(metadata);
            assertEquals(metadata, block.getMetaFromState(state));
            assertEquals(metadata % 8, state.getValue(BlockFluidBase.LEVEL).intValue());
            assertEquals(metadata >= 8, block.hasTileEntity(state));
            assertEquals(metadata % 8 == 0, block.canCollideCheck(state, true));
        }
    }

    @Test
    public void sourceAndBucketShareAStableFluidAndAttributeEncoding() {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("warmth", 2);
        NBTTagCompound batch = new NBTTagCompound();
        batch.setString("origin", "spring");
        attributes.setTag("batch", batch);
        NBTTagCompound data = FluidContents.write(new FluidStack(PSFluids.COFFEE, Fluid.BUCKET_VOLUME, attributes));
        FluidStack restored = FluidContents.read(data, Fluid.BUCKET_VOLUME);
        assertSame(PSFluids.COFFEE, restored.getFluid());
        assertEquals(Fluid.BUCKET_VOLUME, restored.amount);
        assertEquals(attributes, restored.tag);
        assertTrue(restored.getFluid().doesVaporize(restored));
    }

    @Test
    public void physicalFluidWaterMembershipMatchesTheDonor() {
        assertEquals(24L, PSFluids.all().stream().filter(PhysicalFluidBlock::isWaterLike).count());
        assertTrue(PhysicalFluidBlock.isWaterLike(FluidRegistry.WATER));
        assertFalse(PhysicalFluidBlock.isWaterLike(FluidRegistry.LAVA));
        assertFalse(PhysicalFluidBlock.isWaterLike(PSFluids.AGAVE));
        assertFalse(PhysicalFluidBlock.isWaterLike(PSFluids.KAVA));
        assertFalse(PhysicalFluidBlock.isWaterLike(PSFluids.BATH_SALTS));
    }

    @Test
    public void firstMatchingFluidSuppliesAttributesEvenAtAWeakerFlowLevel() {
        PhysicalFluidBlock brew = new PhysicalFluidBlock(PSFluids.APPLE);
        PhysicalFluidBlock coffee = new PhysicalFluidBlock(PSFluids.COFFEE);
        FluidWorld world = new FluidWorld();
        BlockPos cell = new BlockPos(0, 64, 0);
        NBTTagCompound mature = new NBTTagCompound();
        mature.setInteger(BrewFluid.MATURATION, 6);
        NBTTagCompound distilled = new NBTTagCompound();
        distilled.setInteger(BrewFluid.DISTILLATION, 2);
        world.put(cell.down(), coffee.getStateFromMeta(8), null);
        world.put(cell.north(), brew.getStateFromMeta(14), mature);
        world.put(cell.east(), brew.getStateFromMeta(8), distilled);

        assertEquals(mature, brew.neighborAttributes(world, cell));
        brew.neighborAttributes(world, cell).setInteger(BrewFluid.MATURATION, 1);
        assertEquals(mature, brew.neighborAttributes(world, cell));

        world.put(cell.down(), brew.getStateFromMeta(7), null);
        assertNull(brew.neighborAttributes(world, cell));
    }

    private static final class FluidWorld extends World {
        private final Map<BlockPos, IBlockState> blocks = new HashMap<>();
        private final Map<BlockPos, TileEntity> tiles = new HashMap<>();

        private FluidWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        private void put(BlockPos pos, IBlockState state, NBTTagCompound attributes) {
            blocks.put(pos, state);
            if (attributes != null) {
                FluidAttributesTile tile = new FluidAttributesTile();
                NBTTagCompound data = new NBTTagCompound();
                data.setTag("attributes", attributes);
                tile.readFromNBT(data);
                tiles.put(pos, tile);
            }
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.getDefaultState());
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return tiles.get(pos);
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

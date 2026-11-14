package ivorius.psychedelicraft.block.entity;

import com.google.common.base.Predicate;
import ivorius.psychedelicraft.entity.RealityRiftEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RiftJarIntakeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void threeWayIntakeUsesTheDonorFloatOrder() {
        IntakeWorld world = new IntakeWorld();
        RealityRiftEntity stable = world.rift(.002F);
        RealityRiftEntity depleted = world.rift(.00002F);
        depleted.onKillCommand();
        RealityRiftEntity closing = world.rift(.002F);
        closing.onKillCommand();

        RiftJarBlockEntity jar = new RiftJarBlockEntity();
        jar.setWorld(world);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("isOpening", true);
        tag.setFloat("fractionOpen", .54F);
        jar.readFromNBT(tag);

        jar.update();

        float rate = (1F / 3) * .001F * .56F;
        float remaining = .002F - rate;
        float withdrawn = .002F - remaining;
        assertEquals(remaining, stable.getRiftSize(), 0F);
        assertEquals(0F, depleted.getRiftSize(), 0F);
        assertEquals(remaining, closing.getRiftSize(), 0F);
        assertEquals(withdrawn + .00002F + withdrawn, jar.getAmount(), 0F);
    }

    private static final class IntakeWorld extends World {
        private final List<RealityRiftEntity> rifts = new ArrayList<>();

        private IntakeWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        private RealityRiftEntity rift(float size) {
            RealityRiftEntity rift = new RealityRiftEntity(this);
            rift.setRiftSize(size);
            rifts.add(rift);
            return rift;
        }

        @Override
        public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type, AxisAlignedBB bounds,
                                                               Predicate<? super T> filter) {
            List<T> result = new ArrayList<>();
            for (RealityRiftEntity rift : rifts) {
                T candidate = type.cast(rift);
                if (bounds.intersects(candidate.getEntityBoundingBox()) && filter.apply(candidate)) result.add(candidate);
            }
            return result;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public void markChunkDirty(BlockPos pos, TileEntity tile) {
        }

        @Override
        public long getTotalWorldTime() {
            return 1;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return false;
        }
    }
}

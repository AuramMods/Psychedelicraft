package ivorius.psychedelicraft.entity;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealityRiftTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void commandClosurePreservesStateThroughNbt() {
        RiftWorld world = new RiftWorld();
        RealityRiftEntity rift = new RealityRiftEntity(world);
        NBTTagCompound initial = new NBTTagCompound();
        initial.setFloat("riftSize", 4F);
        initial.setFloat("instability", .75F);
        rift.readEntityFromNBT(initial);
        assertTrue(rift.isCritical());

        rift.onKillCommand();
        assertFalse(rift.isDead);
        assertTrue(rift.isRiftClosing());
        assertFalse(rift.isCritical());
        assertEquals(4F, rift.getRiftSize(), 0F);

        NBTTagCompound saved = new NBTTagCompound();
        rift.writeEntityToNBT(saved);
        RealityRiftEntity restored = new RealityRiftEntity(world);
        restored.readEntityFromNBT(saved);
        assertEquals(4F, restored.getRiftSize(), 0F);
        assertEquals(.75F, restored.getInstability(), 0F);
        assertTrue(restored.isRiftClosing());
    }

    @Test
    public void omittedNbtKeepsInitialValuesAndExplicitZeroStillLoads() {
        RealityRiftEntity rift = new RealityRiftEntity(new RiftWorld());
        rift.setRiftSize(.5F);
        rift.onKillCommand();
        rift.readEntityFromNBT(new NBTTagCompound());
        assertEquals(.5F, rift.getRiftSize(), 0F);
        assertTrue(rift.isRiftClosing());

        NBTTagCompound empty = new NBTTagCompound();
        empty.setFloat("riftSize", 0);
        empty.setBoolean("isRiftClosing", false);
        rift.readEntityFromNBT(empty);
        assertEquals(0F, rift.getRiftSize(), 0F);
        assertFalse(rift.isRiftClosing());
    }

    private static final class RiftWorld extends World {
        private RiftWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
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

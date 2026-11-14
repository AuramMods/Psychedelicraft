package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorksiteReleasesTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        GameRegistry.registerTileEntity(DryingTableBlockEntity.class, new ResourceLocation("psychedelicraft", "drying_table"));
    }

    @Test
    public void savedReleasesStayInTheirDimensionAndMatchTheReservedOwner() {
        UUID owner = new UUID(1, 2);
        UUID other = new UUID(3, 4);
        BlockPos site = new BlockPos(35, 64, -48);
        WorksiteReleases records = new WorksiteReleases("test");
        records.add(0, site, owner);
        records.add(0, site, owner);
        records.add(0, site, other);
        records.add(-1, site, other);
        NBTTagCompound saved = records.writeToNBT(new NBTTagCompound());
        assertEquals(3, saved.getTagList("Releases", 10).tagCount());
        records = new WorksiteReleases("test");
        records.readFromNBT(saved);

        Chunk chunk = new Chunk(null, site.getX() >> 4, site.getZ() >> 4);
        DryingTableBlockEntity table = new DryingTableBlockEntity();
        table.claim(owner);
        chunk.getTileEntityMap().put(site, table);

        records.apply(-1, chunk);
        assertFalse(table.availableTo(other));
        records.apply(0, chunk);
        assertTrue(table.availableTo(other));
        assertEquals(0, records.writeToNBT(new NBTTagCompound()).getTagList("Releases", 10).tagCount());
        table.claim(other);
        records.apply(0, chunk);
        assertFalse(table.availableTo(owner));
    }

    @Test
    public void savedPotentialReleaseOnlyClearsTheMatchingTemporaryReservation() {
        UUID owner = new UUID(1, 2);
        UUID other = new UUID(3, 4);
        BlockPos site = new BlockPos(35, 64, -48);
        Chunk chunk = new Chunk(null, site.getX() >> 4, site.getZ() >> 4);
        DryingTableBlockEntity table = new DryingTableBlockEntity();
        chunk.getTileEntityMap().put(site, table);

        assertTrue(table.reserve(owner, 1200));
        assertFalse(table.reserve(other, 1200));
        NBTTagCompound saved = table.writeToNBT(new NBTTagCompound());
        table.readFromNBT(saved);
        assertTrue(table.reservedFor(owner));

        WorksiteReleases original = new WorksiteReleases("test");
        original.add(0, site, owner, 1200);
        WorksiteReleases loaded = new WorksiteReleases("test");
        loaded.readFromNBT(original.writeToNBT(new NBTTagCompound()));
        loaded.apply(0, chunk);
        assertTrue(table.availableTo(other));

        assertTrue(table.reserve(owner, 1300));
        loaded.readFromNBT(original.writeToNBT(new NBTTagCompound()));
        loaded.apply(0, chunk);
        assertFalse(table.availableTo(other));

        assertTrue(table.claim(owner));
        loaded.readFromNBT(original.writeToNBT(new NBTTagCompound()));
        loaded.apply(0, chunk);
        assertFalse(table.availableTo(other));
    }
}

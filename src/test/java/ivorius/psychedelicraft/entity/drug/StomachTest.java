package ivorius.psychedelicraft.entity.drug;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StomachTest {
    @Test
    public void appetiteCountsFoodOverTheRealCapAndPersistsItWithTheLock() {
        StomachFoodStats stats = new StomachFoodStats();
        Stomach stomach = new Stomach();
        stats.bind(stomach, false);
        stats.setFoodLevel(12);
        stomach.updateHunger(12, 5, -0.5F);
        stats.addStats(8, 0.8F);
        assertTrue(stats.needFood());
        assertEquals(20, StomachFoodStats.packetFoodLevel(stats));
        assertEquals(16, stats.getFoodLevel());
        assertEquals(0F, stomach.toNbt().getFloat("overeating"), 0F);
        for (int i = 0; i < 10; i++) stats.addStats(8, 0.8F);
        assertEquals(10F, stomach.toNbt().getFloat("overeating"), 0F);

        Stomach restored = new Stomach();
        restored.fromNbt(stomach.toNbt());
        assertTrue(restored.needsFood(false));
        assertEquals(16, restored.perceivedFood(20));
        assertEquals(10F, restored.toNbt().getFloat("overeating"), 0F);
        restored.fromNbt(new NBTTagCompound());
        assertFalse(restored.isLocked());
        assertEquals(0F, restored.toNbt().getFloat("overeating"), 0F);
    }

    @Test
    public void nativeFoodPacketsUseRawLevelsAndClientProjectionRunsOnce() {
        StomachFoodStats server = new StomachFoodStats();
        Stomach serverStomach = new Stomach();
        server.bind(serverStomach, false);
        server.setFoodLevel(16);
        serverStomach.updateHunger(16, 5, 0.5F);
        server.addStats(-8, 0.25F);
        assertEquals(8, StomachFoodStats.packetFoodLevel(server));
        assertEquals(1F, StomachFoodStats.packetSaturation(server), 0F);
        assertEquals(12, server.getFoodLevel());
        assertEquals(3F, server.getSaturationLevel(), 0F);
        assertFalse(server.needFood());

        StomachFoodStats client = new StomachFoodStats();
        Stomach clientStomach = new Stomach();
        clientStomach.fromNbt(serverStomach.toNbt());
        client.bind(clientStomach, true);
        client.setFoodLevel(StomachFoodStats.packetFoodLevel(server));
        assertEquals(12, client.getFoodLevel());
        server.setFoodLevel(2);
        assertEquals(8, StomachFoodStats.packetFoodLevel(server));

        serverStomach.updateHunger(8, 1, 0);
        assertTrue(server.needFood());
        assertEquals(8, server.getFoodLevel());
        assertEquals(1F, server.getSaturationLevel(), 0F);
    }
}

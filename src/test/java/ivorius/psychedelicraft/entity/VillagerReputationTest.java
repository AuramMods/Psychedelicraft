package ivorius.psychedelicraft.entity;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VillagerReputationTest {
    @Test
    public void pricesCombineDemandAndBothAdjustmentsBeforeClamping() {
        assertEquals(4, VillagerPrices.adjustedCost(9, 6, 64, 0.5F, 6, 2));
        assertEquals(21, VillagerPrices.adjustedCost(8, 6, 64, 0.5F, -25, -1));
        assertEquals(64, VillagerPrices.adjustedCost(79, 6, 64, 0.5F, 4, -1));
        assertEquals(62, VillagerPrices.adjustedCost(64, 6, 64, 0.5F, 4, -1));
        assertEquals(1, VillagerPrices.adjustedCost(2, 2, 64, 0.8F, 125, 0));
        assertEquals(2, VillagerPrices.adjustedCost(3, 3, 64, 0.5F, 0, 0));
    }

    @Test
    public void gossipUsesDonorWeightsAndDropsExpiredTopics() {
        NBTTagCompound players = new NBTTagCompound();
        NBTTagCompound first = new NBTTagCompound();
        first.setInteger("MAJOR_POSITIVE", 20);
        first.setInteger("MINOR_POSITIVE", 25);
        first.setInteger("MAJOR_NEGATIVE", 25);
        first.setInteger("MINOR_NEGATIVE", 25);
        first.setInteger("TRADING", 2);
        players.setTag("first", first);
        NBTTagCompound second = new NBTTagCompound();
        second.setInteger("TRADING", 3);
        players.setTag("second", second);

        assertEquals(-23, VillagerReputation.score(first));
        VillagerReputation.decay(players);
        assertEquals(44, VillagerReputation.score(first));
        assertFalse(first.hasKey("TRADING"));
        assertFalse(players.hasKey("second"));
    }
}

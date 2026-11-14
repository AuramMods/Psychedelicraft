package ivorius.psychedelicraft.entity;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WanderingTraderSpawnerTest {
    @Test
    public void freshWorldStartsWithOneDayAndTheMinimumChance() {
        WanderingTraderSpawner.SpawnSchedule schedule = new WanderingTraderSpawner.SpawnSchedule("trader");
        schedule.readFromNBT(new NBTTagCompound());
        NBTTagCompound saved = schedule.writeToNBT(new NBTTagCompound());
        assertEquals(24000, saved.getInteger("Delay"));
        assertEquals(25, saved.getInteger("Chance"));
    }

    @Test
    public void keepsTheRemainingDelayAndIncreasedChanceAcrossLoads() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setInteger("Delay", 1953);
        saved.setInteger("Chance", 75);
        WanderingTraderSpawner.SpawnSchedule schedule = new WanderingTraderSpawner.SpawnSchedule("trader");
        schedule.readFromNBT(saved);
        assertEquals(saved, schedule.writeToNBT(new NBTTagCompound()));
    }
}

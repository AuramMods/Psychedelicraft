package ivorius.psychedelicraft.entity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class VillageSiegeRewardTest {
    @Test
    public void reloadKeepsParticipantsInACompletedWaveAndDropsInterruptedSpawns() {
        UUID defender = new UUID(1, 2);
        UUID attacker = new UUID(3, 4);
        NBTTagCompound completed = new NBTTagCompound();
        completed.setUniqueId("Id", new UUID(5, 6));
        completed.setLong("Center", new BlockPos(-12, 63, 48).toLong());
        completed.setLong("Expires", 50000);
        completed.setBoolean("Closed", true);
        completed.setTag("Defenders", ids(defender));
        completed.setTag("Attackers", ids(attacker));

        NBTTagCompound spawning = completed.copy();
        spawning.setUniqueId("Id", new UUID(7, 8));
        spawning.setBoolean("Closed", false);
        NBTTagList saved = new NBTTagList();
        saved.appendTag(completed);
        saved.appendTag(spawning);
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Sieges", saved);

        VillageSiegeReward.Records records = new VillageSiegeReward.Records("test");
        records.readFromNBT(data);

        NBTTagList loaded = records.writeToNBT(new NBTTagCompound()).getTagList("Sieges", 10);
        assertEquals(1, loaded.tagCount());
        assertEquals(completed, loaded.getCompoundTagAt(0));
    }

    private static NBTTagList ids(UUID id) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setUniqueId("Id", id);
        NBTTagList list = new NBTTagList();
        list.appendTag(entry);
        return list;
    }
}

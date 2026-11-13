package ivorius.psychedelicraft.entity;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;

final class AddictRestockTask extends EntityAIBase {
    private static final int CHECK_INTERVAL = 300;
    private final EntityVillager villager;

    AddictRestockTask(EntityVillager villager) {
        this.villager = villager;
    }

    @Override
    public boolean shouldExecute() {
        if (villager.getProfessionForge() != VillagerCareers.ADDICT || !VillagerWork.canWork(villager)) return false;
        long now = villager.world.getTotalWorldTime();
        return now >= VillagerCareers.savedData(villager).getLong("NextRestockCheck")
                && now >= CHECK_INTERVAL && villager.world.rand.nextInt(2) == 0;
    }

    @Override
    public void startExecuting() {
        NBTTagCompound data = VillagerCareers.data(villager);
        data.setLong("NextRestockCheck", villager.world.getTotalWorldTime() + CHECK_INTERVAL);
        VillagerCareers.restock(villager);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }
}

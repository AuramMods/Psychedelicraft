package ivorius.psychedelicraft.client;

import java.util.UUID;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;

public final class ClientDrugSync {
    private ClientDrugSync() {
    }

    public static void apply(INetHandler sender, int entityId, int dimension, UUID playerId, NBTTagCompound data) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() != sender || client.world == null
                || client.world.provider.getDimension() != dimension) {
            return;
        }
        Entity subject = client.world.getEntityByID(entityId);
        if (subject instanceof EntityPlayer && playerId.equals(subject.getUniqueID())) {
            DrugProperties properties = DrugCapability.get((EntityPlayer) subject);
            if (properties != null) {
                properties.fromNbt(data);
            }
        }
    }
}

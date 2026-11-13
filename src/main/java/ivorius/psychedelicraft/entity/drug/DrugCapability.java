package ivorius.psychedelicraft.entity.drug;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public final class DrugCapability implements ICapabilitySerializable<NBTTagCompound> {
    @CapabilityInject(DrugProperties.class)
    private static Capability<DrugProperties> capability = null;

    private final DrugProperties properties = new DrugProperties();

    public static void register() {
        CapabilityManager.INSTANCE.register(DrugProperties.class, new Capability.IStorage<DrugProperties>() {
            @Override
            public NBTBase writeNBT(Capability<DrugProperties> cap, DrugProperties instance, EnumFacing side) {
                return instance.toNbt();
            }

            @Override
            public void readNBT(Capability<DrugProperties> cap, DrugProperties instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.fromNbt((NBTTagCompound) nbt);
                }
            }
        }, DrugProperties::new);
    }

    @Nullable
    public static DrugProperties get(EntityPlayer player) {
        return player.getCapability(capability, null);
    }

    @Override
    public boolean hasCapability(Capability<?> query, @Nullable EnumFacing side) {
        return query == capability;
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> query, @Nullable EnumFacing side) {
        return hasCapability(query, side) ? capability.cast(properties) : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return properties.toNbt();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        properties.fromNbt(nbt);
    }
}

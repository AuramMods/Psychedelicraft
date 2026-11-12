package ivorius.psychedelicraft.util;

import net.minecraft.nbt.NBTTagCompound;

public interface NbtSerialisable {
    default NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        toNbt(tag);
        return tag;
    }

    void toNbt(NBTTagCompound tag);

    void fromNbt(NBTTagCompound tag);
}

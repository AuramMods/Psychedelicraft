package ivorius.psychedelicraft.util;

import net.minecraft.nbt.CompoundTag;

public interface NbtSerialisable {
    default CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        toNbt(tag);
        return tag;
    }

    void toNbt(CompoundTag tag);

    void fromNbt(CompoundTag tag);
}

package ivorius.psychedelicraft.entity.drug;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DrugCapability implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<DrugProperties> TYPE = CapabilityManager.get(new CapabilityToken<>() {});
    private final DrugProperties properties = new DrugProperties();
    private LazyOptional<DrugProperties> optional = LazyOptional.of(() -> properties);

    public static DrugProperties get(Player player) {
        return player.getCapability(TYPE).orElseThrow(() -> new IllegalStateException("Missing player drug capability"));
    }

    public void invalidate() {
        var previous=optional;
        optional=null;
        if(previous!=null)previous.invalidate();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        if(capability!=TYPE)return LazyOptional.empty();
        // Forge revives the same player/dispatcher after dimension travel and while copying a clone.
        // The owner gates queries while invalid; revive exposes a fresh handle to the retained data.
        if(optional==null)optional=LazyOptional.of(() -> properties);
        return optional.cast();
    }

    @Override
    public CompoundTag serializeNBT() { return properties.toNbt(); }

    @Override
    public void deserializeNBT(CompoundTag tag) { properties.fromNbt(tag); }
}

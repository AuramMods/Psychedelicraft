package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public enum HallucinationType {
    RASTA_HEAD("rasta_head"),
    HOSTILE_VILLAGERS("hostile_villagers"),
    FRIENDLY_ZOMBIES("friendly_zombies");

    private final ResourceLocation id;

    HallucinationType(String name) {
        id = Psychedelicraft.id(name);
    }

    public String id() {
        return id.toString();
    }

    @Nullable
    public static HallucinationType byName(String name) {
        for (HallucinationType type : values()) {
            if (type.id().equals(name) || type.id.getResourcePath().equals(name)) return type;
        }
        return null;
    }
}

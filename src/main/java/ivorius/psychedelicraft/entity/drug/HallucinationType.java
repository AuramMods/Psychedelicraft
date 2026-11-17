/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

public enum HallucinationType {
    RASTA_HEAD("rasta_head"), SINGLE_ENTITY("single_entity"), MULTIPLE_ENTITY("multiple_entity"),
    HOSTILE_VILLAGERS("hostile_villagers"), FRIENDLY_ZOMBIES("friendly_zombies");
    private final ResourceLocation id;
    HallucinationType(String name){id=Psychedelicraft.id(name);}
    public ResourceLocation id(){return id;}
    @Nullable public static HallucinationType byName(String name){
        for(var type:values())if(type.id.toString().equals(name)||type.id.getPath().equals(name))return type;
        return null;
    }
}

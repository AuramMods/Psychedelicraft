/* Copyright (c) 2014, Lukas Tenbrink. Native tag pools originally by Sollace. */
package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

final class ApparitionPool {
    private static final TagKey<EntityType<?>> SINGLE=tag("single_entity_hallucinations"),MULTIPLE=tag("multiple_entity_hallucinations");
    private ApparitionPool(){}
    private static TagKey<EntityType<?>> tag(String name){return TagKey.create(Registries.ENTITY_TYPE,Psychedelicraft.id(name));}
    static EntityType<?> choose(boolean grouped,RandomSource random){
        return BuiltInRegistries.ENTITY_TYPE.getTag(grouped?MULTIPLE:SINGLE).flatMap(pool->pool.getRandomElement(random)).<EntityType<?>>map(holder->holder.value()).orElse(EntityType.PIG);
    }
}

package ivorius.psychedelicraft.client.render;

import net.minecraft.world.entity.Entity;

/** Native client constructors do not reserve their generated ID against server-assigned entity IDs. */
final class DetachedEntities {
    private static int nextId=-1;
    private DetachedEntities(){}
    static <T extends Entity> T identify(T entity){
        while(entity.level().getEntity(nextId)!=null)nextId--;
        entity.setId(nextId--);
        return entity;
    }
}

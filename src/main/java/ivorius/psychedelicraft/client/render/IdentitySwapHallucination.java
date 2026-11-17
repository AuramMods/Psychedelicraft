/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import ivorius.psychedelicraft.mixin.client.WalkAnimationAccessor;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import javax.annotation.Nullable;

final class IdentitySwapHallucination {
    private final HallucinationType type;
    private int age;
    private Mob source,disguise;
    IdentitySwapHallucination(HallucinationType type){this.type=type;}
    boolean tick(){return ++age>300||source!=null&&source.isRemoved();}
    @Nullable Entity disguise(Entity original){
        EntityType<?> target=type==HallucinationType.HOSTILE_VILLAGERS?EntityType.VILLAGER:EntityType.ZOMBIE;
        if(original.getType()!=target||source!=null&&source!=original)return null;
        if(source==null){
            source=(Mob)original;
            if(type==HallucinationType.FRIENDLY_ZOMBIES)disguise=EntityType.VILLAGER.create(source.level());
            else if(source.getRandom().nextBoolean())disguise=EntityType.ZOMBIE.create(source.level());
            else{var villager=EntityType.ZOMBIE_VILLAGER.create(source.level());villager.setVillagerData(((Villager)source).getVillagerData());disguise=villager;}
            DetachedEntities.identify(disguise);disguise.setUUID(source.getUUID());disguise.setCustomName(source.getCustomName());disguise.setCustomNameVisible(source.isCustomNameVisible());
            disguise.setLeftHanded(source.isLeftHanded());disguise.setSilent(true);
            for(var slot:EquipmentSlot.values())disguise.setItemSlot(slot,source.getItemBySlot(slot).copy());
        }
        disguise.copyPosition(source);disguise.xo=source.xo;disguise.yo=source.yo;disguise.zo=source.zo;
        disguise.xOld=source.xOld;disguise.yOld=source.yOld;disguise.zOld=source.zOld;disguise.yRotO=source.yRotO;disguise.xRotO=source.xRotO;
        disguise.yBodyRot=source.yBodyRot;disguise.yBodyRotO=source.yBodyRotO;disguise.yHeadRot=source.yHeadRot;disguise.yHeadRotO=source.yHeadRotO;
        var from=(WalkAnimationAccessor)source.walkAnimation;var to=(WalkAnimationAccessor)disguise.walkAnimation;
        to.psychedelicraft$setSpeedOld(from.psychedelicraft$getSpeedOld());to.psychedelicraft$setSpeed(from.psychedelicraft$getSpeed());to.psychedelicraft$setPosition(from.psychedelicraft$getPosition());
        disguise.attackAnim=source.attackAnim;disguise.oAttackAnim=source.oAttackAnim;disguise.swingingArm=source.swingingArm;
        disguise.tickCount=source.tickCount;disguise.hurtTime=source.hurtTime;disguise.deathTime=source.deathTime;
        disguise.setOnGround(source.onGround());disguise.setPose(source.getPose());disguise.setShiftKeyDown(source.isShiftKeyDown());
        disguise.setInvisible(source.isInvisible());disguise.setGlowingTag(source.isCurrentlyGlowing());disguise.setSharedFlagOnFire(source.isOnFire());
        if(disguise instanceof Zombie zombie)zombie.setBaby(source.isBaby());
        if(disguise instanceof Villager villager)villager.setAge(source.isBaby()?-24000:0);
        return disguise;
    }
}

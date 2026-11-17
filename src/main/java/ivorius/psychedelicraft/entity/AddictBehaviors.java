/* Copyright (c) 2014, Lukas Tenbrink. Native behavior adaptation of Sollace's addict tasks. */
package ivorius.psychedelicraft.entity;

import com.google.common.collect.ImmutableMap;
import ivorius.psychedelicraft.PSDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.Villager;

public final class AddictBehaviors {
    private AddictBehaviors() {}
    private static boolean canWork(ServerLevel level,Villager villager){
        long day=Math.floorMod(level.getDayTime(),24000L);
        return villager.isAlive()&&!villager.isBaby()&&!villager.isTrading()&&!villager.isSleeping()&&day>=2000&&day<9000;
    }
    public static final class Restock extends Behavior<Villager>{
        public Restock(){super(ImmutableMap.of(),1);}
        @Override protected boolean checkExtraStartConditions(ServerLevel level,Villager villager){
            long time=level.getGameTime();
            return canWork(level,villager)&&time>=300&&time>=villager.getPersistentData().getLong("psychedelicraftNextRestockCheck")&&villager.getRandom().nextInt(2)==0;
        }
        @Override protected void start(ServerLevel level,Villager villager,long time){
            villager.getPersistentData().putLong("psychedelicraftNextRestockCheck",time+300);
            if(villager.shouldRestock()){
                villager.playWorkSound();villager.restock();
                villager.hurt(PSDamageTypes.of(villager,PSDamageTypes.OVERDOSE),1+villager.getRandom().nextFloat()*5);
            }
        }
    }
    public static final class Stroll extends Behavior<Villager>{
        private long nextStroll;
        public Stroll(){super(ImmutableMap.of(MemoryModuleType.WALK_TARGET,MemoryStatus.VALUE_ABSENT),1);}
        @Override protected boolean checkExtraStartConditions(ServerLevel level,Villager villager){
            return canWork(level,villager)&&level.getGameTime()>nextStroll&&level.getNearestPlayer(villager,9)!=null;
        }
        @Override protected void start(ServerLevel level,Villager villager,long time){
            nextStroll=time+180;
            var target=DefaultRandomPos.getPos(villager,8,6);
            if(target!=null)villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new net.minecraft.world.entity.ai.memory.WalkTarget(target,.5F,1));
        }
    }
    public static float shake(net.minecraft.world.entity.LivingEntity entity){
        float scale=1-entity.getHealth()/entity.getMaxHealth();
        float shake=(float)(Math.cos(entity.tickCount*3.25)*Math.PI*.4F*(1+scale*8));
        entity.hurtTime=Math.abs(shake)>5?1:0;
        return shake;
    }
}

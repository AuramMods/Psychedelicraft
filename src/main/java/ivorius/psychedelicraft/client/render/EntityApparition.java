/* Copyright (c) 2014, Lukas Tenbrink. Modern entity integration adapted from Sollace. */
package ivorius.psychedelicraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.entity.drug.HallucinationType;
import ivorius.psychedelicraft.mixin.client.EntityVisualAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.*;

final class EntityApparition {
    private final Entity entity;
    private final Vec3 velocity;
    private final Vec3[] offsets;
    private final float rotationSpeed,red,green,blue,scale;
    private final int maxAge;
    private Vec3 previousPosition,position;
    private float previousYaw,yaw;
    private int age;

    EntityApparition(LocalPlayer player,HallucinationType type,@Nullable Vec3 commandedPosition){
        boolean grouped=type==HallucinationType.MULTIPLE_ENTITY;
        Entity selected=ApparitionPool.choose(grouped,player.level().random).create(player.level());
        entity=DetachedEntities.identify(selected==null?EntityType.PIG.create(player.level()):selected);
        var random=player.getRandom();
        position=player.position().add(random.nextDouble()*50-25,random.nextDouble()*10-5,random.nextDouble()*50-25);
        if(commandedPosition!=null)position=commandedPosition;
        previousPosition=position;
        velocity=new Vec3((random.nextDouble()-.5)/10,(random.nextDouble()-.5)/10,(random.nextDouble()-.5)/10);
        previousYaw=yaw=random.nextInt(360);maxAge=(random.nextInt(59)+3)*20;
        rotationSpeed=random.nextFloat()*10*(random.nextBoolean()?0:1);
        red=random.nextFloat();green=random.nextFloat();blue=random.nextFloat();
        float size=1;while(random.nextFloat()<.3F)size*=random.nextFloat()*2.7F+.3F;scale=Math.min(size,20);
        offsets=new Vec3[grouped?random.nextInt(11)+5:1];
        for(int i=0;i<offsets.length;i++){
            double spread=Math.max(2,i/10F)*entity.getBbWidth();
            offsets[i]=grouped?new Vec3((random.nextDouble()-random.nextDouble())*spread,(random.nextDouble()-random.nextDouble())*spread,(random.nextDouble()-random.nextDouble())*spread):Vec3.ZERO;
        }
        entity.setDeltaMovement(velocity);
        if(entity instanceof LivingEntity living&&living.canBreatheUnderwater())((EntityVisualAccessor)entity).psychedelicraft$setTouchingWater(true);
        updateEntity();
    }
    boolean tick(){age++;previousPosition=position;position=position.add(velocity);previousYaw=yaw;yaw=Mth.wrapDegrees(yaw+rotationSpeed);updateEntity();return age>=maxAge;}
    private void updateEntity(){
        entity.tickCount=age;entity.setPos(position);entity.xo=entity.xOld=previousPosition.x;entity.yo=entity.yOld=previousPosition.y;entity.zo=entity.zOld=previousPosition.z;
        entity.yRotO=previousYaw;entity.setYRot(yaw);
        if(entity instanceof LivingEntity living){living.yBodyRotO=living.yHeadRotO=previousYaw;living.yBodyRot=living.yHeadRot=yaw;living.walkAnimation.update(Math.min(1,(float)velocity.horizontalDistance()*4),.4F);}
    }
    void render(Minecraft client,float partial,float strength,PoseStack poses){
        float alpha=HallucinationTiming.lifeAlpha(age,maxAge)*strength;if(alpha<=.001F)return;
        Vec3 center=previousPosition.lerp(position,partial).subtract(client.gameRenderer.getMainCamera().getPosition());
        float facing=previousYaw+Mth.wrapDegrees(yaw-previousYaw)*partial;
        Arrays.sort(offsets,Comparator.comparingDouble((Vec3 offset)->center.add(offset.scale(scale)).lengthSqr()).reversed());
        ApparitionRender.drawEntity(client,entity,offsets,center,facing,partial,scale,red,green,blue,alpha,poses);
    }
}

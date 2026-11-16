/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.item.SmokeableItem;
import ivorius.psychedelicraft.particle.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SmokeEffects {
    private SmokeEffects(){}
    public static void playBreath(Player player){
        if(!player.level().isClientSide)player.level().playSound(null,player,PSSounds.BREATH.get(),SoundSource.PLAYERS,.02F,1.5F);
    }
    static void exhale(Player player,int color){
        RandomSource random=player.getRandom();if(random.nextBoolean())puff(player,color,1);if(random.nextInt(5)==0)puff(player,color,2.5F);
    }
    private static void puff(Player player,int color,float scale){
        Vec3 look=player.getLookAngle(),origin=player.getEyePosition().add(look.scale(.1)).add(0,-.1,0);
        Vec3 velocity=look.scale(player.getRandom().nextFloat()*.05+.1).add(player.getDeltaMovement()).add(0,.03,0);
        send((ServerLevel)player.level(),origin,velocity,color,scale);
    }
    private static void send(ServerLevel level,Vec3 origin,Vec3 velocity,int color,float scale){
        level.sendParticles(new SmokeParticleOptions(color,scale),origin.x,origin.y,origin.z,0,velocity.x,velocity.y,velocity.z,1);
    }
    public static void fuelConsumed(AbstractFurnaceBlockEntity furnace){
        if(!(furnace.getLevel() instanceof ServerLevel level)||!(furnace.getItem(1).getItem() instanceof SmokeableItem item))return;
        BlockPos pos=furnace.getBlockPos();
        for(Player player:level.getEntitiesOfClass(Player.class,new AABB(pos).inflate(3),player->!player.isSpectator()))item.applyInfluences(player,DrugCapability.get(player));
        RandomSource random=level.random;
        for(int i=0;i<30;i++){
            double x=pos.getX()+.5+triangular(random),z=pos.getZ()+.5+triangular(random);
            Vec3 origin=new Vec3(x+random.nextGaussian()*(float)triangular(random),pos.getY()+1+random.nextGaussian()*(float)(.8+triangular(random)),z+random.nextGaussian()*(float)triangular(random));
            send(level,origin,new Vec3(random.nextGaussian()*.001F,random.nextGaussian()*.001F,random.nextGaussian()*.001F),item.smokeColor(),1);
        }
    }
    private static double triangular(RandomSource random){return .3*(random.nextDouble()-random.nextDouble());}
}

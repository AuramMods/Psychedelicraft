package ivorius.psychedelicraft.gametest;

import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraftforge.gametest.*;
import java.util.*;

/** Native block ticks, contact, power and hostile targeting across every mod fluid. */
@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class NativeConduitGameTests {
    private static final List<Runnable> CLEANUP=new ArrayList<>();
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    @GameTestGenerator public static Collection<TestFunction> everyFluidFamily(){
        var result=new ArrayList<TestFunction>();
        result.add(test("water",null));
        for(var family:PSFluids.all())result.add(test(family.profile.path,family));
        return result;
    }
    private static TestFunction test(String name,PSFluids.Family family){
        return new TestFunction("native_conduit","psychedelicrafttests:conduit_"+name,"psychedelicrafttests:world_fluid",160,0,true,h->exercise(h,name,family));
    }
    private static void exercise(GameTestHelper h,String name,PSFluids.Family family){
        WorldFluidGameTests.basin(h);var level=h.getLevel();var center=h.absolutePos(new BlockPos(8,5,8));
        for(var pos:BlockPos.betweenClosed(center.offset(-2,-2,-2),center.offset(2,2,2))){
            int x=pos.getX()-center.getX(),y=pos.getY()-center.getY(),z=pos.getZ()-center.getZ();
            boolean shell=Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))==2;
            boolean frame=shell&&(x==0||y==0||z==0);
            level.setBlock(pos,(frame?Blocks.PRISMARINE:shell?Blocks.GLASS:Blocks.WATER).defaultBlockState(),3);
            if(!shell&&family!=null)family.block.get().setFluid(level,pos,family.get().defaultFluidState(),new CompoundTag());
        }
        level.setBlock(center,Blocks.CONDUIT.defaultBlockState().setValue(ConduitBlock.WATERLOGGED,true),3);
        var conduit=(ConduitBlockEntity)level.getBlockEntity(center);
        var player=AdvancementGameTests.player(h);player.setPos(center.getX()+1.5,center.getY()-1,center.getZ()+.5);player.setNoGravity(true);player.setAirSupply(100);
        level.addNewPlayer(player);CLEANUP.add(()->level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED));
        // The fixture has no packet-listener tick; drive its normal player tick explicitly.
        h.onEachTick(player::doTick);
        var drowned=EntityType.DROWNED.create(level);drowned.setNoAi(true);drowned.setNoGravity(true);drowned.setPos(center.getX()-.5,center.getY()-1,center.getZ()+.5);level.addFreshEntity(drowned);CLEANUP.add(drowned::discard);
        boolean expected=family==null||family.waterLike();
        h.startSequence().thenWaitUntil(()->check(conduit.tickCount>=85,"native conduit receives real world ticks")).thenExecute(()->{
            LogUtils.getLogger().info("NATIVE_CONDUIT family={} active={} hunting={} playerWater={} playerType={} power={} drownedWater={} drownedType={} health={} ticks={}",name,conduit.isActive(),conduit.isHunting(),player.isInWaterOrRain(),player.getEyeInFluidType(),player.hasEffect(MobEffects.CONDUIT_POWER),drowned.isInWaterOrRain(),drowned.getEyeInFluidType(),drowned.getHealth(),conduit.tickCount);
            check(conduit.isActive()==expected&&conduit.isHunting()==expected,"native full-frame activation/hunting matches accepted water-like classification: "+name);
            check(player.hasEffect(MobEffects.CONDUIT_POWER)==expected,"native power reaches the actually submerged player: "+name);
            check((drowned.getHealth()<drowned.getMaxHealth())==expected,"native hostile targeting recognizes actual fluid contact: "+name);
            if(expected){
                check(player.getAirSupply()>=60,"native conduit breathing prevents continued air loss: "+name);
                float boosted=player.getDestroySpeed(Blocks.STONE.defaultBlockState());player.removeEffect(MobEffects.CONDUIT_POWER);
                check(Math.abs(boosted-player.getDestroySpeed(Blocks.STONE.defaultBlockState())*1.2F)<.0001F,"native conduit applies its own mining-speed multiplier: "+name);
            }
        }).thenSucceed();
    }
    @AfterBatch(batch="native_conduit") public static void cleanup(ServerLevel level){CLEANUP.forEach(Runnable::run);CLEANUP.clear();}
}

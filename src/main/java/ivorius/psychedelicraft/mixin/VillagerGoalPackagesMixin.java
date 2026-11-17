package ivorius.psychedelicraft.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import ivorius.psychedelicraft.entity.*;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.npc.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {
    @Shadow private static Pair<Integer,BehaviorControl<net.minecraft.world.entity.LivingEntity>> getMinimalLookBehavior(){return null;}
    @Inject(method="getWorkPackage",at=@At("HEAD"),cancellable=true)
    private static void psychedelicraft$dealerWork(VillagerProfession profession,float speed,
            CallbackInfoReturnable<ImmutableList<Pair<Integer,? extends BehaviorControl<? super Villager>>>> callback){
        if(profession!=PSTradeOffers.DRUG_DEALER.get())return;
        var job=net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE;
        callback.setReturnValue(ImmutableList.of(getMinimalLookBehavior(),
                Pair.of(5,new DealerBehaviors.WorkCycle(speed)),
                Pair.of(10,new ShowTradesToPlayer(400,1600)),
                Pair.of(10,SetLookAndInteract.create(net.minecraft.world.entity.EntityType.PLAYER,4)),
                Pair.of(2,SetWalkTargetFromBlockMemory.create(job,speed,9,100,1200)),Pair.of(99,UpdateActivityFromSchedule.create())));
    }

    @Inject(method="getCorePackage",at=@At("RETURN"),cancellable=true)
    private static void psychedelicraft$addictCore(VillagerProfession profession,float speed,
            CallbackInfoReturnable<ImmutableList<Pair<Integer,? extends BehaviorControl<? super Villager>>>> callback){
        if(profession==PSTradeOffers.DRUG_ADDICT.get())callback.setReturnValue(
                ImmutableList.<Pair<Integer,? extends BehaviorControl<? super Villager>>>builder()
                        .addAll(callback.getReturnValue()).add(Pair.of(7,new AddictBehaviors.Restock())).build());
    }
    @Inject(method="getIdlePackage",at=@At("RETURN"),cancellable=true)
    private static void psychedelicraft$addictStroll(VillagerProfession profession,float speed,
            CallbackInfoReturnable<ImmutableList<Pair<Integer,? extends BehaviorControl<? super Villager>>>> callback){
        if(profession==PSTradeOffers.DRUG_ADDICT.get())callback.setReturnValue(
                ImmutableList.<Pair<Integer,? extends BehaviorControl<? super Villager>>>builder()
                        .add(Pair.of(4,new AddictBehaviors.Stroll())).addAll(callback.getReturnValue()).build());
    }
}

package ivorius.psychedelicraft.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import ivorius.psychedelicraft.entity.*;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.npc.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {
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

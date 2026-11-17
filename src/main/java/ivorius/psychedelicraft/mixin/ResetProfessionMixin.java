package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.entity.PSTradeOffers;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResetProfession.class)
public abstract class ResetProfessionMixin {
    @Inject(method="create",at=@At("HEAD"),cancellable=true)
    private static void psychedelicraft$keepAddict(CallbackInfoReturnable<BehaviorControl<Villager>> callback){
        // Native reset rule with the same jobless exception as nitwits. Avoid targeting a compiler-generated lambda.
        callback.setReturnValue(BehaviorBuilder.create(context->context.group(context.absent(MemoryModuleType.JOB_SITE))
                .apply(context,unused->(level,villager,time)->{
                    var data=villager.getVillagerData();var profession=data.getProfession();
                    if(profession==PSTradeOffers.DRUG_ADDICT.get()||profession==VillagerProfession.NONE||profession==VillagerProfession.NITWIT
                            ||villager.getVillagerXp()!=0||data.getLevel()>1)return false;
                    villager.setVillagerData(data.setProfession(VillagerProfession.NONE));villager.refreshBrain(level);return true;
                })));
    }
}

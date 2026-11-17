package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.entity.PSTradeOffers;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method="mobInteract",at=@At("HEAD"),cancellable=true)
    private void psychedelicraft$feed(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> callback){
        var villager=(Villager)(Object)this;
        var stack=player.getItemInHand(hand);
        if(!villager.isAlive()||villager.isBaby()||!stack.is(PSItems.HASH_MUFFIN.get()))return;
        var profession=villager.getVillagerData().getProfession();
        if(profession==VillagerProfession.NONE||profession==VillagerProfession.NITWIT){
            if(villager.level() instanceof ServerLevel level){
                if(!player.getAbilities().instabuild)stack.shrink(1);
                villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
                villager.releasePoi(MemoryModuleType.JOB_SITE);
                villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                villager.setVillagerData(villager.getVillagerData().setProfession(PSTradeOffers.DRUG_ADDICT.get()).setLevel(1));
                villager.setVillagerXp(0);
                villager.refreshBrain(level);
                villager.playSound(SoundEvents.GENERIC_EAT,1+villager.getRandom().nextFloat(),.3F+villager.getRandom().nextFloat()*.7F);
                PSCriteria.feedVillager(player);
            }
            callback.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide));
        }else callback.setReturnValue(InteractionResult.CONSUME);
    }
    @Inject(method="rewardTradeXp",at=@At("RETURN"))
    private void psychedelicraft$tradeOverdose(MerchantOffer offer,CallbackInfo callback){
        var villager=(Villager)(Object)this;
        if(villager.getVillagerData().getProfession()==PSTradeOffers.DRUG_ADDICT.get())
            villager.hurt(PSDamageTypes.of(villager,PSDamageTypes.OVERDOSE),offer.getUses()*offer.getResult().getCount()+1);
    }
    @Inject(method="shouldIncreaseLevel",at=@At("HEAD"),cancellable=true)
    private void psychedelicraft$careerLimit(CallbackInfoReturnable<Boolean> callback){
        var villager=(Villager)(Object)this;var profession=villager.getVillagerData().getProfession();
        if(profession==PSTradeOffers.DRUG_ADDICT.get()||profession==PSTradeOffers.DRUG_DEALER.get()&&villager.getVillagerData().getLevel()>=3)
            callback.setReturnValue(false);
    }
}

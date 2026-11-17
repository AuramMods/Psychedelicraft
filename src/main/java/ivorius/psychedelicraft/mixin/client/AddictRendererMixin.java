package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.entity.AddictBehaviors;
import ivorius.psychedelicraft.entity.PSTradeOffers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntityRenderer.class)
public abstract class AddictRendererMixin {
    @ModifyVariable(method="setupRotations",at=@At("HEAD"),ordinal=1,argsOnly=true)
    private float psychedelicraft$shake(float bodyYaw,LivingEntity entity){
        return entity instanceof VillagerDataHolder villager&&villager.getVillagerData().getProfession()==PSTradeOffers.DRUG_ADDICT.get()
                ?bodyYaw+AddictBehaviors.shake(entity):bodyYaw;
    }
}

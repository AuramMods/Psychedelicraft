package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements FoodDataOwner {
    @Unique private Player psychedelicraft$player;
    @Override public void psychedelicraft$owner(Player player){psychedelicraft$player=player;}
    @ModifyVariable(method="addExhaustion",at=@At("HEAD"),argsOnly=true)
    private float psychedelicraft$exhaustion(float amount){
        var player=psychedelicraft$player;
        if(player==null||!player.isAlive()||player.isRemoved()||StomachFoodStats.packetSaturation(player.getFoodData())<=0)return amount;
        return amount*player.getCapability(DrugCapability.TYPE).map(properties->properties.getMultiplier(Drug::saturationExhaustionModifier)).orElse(1F);
    }
}

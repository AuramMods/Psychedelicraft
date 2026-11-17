package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.entity.drug.StomachFoodStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Health packets and native food scores carry the raw baseline, projected once by the client. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerFoodMixin {
    @Redirect(method="doTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/food/FoodData;getFoodLevel()I"))
    private int psychedelicraft$rawFood(FoodData food){return StomachFoodStats.packetFoodLevel(food);}
    @Redirect(method="doTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/food/FoodData;getSaturationLevel()F"))
    private float psychedelicraft$rawSaturation(FoodData food){return StomachFoodStats.packetSaturation(food);}
}

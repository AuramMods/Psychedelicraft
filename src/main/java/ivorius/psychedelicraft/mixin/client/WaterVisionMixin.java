/* Retain the native water-vision timer/curve when eyes are in water-like brews. */
package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.*;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LocalPlayer.class)
public abstract class WaterVisionMixin {
    @Redirect(method={"aiStep","getWaterVision"},at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean psychedelicraft$waterLikeEyes(LocalPlayer player,TagKey<Fluid> tag){
        return player.isEyeInFluid(tag)||tag==FluidTags.WATER&&PSFluids.isWaterLike(player.getEyeInFluidType());
    }
}

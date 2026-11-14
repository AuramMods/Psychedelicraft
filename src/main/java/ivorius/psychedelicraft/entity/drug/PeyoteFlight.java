package ivorius.psychedelicraft.entity.drug;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;

final class PeyoteFlight {
    private boolean granted;

    boolean isGranted() {
        return granted;
    }

    void restoreGrant(boolean granted) {
        this.granted = granted;
    }

    void tick(EntityPlayer player, double intensity) {
        PlayerCapabilities abilities = player.capabilities;
        if (player.isCreative() || player.isSpectator()) {
            granted = false;
            return;
        }
        boolean eligible = player.isEntityAlive() && intensity > 0 && StomachFoodStats.packetSaturation(player.getFoodStats()) > 0;
        if (eligible && granted && abilities.isFlying && !player.isRiding()) {
            float saturation = StomachFoodStats.changeSaturation(player, -saturationPerTick(intensity), player.ticksExisted % 20 == 0);
            eligible = saturation > 0F;
        }
        if (eligible && !abilities.allowFlying) {
            granted = true;
            abilities.allowFlying = true;
            player.sendPlayerAbilities();
        } else if (!eligible && granted) {
            granted = false;
            abilities.allowFlying = false;
            abilities.isFlying = false;
            player.sendPlayerAbilities();
        }
    }

    static float saturationPerTick(double intensity) {
        double secondsPerPoint = Math.max(1, (1 + 9 * intensity) / 2);
        return (float) (1 / (20 * secondsPerPoint));
    }
}

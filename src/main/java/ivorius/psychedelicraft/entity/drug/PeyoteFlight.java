package ivorius.psychedelicraft.entity.drug;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Abilities;

final class PeyoteFlight {
    private boolean granted;

    boolean isGranted() { return granted; }
    void restoreGrant(boolean value) { granted = value; }

    void tick(Player player, double intensity) {
        Abilities abilities = player.getAbilities();
        if (player.isCreative() || player.isSpectator()) {
            granted = false;
            return;
        }
        boolean eligible = player.isAlive() && intensity > 0 && player.getFoodData().getSaturationLevel() > 0;
        if (eligible && granted && abilities.flying && !player.isPassenger()) {
            float saturation = StomachFoodStats.changeSaturation(player, -saturationPerTick(intensity), player.tickCount % 20 == 0);
            eligible = saturation > 0;
        }
        if (eligible && !abilities.mayfly) {
            granted = true;
            abilities.mayfly = true;
            player.onUpdateAbilities();
        } else if (!eligible && granted) {
            granted = false;
            abilities.mayfly = false;
            abilities.flying = false;
            player.onUpdateAbilities();
        }
    }

    static float saturationPerTick(double intensity) {
        double secondsPerPoint = Math.max(1, (1 + 9 * intensity) / 2);
        return (float) (1 / (20 * secondsPerPoint));
    }
}

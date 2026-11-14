package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.ConduitPower;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class ConduitVision {
    private static EntityPlayerSP owner;
    private static int submergedTicks;

    private ConduitVision() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (owner != player) {
            owner = player;
            submergedTicks = 0;
        }
        if (player == null) return;
        int delta = player.isInsideOfMaterial(Material.WATER) ? (player.isSpectator() ? 10 : 1) : -10;
        submergedTicks = MathHelper.clamp(submergedTicks + delta, 0, 600);
    }

    public static boolean hasVision(EntityLivingBase entity, Potion nightVision) {
        return entity.isPotionActive(nightVision) || underwaterBrightness(entity) > 0F;
    }

    public static float brightness(EntityLivingBase entity, float partialTicks) {
        PotionEffect conventional = entity.getActivePotionEffect(MobEffects.NIGHT_VISION);
        if (conventional != null) {
            int duration = conventional.getDuration();
            return duration > 200 ? 1F : 0.7F + MathHelper.sin((duration - partialTicks) * (float) Math.PI * 0.2F) * 0.3F;
        }
        return underwaterBrightness(entity);
    }

    private static float underwaterBrightness(EntityLivingBase entity) {
        if (!entity.isPotionActive(ConduitPower.EFFECT) || !entity.isInsideOfMaterial(Material.WATER)) return 0F;
        if (entity != owner) return 1F;
        return Math.min(submergedTicks, 100) * 0.006F + Math.max(0, submergedTicks - 100) * 0.0008F;
    }
}

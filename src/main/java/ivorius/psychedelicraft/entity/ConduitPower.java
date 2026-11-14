package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class ConduitPower extends Potion {
    public static final Potion EFFECT = new ConduitPower().setPotionName("effect.psychedelicraft.conduit_power")
            .setRegistryName(Psychedelicraft.id("conduit_power"));
    private static final ResourceLocation ICON = Psychedelicraft.id("textures/mob_effect/conduit_power.png");

    private ConduitPower() {
        super(false, 1950417);
        setBeneficial();
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(EFFECT);
    }

    public static boolean allowsBreathing(EntityLivingBase entity, Potion waterBreathing) {
        return entity.isPotionActive(waterBreathing) || entity.isPotionActive(EFFECT);
    }

    public static boolean hasMiningBoost(EntityLivingBase entity, Potion haste) {
        return entity.isPotionActive(haste) || entity.isPotionActive(EFFECT);
    }

    public static PotionEffect miningBoost(EntityLivingBase entity, Potion haste) {
        PotionEffect conventional = entity.getActivePotionEffect(haste);
        PotionEffect conduit = entity.getActivePotionEffect(EFFECT);
        return conduit != null && (conventional == null || conduit.getAmplifier() > conventional.getAmplifier())
                ? conduit : conventional;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        drawIcon(x + 6, y + 7);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        drawIcon(x + 3, y + 3);
    }

    @SideOnly(Side.CLIENT)
    private static void drawIcon(int x, int y) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
    }
}

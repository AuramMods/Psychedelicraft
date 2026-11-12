package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.VillagerCareers;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class VillagerClothes implements LayerRenderer<EntityLivingBase> {
    private static final Set<RenderLivingBase<?>> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final ResourceLocation DEALER = texture(false, "drug_dealer");
    private static final ResourceLocation ADDICT = texture(false, "drug_addict");
    private static final ResourceLocation ZOMBIE_DEALER = texture(true, "drug_dealer");
    private static final ResourceLocation ZOMBIE_ADDICT = texture(true, "drug_addict");
    private final RenderLivingBase<?> renderer;

    private VillagerClothes(RenderLivingBase<?> renderer) {
        this.renderer = renderer;
    }

    @SubscribeEvent
    public static void attachLayer(RenderLivingEvent.Pre<?> event) {
        VillagerProfession profession = profession(event.getEntity());
        if ((profession == VillagerCareers.DEALER || profession == VillagerCareers.ADDICT)
                && INSTALLED.add(event.getRenderer())) {
            event.getRenderer().addLayer(new VillagerClothes(event.getRenderer()));
        }
    }

    public static float shake(EntityLivingBase entity, float yaw) {
        if (profession(entity) != VillagerCareers.ADDICT) return yaw;
        float health = 1F - entity.getHealth() / entity.getMaxHealth();
        float shake = (float) (Math.cos(entity.ticksExisted * 3.25D) * Math.PI * 0.4D * (1F + health * 8F));
        entity.hurtTime = Math.abs(shake) > 5F ? 1 : 0;
        return yaw + shake;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float age, float headYaw, float headPitch, float scale) {
        VillagerProfession profession = profession(entity);
        if (entity.isInvisible() || profession != VillagerCareers.DEALER && profession != VillagerCareers.ADDICT) return;
        boolean zombie = entity instanceof EntityZombieVillager;
        renderer.bindTexture(profession == VillagerCareers.DEALER ? zombie ? ZOMBIE_DEALER : DEALER : zombie ? ZOMBIE_ADDICT : ADDICT);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        try {
            renderer.getMainModel().render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale);
        } finally {
            GlStateManager.disableBlend();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }

    @Nullable
    private static VillagerProfession profession(EntityLivingBase entity) {
        if (entity instanceof EntityVillager) return ((EntityVillager) entity).getProfessionForge();
        if (entity instanceof EntityZombieVillager) return ((EntityZombieVillager) entity).getForgeProfession();
        return null;
    }

    private static ResourceLocation texture(boolean zombie, String name) {
        return Psychedelicraft.id("textures/entity/" + (zombie ? "zombie_villager" : "villager") + "/profession/" + name + ".png");
    }
}

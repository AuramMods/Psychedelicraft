package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import ivorius.psychedelicraft.entity.Riptide;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public final class TridentPoses implements LayerRenderer<AbstractClientPlayer> {
    private static final ResourceLocation SPIN_TEXTURE = Psychedelicraft.id("textures/entity/trident_riptide.png");
    private final RenderPlayer renderer;
    private final ModelRenderer box;

    private TridentPoses(RenderPlayer renderer) {
        this.renderer = renderer;
        ModelBase base = new ModelBase() {};
        base.textureWidth = 64;
        base.textureHeight = 64;
        box = new ModelRenderer(base, 0, 0);
        box.addBox(-8, -16, -8, 16, 32, 16);
    }

    public static void attach(RenderPlayer renderer) {
        renderer.addLayer(new TridentPoses(renderer));
    }

    public static void throwArm(ModelBiped model, Entity entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (!player.isHandActive() || player.getActiveItemStack().getItem() != PSItems.TRIDENT) return;
        EnumHandSide side = player.getActiveHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        ModelRenderer arm = side == EnumHandSide.RIGHT ? model.bipedRightArm : model.bipedLeftArm;
        arm.rotateAngleX = arm.rotateAngleX * 0.5F - (float) Math.PI;
        arm.rotateAngleY = 0;
    }

    public static void firstPerson(AbstractClientPlayer player, EnumHand hand, ItemStack stack, float partialTicks) {
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        int direction = side == EnumHandSide.RIGHT ? 1 : -1;
        if (player.isHandActive() && player.getActiveHand() == hand && stack.getItem() == PSItems.TRIDENT) {
            float elapsed = stack.getMaxItemUseDuration() - (player.getItemInUseCount() - partialTicks + 1);
            float progress = MathHelper.clamp(elapsed / 10, 0, 1);
            GlStateManager.translate(-0.5F * direction, 0.7F, 0.1F);
            GlStateManager.rotate(-55, 1, 0, 0);
            GlStateManager.rotate(direction * 35.3F, 0, 1, 0);
            GlStateManager.rotate(direction * -9.785F, 0, 0, 1);
            if (progress > 0.1F) {
                GlStateManager.translate(0, MathHelper.sin((elapsed - 0.1F) * 1.3F) * (progress - 0.1F) * 0.004F, 0);
            }
            GlStateManager.translate(0, 0, progress * 0.2F);
            GlStateManager.scale(1, 1, 1 + progress * 0.2F);
            GlStateManager.rotate(direction * -45, 0, 1, 0);
        } else if (Riptide.isSpinning(player)) {
            GlStateManager.translate(-0.4F * direction, 0.8F, 0.3F);
            GlStateManager.rotate(direction * 65, 0, 1, 0);
            GlStateManager.rotate(direction * -85, 0, 0, 1);
        }
    }

    public static void spinPlayer(EntityLivingBase entity, float age) {
        if (Riptide.isSpinning(entity)) {
            GlStateManager.rotate(-90 - entity.rotationPitch, 1, 0, 0);
            GlStateManager.rotate(age * -75, 0, 1, 0);
        } else if (Riptide.isCrawling(entity)) {
            GlStateManager.rotate(-90, 1, 0, 0);
            GlStateManager.translate(0, -1, 0.3F);
        }
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float stride, float strideAmount, float partialTicks,
                              float age, float yaw, float pitch, float scale) {
        if (!Riptide.isSpinning(player)) return;
        renderer.bindTexture(SPIN_TEXTURE);
        boolean wasCulled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableCull();
        try (Scope scope = DrugRenderer.beginWorldModel()) {
            for (int layer = 1; layer < 3; layer++) {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.rotate(age * -(45 + layer * 5), 0, 1, 0);
                    float size = 0.75F * layer;
                    GlStateManager.scale(size, size, size);
                    GlStateManager.translate(0, -0.2F + 0.6F * layer, 0);
                    box.render(scale);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        } finally {
            if (wasCulled) GlStateManager.enableCull();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}

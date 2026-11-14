package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.DrownedEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public final class DrownedRenderer extends RenderBiped<DrownedEntity> {
    private static final ResourceLocation SKIN = Psychedelicraft.id("textures/entity/drowned.png");
    private static final ResourceLocation OUTER_SKIN = Psychedelicraft.id("textures/entity/drowned_outer_layer.png");

    public DrownedRenderer(RenderManager manager) {
        super(manager, new DrownedModel(0, false), 0.5F);
        addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                modelLeggings = new DrownedModel(0.5F, true);
                modelArmor = new DrownedModel(1, true);
            }
        });
        addLayer(new LayerRenderer<DrownedEntity>() {
            private final DrownedModel outer = new DrownedModel(0.25F, false);

            @Override
            public void doRenderLayer(DrownedEntity drowned, float stride, float strideAmount, float partialTicks,
                                      float age, float yaw, float pitch, float scale) {
                if (drowned.isInvisible()) return;
                outer.setModelAttributes(mainModel);
                outer.setLivingAnimations(drowned, stride, strideAmount, partialTicks);
                bindTexture(OUTER_SKIN);
                outer.render(drowned, stride, strideAmount, age, yaw, pitch, scale);
            }

            @Override
            public boolean shouldCombineTextures() {
                return true;
            }
        });
    }

    @Override
    protected ResourceLocation getEntityTexture(DrownedEntity drowned) {
        return SKIN;
    }

    @Override
    protected void applyRotations(DrownedEntity drowned, float age, float yaw, float partialTicks) {
        super.applyRotations(drowned, age, yaw, partialTicks);
        float swim = drowned.swimProgress(partialTicks);
        if (swim > 0) {
            float pivot = drowned.height / 2;
            GlStateManager.translate(0, pivot, 0);
            GlStateManager.rotate(swim * (-10 - drowned.rotationPitch), 1, 0, 0);
            GlStateManager.translate(0, -pivot, 0);
        }
    }

    private static final class DrownedModel extends ModelZombie {
        private float swim;

        private DrownedModel(float expansion, boolean armor) {
            super(expansion, armor);
            if (armor) return;
            boxList.remove(bipedLeftArm);
            bipedLeftArm = new ModelRenderer(this, 32, 48);
            bipedLeftArm.addBox(-1, -2, -2, 4, 12, 4, expansion);
            bipedLeftArm.setRotationPoint(5, 2, 0);
            boxList.remove(bipedLeftLeg);
            bipedLeftLeg = new ModelRenderer(this, 16, 48);
            bipedLeftLeg.addBox(-2, 0, -2, 4, 12, 4, expansion);
            bipedLeftLeg.setRotationPoint(1.9F, 12, 0);
        }

        @Override
        public void setLivingAnimations(EntityLivingBase entity, float stride, float strideAmount, float partialTicks) {
            super.setLivingAnimations(entity, stride, strideAmount, partialTicks);
            swim = ((DrownedEntity) entity).swimProgress(partialTicks);
        }

        @Override
        public void setRotationAngles(float stride, float strideAmount, float age, float yaw, float pitch, float scale, Entity entity) {
            super.setRotationAngles(stride, strideAmount, age, yaw, pitch, scale, entity);
            DrownedEntity drowned = (DrownedEntity) entity;
            if (drowned.isArmsRaised() && drowned.getHeldItemMainhand().getItem() == PSItems.TRIDENT) {
                ModelRenderer arm = drowned.getPrimaryHand() == EnumHandSide.RIGHT ? bipedRightArm : bipedLeftArm;
                arm.rotateAngleX = arm.rotateAngleX * 0.5F - (float) Math.PI;
                arm.rotateAngleY = 0;
            }
            if (swim <= 0) return;
            float kick = swim * MathHelper.sin(age * 0.1F);
            float reach = (float) (-Math.PI * 4 / 5);
            bipedRightArm.rotateAngleX += swim * (reach - bipedRightArm.rotateAngleX) + kick * 0.35F;
            bipedLeftArm.rotateAngleX += swim * (reach - bipedLeftArm.rotateAngleX) - kick * 0.35F;
            bipedRightArm.rotateAngleZ += swim * (-0.15F - bipedRightArm.rotateAngleZ);
            bipedLeftArm.rotateAngleZ += swim * (0.15F - bipedLeftArm.rotateAngleZ);
            bipedRightLeg.rotateAngleX += kick * 0.55F;
            bipedLeftLeg.rotateAngleX -= kick * 0.55F;
            bipedHead.rotateAngleX = 0;
            bipedHeadwear.rotateAngleX = 0;
        }
    }
}

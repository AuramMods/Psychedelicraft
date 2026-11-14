package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.BeeEntity;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

final class BeeModel extends ModelBase {
    private static final float PI = (float) Math.PI;
    private static final float BODY_Y = 19F;
    private static final float FLAP_DEGREES_PER_TICK = 120.32113F;

    private final ModelRenderer root;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;
    private final ModelRenderer rightAntenna;
    private final ModelRenderer leftAntenna;
    private final ModelRenderer frontLegs;
    private final ModelRenderer middleLegs;
    private final ModelRenderer backLegs;
    private final ModelRenderer stinger;
    private float roll;

    BeeModel() {
        textureWidth = textureHeight = 64;
        root = new ModelRenderer(this);
        root.setRotationPoint(0F, BODY_Y, 0F);
        root.addChild(new ModelRenderer(this, 0, 0).addBox(-3.5F, -4F, -5F, 7, 7, 10));
        stinger = part(26, 7, 0F, 0F, 0F).addBox(0F, -1F, 5F, 0, 1, 2);
        leftAntenna = part(2, 0, 0F, -2F, -5F).addBox(1.5F, -2F, -3F, 1, 2, 3);
        rightAntenna = part(2, 3, 0F, -2F, -5F).addBox(-2.5F, -2F, -3F, 1, 2, 3);
        rightWing = part(0, 18, -1.5F, -4F, -3F);
        rightWing.addBox(-9F, 0F, 0F, 9, 0, 6, 0.001F);
        leftWing = part(0, 18, 1.5F, -4F, -3F);
        leftWing.mirror = true;
        leftWing.addBox(0F, 0F, 0F, 9, 0, 6, 0.001F);
        frontLegs = legs(1, -2F);
        middleLegs = legs(3, 0F);
        backLegs = legs(5, 2F);
    }

    private ModelRenderer part(int u, int v, float x, float y, float z) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.setRotationPoint(x, y, z);
        root.addChild(part);
        return part;
    }

    private ModelRenderer legs(int v, float z) {
        return part(26, v, 1.5F, 3F, z).addBox(-5F, 0F, 0F, 7, 2, 0);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float swing, float swingAmount, float partialTicks) {
        BeeEntity bee = (BeeEntity) entity;
        roll = bee.getRollAmount(partialTicks);
        stinger.showModel = !bee.hasStung();
    }

    @Override
    public void setRotationAngles(float swing, float swingAmount, float age, float yaw, float pitch, float scale, Entity entity) {
        BeeEntity bee = (BeeEntity) entity;
        boolean resting = bee.onGround && bee.motionX * bee.motionX + bee.motionY * bee.motionY + bee.motionZ * bee.motionZ < 1.0E-7D;
        rightWing.rotateAngleY = resting ? -0.2618F : 0F;
        leftWing.rotateAngleY = -rightWing.rotateAngleY;
        rightWing.rotateAngleZ = resting ? 0F : MathHelper.cos(age * FLAP_DEGREES_PER_TICK * (PI / 180F)) * PI * 0.15F;
        leftWing.rotateAngleZ = -rightWing.rotateAngleZ;
        root.rotationPointY = BODY_Y;
        root.rotateAngleX = 0F;
        leftAntenna.rotateAngleX = rightAntenna.rotateAngleX = 0F;
        frontLegs.rotateAngleX = middleLegs.rotateAngleX = backLegs.rotateAngleX = resting ? 0F : PI * 0.25F;
        if (!resting && !bee.isAngry()) {
            float bob = MathHelper.cos(age * 0.18F);
            root.rotationPointY -= bob * 0.9F;
            root.rotateAngleX = 0.1F + bob * PI * 0.025F;
            leftAntenna.rotateAngleX = rightAntenna.rotateAngleX = bob * PI * 0.03F;
            frontLegs.rotateAngleX = PI * 0.125F - bob * PI * 0.1F;
            backLegs.rotateAngleX -= bob * PI * 0.05F;
        }
        if (roll > 0F) {
            float delta = 3.0915928F - root.rotateAngleX;
            root.rotateAngleX += roll * delta;
        }
    }

    @Override
    public void render(Entity entity, float swing, float swingAmount, float age, float yaw, float pitch, float scale) {
        if (isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0F, 24F * scale, 0F);
        }
        try {
            root.render(scale);
        } finally {
            if (isChild) GlStateManager.popMatrix();
        }
    }
}

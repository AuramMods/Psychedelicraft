package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.ThrownTridentEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import java.util.Collections;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class TridentRenderer extends Render<ThrownTridentEntity> {
    private static final ResourceLocation TEXTURE = Psychedelicraft.id("textures/entity/trident.png");
    private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    private static final ModelResourceLocation ICON = model("trident");
    private static final ModelResourceLocation HELD = model("trident_in_hand");
    private static final ModelResourceLocation THROWING = model("trident_throwing");
    private static final TridentMesh MESH = new TridentMesh();

    public TridentRenderer(RenderManager manager) {
        super(manager);
    }

    public static void registerItemRenderers() {
        ModelBakery.registerItemVariants(PSItems.TRIDENT, HELD, THROWING);
        PSItems.TRIDENT.setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            @Override
            public void renderByItem(ItemStack stack, float partialTicks) {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.scale(1, -1, -1);
                    renderMesh(null, stack.getItem().hasEffect(stack));
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        });
    }

    @SubscribeEvent
    public static void bake(ModelBakeEvent event) {
        IBakedModel icon = event.getModelRegistry().getObject(ICON);
        IBakedModel held = event.getModelRegistry().getObject(HELD);
        IBakedModel throwing = event.getModelRegistry().getObject(THROWING);
        event.getModelRegistry().putObject(ICON, new TridentDisplay(icon, held, throwing));
    }

    private static ModelResourceLocation model(String name) {
        return new ModelResourceLocation(Psychedelicraft.id(name), "inventory");
    }

    @Override
    public void doRender(ThrownTridentEntity trident, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(interpolate(trident.prevRotationYaw, trident.rotationYaw, partialTicks) - 90, 0, 1, 0);
            GlStateManager.rotate(interpolate(trident.prevRotationPitch, trident.rotationPitch, partialTicks) + 90, 0, 0, 1);
            renderMesh(trident, trident.hasFoil());
        } finally {
            GlStateManager.popMatrix();
        }
        super.doRender(trident, x, y, z, yaw, partialTicks);
    }

    private static float interpolate(float from, float to, float partial) {
        return from + MathHelper.wrapDegrees(to - from) * partial;
    }

    private static void renderMesh(Entity entity, boolean foil) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(TEXTURE);
        DrugRenderer.drawEntityModel(MESH, entity, 0, 0, 0, 0, 0, 1F / 16, false);
        if (foil) {
            minecraft.getTextureManager().bindTexture(GLINT);
            GlStateManager.depthFunc(514);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
            GlStateManager.color(0.5F, 0.25F, 0.8F, 1);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                GlStateManager.translate(Minecraft.getSystemTime() % 3000 / 3000F, 0, 0);
                GlStateManager.rotate(30, 0, 0, 1);
                GlStateManager.matrixMode(5888);
                DrugRenderer.drawEntityModel(MESH, entity, 0, 0, 0, 0, 0, 1F / 16, true);
            } finally {
                GlStateManager.matrixMode(5890);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(5888);
                GlStateManager.disableBlend();
                GlStateManager.depthMask(true);
                GlStateManager.depthFunc(515);
                GlStateManager.color(1, 1, 1, 1);
            }
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(ThrownTridentEntity trident) {
        return TEXTURE;
    }

    private static final class TridentDisplay extends BakedModelWrapper<IBakedModel> {
        private final IBakedModel held;
        private final IBakedModel throwing;
        private final ItemOverrideList overrides;

        private TridentDisplay(IBakedModel icon, IBakedModel held, IBakedModel throwing) {
            super(icon);
            this.held = held;
            this.throwing = throwing;
            overrides = new ItemOverrideList(Collections.emptyList()) {
                @Override
                public IBakedModel handleItemState(IBakedModel original, ItemStack stack, World world, EntityLivingBase entity) {
                    if (entity == null || !entity.isHandActive() || entity.getActiveItemStack() != stack) return original;
                    return new TridentDisplay(originalModel, TridentDisplay.this.throwing, TridentDisplay.this.throwing);
                }
            };
        }

        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType perspective) {
            switch (perspective) {
                case FIRST_PERSON_LEFT_HAND:
                case FIRST_PERSON_RIGHT_HAND:
                case THIRD_PERSON_LEFT_HAND:
                case THIRD_PERSON_RIGHT_HAND:
                    return held.handlePerspective(perspective);
                default:
                    return originalModel.handlePerspective(perspective);
            }
        }
    }

    private static final class TridentMesh extends ModelBase {
        private final ModelRenderer pole;

        private TridentMesh() {
            textureWidth = 32;
            textureHeight = 32;
            pole = new ModelRenderer(this, 0, 6);
            pole.addBox(-0.5F, 2, -0.5F, 1, 25, 1);
            pole.setTextureOffset(4, 0).addBox(-1.5F, 0, -0.5F, 3, 2, 1);
            pole.setTextureOffset(4, 3).addBox(-2.5F, -3, -0.5F, 1, 4, 1);
            pole.setTextureOffset(0, 0).addBox(-0.5F, -4, -0.5F, 1, 4, 1);
            pole.mirror = true;
            pole.setTextureOffset(4, 3).addBox(1.5F, -3, -0.5F, 1, 4, 1);
        }

        @Override
        public void render(Entity entity, float stride, float amount, float age, float yaw, float pitch, float scale) {
            pole.render(scale);
        }
    }
}

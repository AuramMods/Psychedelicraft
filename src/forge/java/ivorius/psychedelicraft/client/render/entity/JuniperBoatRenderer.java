package ivorius.psychedelicraft.client.render.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.ResourceLocation;

public final class JuniperBoatRenderer extends RenderBoat {
    private static final ResourceLocation HULL = Psychedelicraft.id("textures/entity/boat/juniper.png");
    private static final ResourceLocation CHEST = Psychedelicraft.id("textures/entity/chest_boat/juniper.png");

    public JuniperBoatRenderer(RenderManager manager, boolean chest) {
        super(manager);
        if (chest) {
            modelBoat = new ChestBoatModel(manager.renderEngine);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBoat boat) {
        return HULL;
    }

    private static final class ChestBoatModel extends ModelBoat {
        private final TextureManager textures;
        private final ModelRenderer bottom;
        private final ModelRenderer lid;
        private final ModelRenderer lock;

        private ChestBoatModel(TextureManager textures) {
            this.textures = textures;
            bottom = chestPart(76, -2, -5, -6, 12, 8, 12);
            lid = chestPart(59, -2, -9, -6, 12, 4, 12);
            lock = chestPart(59, -1, -6, -1, 2, 4, 1);
        }

        private ModelRenderer chestPart(int textureY, float x, float y, float z, int width, int height, int depth) {
            ModelRenderer part = new ModelRenderer(this, 0, textureY).setTextureSize(128, 128);
            part.addBox(0, 0, 0, width, height, depth);
            part.setRotationPoint(x, y, z);
            part.rotateAngleY = (float) (-Math.PI / 2);
            return part;
        }

        @Override
        public void render(Entity entity, float partialTicks, float limbSwingAmount, float ageInTicks,
                           float netHeadYaw, float headPitch, float scale) {
            super.render(entity, partialTicks, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            textures.bindTexture(CHEST);
            bottom.render(scale);
            lid.render(scale);
            lock.render(scale);
        }
    }
}

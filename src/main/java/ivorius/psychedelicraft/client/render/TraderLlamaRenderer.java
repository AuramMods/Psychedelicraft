package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.model.ModelLlama;
import net.minecraft.client.renderer.entity.RenderLlama;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.util.ResourceLocation;

public final class TraderLlamaRenderer extends RenderLlama {
    private static final ResourceLocation DECOR = Psychedelicraft.id("textures/entity/trader_llama.png");

    public TraderLlamaRenderer(RenderManager manager) {
        super(manager);
        addLayer(new TraderDecor());
    }

    private final class TraderDecor implements LayerRenderer<EntityLlama> {
        private final ModelLlama model = new ModelLlama(0.5F);

        @Override
        public void doRenderLayer(EntityLlama llama, float swing, float amount, float partialTicks, float age,
                                  float yaw, float pitch, float scale) {
            if (!llama.hasColor()) {
                bindTexture(DECOR);
                model.setModelAttributes(getMainModel());
                model.render(llama, swing, amount, age, yaw, pitch, scale);
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}

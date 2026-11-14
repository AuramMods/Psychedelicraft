package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.BeeEntity;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public final class BeeRenderer extends RenderLiving<BeeEntity> {
    private static final ResourceLocation BEE = Psychedelicraft.id("textures/entity/bee.png");
    private static final ResourceLocation ANGRY = Psychedelicraft.id("textures/entity/bee_angry.png");
    private static final ResourceLocation NECTAR = Psychedelicraft.id("textures/entity/bee_nectar.png");
    private static final ResourceLocation ANGRY_NECTAR = Psychedelicraft.id("textures/entity/bee_angry_nectar.png");

    public BeeRenderer(RenderManager manager) {
        super(manager, new BeeModel(), 0.4F);
    }

    @Override
    protected ResourceLocation getEntityTexture(BeeEntity bee) {
        if (bee.isAngry()) return bee.hasNectar() ? ANGRY_NECTAR : ANGRY;
        return bee.hasNectar() ? NECTAR : BEE;
    }
}

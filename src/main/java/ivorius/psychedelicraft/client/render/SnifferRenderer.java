package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.SnifferEntity;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public final class SnifferRenderer extends RenderLiving<SnifferEntity> {
    private static final ResourceLocation SKIN = Psychedelicraft.id("textures/entity/sniffer.png");

    public SnifferRenderer(RenderManager manager) {
        super(manager, new SnifferModel(), 1.1F);
    }

    @Override
    protected ResourceLocation getEntityTexture(SnifferEntity sniffer) {
        return SKIN;
    }
}

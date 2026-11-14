package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.WanderingTraderEntity;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public final class WanderingTraderRenderer extends RenderLiving<WanderingTraderEntity> {
    private static final ResourceLocation SKIN = Psychedelicraft.id("textures/entity/wandering_trader.png");

    public WanderingTraderRenderer(RenderManager manager) {
        super(manager, traderModel(), 0.5F);
    }

    private static ModelVillager traderModel() {
        ModelVillager model = new ModelVillager(0);
        ModelRenderer hood = new ModelRenderer(model, 32, 0).setTextureSize(64, 64);
        hood.addBox(-4, -10, -4, 8, 10, 8, 0.51F);
        model.villagerHead.addChild(hood);
        ModelRenderer rim = new ModelRenderer(model, 30, 47).setTextureSize(64, 64);
        rim.addBox(-8, -8, -6, 16, 16, 1);
        rim.rotateAngleX = -(float) Math.PI / 2;
        hood.addChild(rim);
        model.villagerBody.cubeList.clear();
        model.villagerBody.setTextureOffset(16, 20).addBox(-4, 0, -3, 8, 12, 6);
        model.villagerBody.setTextureOffset(0, 38).addBox(-4, 0, -3, 8, 20, 6, 0.5F);
        return model;
    }

    @Override
    protected ResourceLocation getEntityTexture(WanderingTraderEntity trader) {
        return SKIN;
    }
}

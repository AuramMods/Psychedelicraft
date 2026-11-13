package ivorius.psychedelicraft.client.render.block;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

final class JuniperHangingSignModel extends ModelBase {
    private static final float SCALE = 1F / 16F;
    private final ModelRenderer board;
    private final ModelRenderer plank;
    private final ModelRenderer attachedChains;
    private final ModelRenderer[] straightChains;

    JuniperHangingSignModel() {
        textureWidth = 64;
        textureHeight = 32;
        board = new ModelRenderer(this, 0, 12);
        board.addBox(-7, 0, -1, 14, 10, 2);
        plank = new ModelRenderer(this, 0, 0);
        plank.addBox(-8, -6, -2, 16, 2, 4);
        attachedChains = new ModelRenderer(this, 14, 6);
        attachedChains.addBox(-6, -6, 0, 12, 6, 0);
        straightChains = new ModelRenderer[] {chain(0, -5, -1), chain(6, -5, 1), chain(0, 5, -1), chain(6, 5, 1)};
    }

    private ModelRenderer chain(int textureX, float x, int angle) {
        ModelRenderer chain = new ModelRenderer(this, textureX, 6);
        chain.addBox(-1.5F, 0, 0, 3, 6, 0);
        chain.setRotationPoint(x, -6, 0);
        chain.rotateAngleY = angle * (float) Math.PI / 4;
        return chain;
    }

    void render(boolean wall, boolean attached) {
        board.render(SCALE);
        if (wall) {
            plank.render(SCALE);
        }
        if (attached) {
            attachedChains.render(SCALE);
        } else {
            for (ModelRenderer chain : straightChains) {
                chain.render(SCALE);
            }
        }
    }
}

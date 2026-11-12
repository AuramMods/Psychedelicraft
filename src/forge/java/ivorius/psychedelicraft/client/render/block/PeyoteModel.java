/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

final class PeyoteModel extends ModelBase {
    private final List<ModelRenderer> parts = new ArrayList<>();

    private PeyoteModel(int width) {
        textureWidth = width;
        textureHeight = 32;
    }

    private void part(int u, int v, float x, float y, float z, int width, int height, int depth,
                      float pivotX, float pivotY, float pivotZ, float pitch, float yaw, float roll) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, width, height, depth, true);
        part.setRotationPoint(pivotX, pivotY, pivotZ);
        part.rotateAngleX = pitch;
        part.rotateAngleY = yaw;
        part.rotateAngleZ = roll;
        parts.add(part);
    }

    void render() {
        for (ModelRenderer part : parts) {
            part.render(1F / 16F);
        }
    }

    static PeyoteModel stage0() {
        PeyoteModel model = new PeyoteModel(32);
        model.part(0, 0, -1F, -1.5F, -1F, 2, 2, 2,
                0F, 24F, 0F, 0.0371786F, 0.2230717F, 0F);
        return model;
    }

    static PeyoteModel stage1() {
        PeyoteModel model = new PeyoteModel(64);
        model.part(0, 0, -2F, -2F, -2F, 3, 3, 3,
                0F, 24F, 0, 0.0371786F, 0.1487144F, 0F);
        model.part(0, 7, 0F, -1F, -1.5F, 2, 2, 2,
                1F, 24F, 0F, 0.1858931F, -0.2230717F, 0F);
        model.part(0, 12, 0F, -1.5F, 0F, 2, 2, 2,
                0F, 24F, 1F, -0.2230717F, 0.3346075F, 0F);
        model.part(0, 17, 1F, 0F, 1F, 1, 1, 1,
                0F, 23F, 0F, 0F, -0.8551081F, 0F);
        return model;
    }

    static PeyoteModel stage2() {
        PeyoteModel model = new PeyoteModel(64);
        model.part(0, 0, -2F, -3F, -3F, 4, 4, 4,
                -1F, 24F, -1F, 0.1115358F, 0.1858931F, -0.1487144F);
        model.part(0, 9, 0F, -2.5F, -2F, 3, 3, 3,
                0.5F, 24F, -1F, 0.0371786F, -0.0371786F, 0.1115358F);
        model.part(0, 16, -1F, -2.5F, 0.5F, 3, 3, 3,
                0F, 24F, 0F, -0.1487144F, 0.3717861F, 0F);
        model.part(0, 23, -2.5F, -2F, 0F, 2, 2, 2,
                0F, 24F, 0F, 0F, 0.0371786F, -0.1115358F);
        model.part(0, 28, -1F, -1.5F, 0F, 2, 2, 2,
                3F, 24F, 0F, -0.2602503F, 0.6320364F, 0F);
        model.part(17, 0, 0F, -1F, 1F, 1, 1, 1,
                0F, 24F, 2F, 0F, -0.8922867F, 0.2974289F);
        model.part(17, 3, -1F, -1F, -1F, 1, 1, 1,
                -3F, 24F, -1F, 0.0743572F, 0.1487144F, 0F);
        return model;
    }

    static PeyoteModel stage3() {
        PeyoteModel model = new PeyoteModel(64);
        model.part(0, 0, -2F, -3F, -3F, 4, 4, 4,
                -1F, 24F, -1F, 0.1115358F, 0.1858931F, -0.1487144F);
        model.part(0, 9, 0F, -2.5F, -3F, 4, 3, 4,
                0.5F, 24F, -1F, 0.1858931F, -0.2602503F, 0.1115358F);
        model.part(0, 17, -2F, -2.5F, 0.5F, 4, 3, 4,
                0F, 24F, 0F, -0.1487144F, 0.3717861F, 0F);
        model.part(0, 25, -3.5F, -2F, 0F, 3, 3, 3,
                0F, 24F, 0F, -0.1487144F, -0.1335332F, -0.1115358F);
        model.part(17, 10, -1F, -2F, 0F, 3, 3, 3,
                3F, 24F, 0F, -0.2602503F, 0.6320364F, 0F);
        model.part(17, 0, 0F, -1.5F, 1F, 2, 2, 2,
                0F, 24F, 2F, 0F, -1.375609F, 0.2974289F);
        model.part(17, 5, -2F, -1F, -1F, 2, 2, 2,
                -3F, 24F, -1F, 0.0743572F, 0.2230717F, -0.2230717F);
        model.part(17, 16, 0F, -1.5F, -1F, 2, 2, 2,
                0F, 24F, -5F, 0.1487144F, -0.2602503F, 0F);
        model.part(17, 21, -1F, -1F, 0F, 1, 1, 1,
                -2F, 24F, -5F, 0F, -0.2602503F, -0.2974289F);
        model.part(17, 24, 0F, -1F, 0F, 1, 1, 1,
                0F, 24F, 5F, -0.1487144F, 0.1487144F, 0F);
        model.part(26, 0, 0F, -5F, -2.5F, 0, 2, 3,
                -1F, 24F, -1F, 0.111544F, 0.185895F, -0.1487195F);
        model.part(26, 0, -1.5F, -5F, -1F, 3, 2, 0,
                -1F, 24F, -1F, 0.111544F, 0.185895F, -0.1487195F);
        return model;
    }
}

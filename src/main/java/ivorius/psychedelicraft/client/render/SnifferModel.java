package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.SnifferEntity;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SnifferModel extends ModelBase {
    private final Map<String, Part> parts = new HashMap<>();
    private final Part root;
    private final Part head;

    SnifferModel() {
        textureWidth = textureHeight = 192;
        root = part("root", null, 0F, 5F, 0F);
        Part bone = part("bone", root, 0F, 0F, 0F);
        Part body = part("body", bone, 0F, 0F, 0F);
        body.box(62, 68, -12.5F, -14F, -20F, 25, 29, 40, 0F);
        body.box(62, 0, -12.5F, -14F, -20F, 25, 24, 40, .5F);
        body.box(87, 68, -12.5F, 12F, -20F, 25, 0, 40, 0F);
        String[] positions = {"front", "mid", "hind"};
        for (int i = 0; i < positions.length; i++) {
            leg(bone, "right_" + positions[i] + "_leg", 32, 87 + 18 * i, -7.5F, -15F + 15F * i);
            leg(bone, "left_" + positions[i] + "_leg", 0, 87 + 18 * i, 7.5F, -15F + 15F * i);
        }
        head = part("head", body, 0F, 6.5F, -19.48F);
        head.box(8, 15, -6.5F, -7.5F, -11.5F, 13, 18, 11, 0F);
        head.box(8, 4, -6.5F, 7.5F, -11.5F, 13, 0, 11, 0F);
        part("left_ear", head, 6.51F, -7.5F, -4.51F).box(2, 0, 0F, 0F, -3F, 1, 19, 7, 0F);
        part("right_ear", head, -6.51F, -7.5F, -4.51F).box(48, 0, -1F, 0F, -3F, 1, 19, 7, 0F);
        part("nose", head, 0F, -4.5F, -11.5F).box(10, 45, -6.5F, -2F, -9F, 13, 2, 9, 0F);
        part("lower_beak", head, 0F, 2.5F, -12.5F).box(10, 57, -6.5F, -7F, -8F, 13, 12, 9, 0F);
    }

    private Part part(String name, Part parent, float x, float y, float z) {
        Part part = new Part(this, x, y, z);
        parts.put(name, part);
        if (parent != null) parent.children.add(part);
        return part;
    }

    private void leg(Part parent, String name, int u, int v, float x, float z) {
        part(name, parent, x, 10F, z).box(u, v, -3.5F, -1F, -4F, 7, 10, 8, 0F);
    }

    @Override
    public void render(Entity entity, float swing, float amount, float age, float yaw, float pitch, float scale) {
        SnifferEntity sniffer = (SnifferEntity) entity;
        parts.values().forEach(Part::reset);
        head.rotate(pitch, yaw, 0F);
        SnifferEntity.Activity activity = sniffer.activity();
        clip(activity == SnifferEntity.Activity.SEARCHING ? "SNIFFER_SNIFF_SEARCH" : "SNIFFER_WALK",
                swing * .45F, MathHelper.clamp(amount * 100F, 0F, 1F));
        float elapsed = Math.max(0F, sniffer.activityTicks() + age - sniffer.ticksExisted) / 20F;
        switch (activity) {
            case SCENTING: clip("SNIFFER_SNIFFSNIFF", elapsed, 1F); break;
            case SNIFFING: clip("SNIFFER_LONGSNIFF", elapsed, 1F); break;
            case DIGGING: clip("SNIFFER_DIG", elapsed, 1F); break;
            case RISING: clip("SNIFFER_STAND_UP", elapsed, 1F); break;
            case HAPPY: clip("SNIFFER_HAPPY", elapsed, 1F); break;
            default: break;
        }

        GlStateManager.pushMatrix();
        try {
            if (sniffer.isChild()) {
                clip("BABY_TRANSFORM", 0F, 1F);
                GlStateManager.scale(.5F, .5F, .5F);
                GlStateManager.translate(0F, 24F * scale, 0F);
            }
            root.render(scale);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private void clip(String name, float time, float weight) {
        SnifferAnimation.CLIPS.get(name).apply(parts, time, weight);
    }

    static final class Part {
        private final ModelRenderer shape;
        private final List<Part> children = new ArrayList<>();
        private final float originX, originY, originZ;
        private float x, y, z;
        private float pitch, yaw, roll;
        private float scaleX, scaleY, scaleZ;

        private Part(ModelBase model, float x, float y, float z) {
            shape = new ModelRenderer(model);
            originX = x;
            originY = y;
            originZ = z;
        }

        private void box(int u, int v, float x, float y, float z, int width, int height, int depth, float inflate) {
            shape.setTextureOffset(u, v).addBox(x, y, z, width, height, depth, inflate);
        }

        private void reset() {
            x = originX;
            y = originY;
            z = originZ;
            pitch = yaw = roll = 0F;
            scaleX = scaleY = scaleZ = 1F;
        }

        void translate(float x, float y, float z) {
            this.x += x;
            this.y += y;
            this.z += z;
        }

        void rotate(float pitch, float yaw, float roll) {
            this.pitch += pitch;
            this.yaw += yaw;
            this.roll += roll;
        }

        void scale(float x, float y, float z) {
            scaleX += x;
            scaleY += y;
            scaleZ += z;
        }

        private void render(float scale) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x * scale, y * scale, z * scale);
                if (roll != 0F) GlStateManager.rotate(roll, 0F, 0F, 1F);
                if (yaw != 0F) GlStateManager.rotate(yaw, 0F, 1F, 0F);
                if (pitch != 0F) GlStateManager.rotate(pitch, 1F, 0F, 0F);
                GlStateManager.scale(scaleX, scaleY, scaleZ);
                shape.render(scale);
                for (Part child : children) child.render(scale);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }
}

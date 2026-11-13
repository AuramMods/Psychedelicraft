package ivorius.psychedelicraft.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

// Render-only geometry for the donor's species that are absent from vanilla 1.12.
enum ApparitionModels {
    ALLAY(0.35F, ApparitionModels::allay),
    BEE(0.7F, ApparitionModels::bee),
    AXOLOTL(0.75F, ApparitionModels::axolotl),
    CAMEL(1.7F, ApparitionModels::camel),
    DOLPHIN(0.9F, ApparitionModels::dolphin),
    WARDEN(0.9F, ApparitionModels::warden),
    GLOW_SQUID(0.8F, ApparitionModels::glowSquid),
    COD(0.5F, ApparitionModels::cod),
    SALMON(0.7F, ApparitionModels::salmon),
    TROPICAL_FISH(0.5F, ApparitionModels::tropicalFish);

    final float width;
    private final Supplier<VoxelModel> factory;
    private VoxelModel model;

    ApparitionModels(float width, Supplier<VoxelModel> factory) {
        this.width = width;
        this.factory = factory;
    }

    @Nullable
    static ApparitionModels forId(ResourceLocation id) {
        if (!"minecraft".equals(id.getResourceDomain())) return null;
        for (ApparitionModels type : values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(id.getResourcePath())) return type;
        }
        return null;
    }

    void draw(float age, float red, float green, float blue, float alpha) {
        if (model == null) model = factory.get();
        for (Part part : model.parts) {
            part.draw(age, red, green, blue, alpha);
        }
    }

    private static VoxelModel allay() {
        VoxelModel m = new VoxelModel();
        m.box(0x41cee9, -2.5F, 8, -2.5F, 5, 5, 5);
        m.box(0x259cc9, -1.5F, 4, -1, 3, 4, 2);
        m.box(0x73ddf0, -1.5F, 6, -1.1F, 3, 2, 1);
        m.eyes(0xf5ffff, 1, 10, -2.6F);
        for (int side : new int[]{-1, 1}) {
            m.joint(0x88e8fb, side * 1.5F, 7, 1, side < 0 ? -3 : 0, -5, 0, 3, 5, 1)
                    .wing(side);
            m.joint(0x38bddd, side * 2, 8, 0, side < 0 ? -1 : 0, -5, 0, 1, 5, 1)
                    .sway(side * 0.4F);
        }
        return m;
    }

    private static VoxelModel bee() {
        VoxelModel m = new VoxelModel();
        m.box(0xe5a22d, -3.5F, 3, -5, 7, 6, 10);
        m.box(0xffca54, -3.5F, 3.05F, -5.02F, 7, 6, 2);
        m.box(0x44301f, -3.6F, 3.1F, -1, 7, 6, 2);
        m.box(0x533627, -3.6F, 3.1F, 3, 7, 6, 2);
        m.box(0x3b2a27, -0.5F, 5.5F, 4.8F, 1, 1, 2);
        m.eyes(0x242940, 2, 5, -5.12F);
        for (int side : new int[]{-1, 1}) {
            m.box(0x3a2824, side * 2 - 0.5F, 8.5F, -4.8F, 1, 2, 1);
            m.joint(0xd6e5e6, side * 1.8F, 9, -1, side < 0 ? -7 : 0, 0, 0, 7, 1, 4)
                    .wing(side);
            for (int z = -3; z <= 3; z += 3) {
                m.box(0x5a3921, side * 2.8F - 0.4F, 1.5F, z, 1, 2, 1);
            }
        }
        return m;
    }

    private static VoxelModel axolotl() {
        VoxelModel m = new VoxelModel();
        m.box(0xefb2ba, -4, 2, -6, 8, 4, 5);
        m.box(0xe5a0ad, -3, 2, -1, 6, 4, 8);
        m.box(0xffc5ce, -4, 2, -6.05F, 8, 2, 2);
        m.eyes(0x4f2937, 2.4F, 4.3F, -6.1F);
        for (int side : new int[]{-1, 1}) {
            m.box(0xd65b88, side < 0 ? -7 : 4, 4.5F, -4.2F, 3, 1, 2);
            m.box(0xdc659b, side < 0 ? -6 : 4, 5, -2.5F, 2, 2, 1);
            m.box(0xee82a9, side < 0 ? -6 : 4, 5.4F, -5.2F, 2, 2, 1);
            m.joint(0xe5a0ad, side * 2.6F, 3, 0, side < 0 ? -2 : 0, -2, 0, 2, 2, 2).sway(side * 0.35F);
            m.joint(0xe5a0ad, side * 2.6F, 3, 5, side < 0 ? -2 : 0, -2, 0, 2, 2, 2).sway(side * -0.35F);
        }
        m.joint(0xe781ab, 0, 3, 6, -0.5F, -2, 0, 1, 5, 8).tail(0.38F);
        return m;
    }

    private static VoxelModel camel() {
        VoxelModel m = new VoxelModel();
        m.box(0xc6945b, -7, 17, -11, 14, 10, 23);
        m.box(0xd5a770, -5.5F, 25, 0, 11, 6, 10);
        m.box(0xc99661, -3.5F, 23, -13, 7, 11, 7);
        m.box(0xd9a96f, -4, 30, -16, 8, 6, 8);
        m.box(0xddb279, -3.5F, 29, -19, 7, 4, 6);
        m.box(0xb58058, -3, 29.2F, -19.08F, 6, 2, 2);
        m.eyes(0x493325, 2.8F, 33, -16.1F);
        for (int side : new int[]{-1, 1}) {
            m.box(0xc49263, side * 3 - 1, 35, -12, 2, 2, 2);
            m.joint(0xba8753, side * 4.8F, 19, -8, -2, -19, -2, 4, 19, 4).sway(side * 0.35F);
            m.joint(0xba8753, side * 4.8F, 19, 9, -2, -19, -2, 4, 19, 4).sway(side * -0.35F);
        }
        m.joint(0xa7774a, 0, 24, 12, -1, -9, 0, 2, 9, 2).tail(0.2F);
        return m;
    }

    private static VoxelModel dolphin() {
        VoxelModel m = new VoxelModel();
        m.box(0x6d899c, -4, 4, -6, 8, 6, 13);
        m.box(0xa8bcc1, -3.5F, 3.5F, -6, 7, 2, 13);
        m.box(0x7894a4, -3.5F, 5, -9, 7, 5, 5);
        m.box(0x7390a2, -2, 5, -13, 4, 2, 5);
        m.eyes(0x283745, 2.7F, 7, -9.1F);
        m.joint(0x52778b, 0, 9, 1, -0.5F, 0, -1, 1, 4, 4).rotate(0.43F, 0, 0);
        for (int side : new int[]{-1, 1}) {
            m.joint(0x5f7a90, side * 3.6F, 5, -1, side < 0 ? -5 : 0, -1, 0, 5, 1, 4)
                    .rotate(0, side * 0.38F, side * -0.34F);
        }
        Part peduncle = m.joint(0x6d8798, 0, 6, 6, -2, -2, 0, 4, 4, 8);
        peduncle.sway(0.22F);
        m.child(peduncle, 0x5c798f, 0, 0, 6, -6, -0.5F, 0, 12, 1, 4);
        return m;
    }

    private static VoxelModel warden() {
        VoxelModel m = new VoxelModel();
        m.box(0x1b3439, -6, 12, -4, 12, 14, 8);
        m.box(0x1a383f, -7, 25, -5, 14, 10, 9);
        m.box(0x19464a, -5, 23, -4.1F, 10, 2, 1);
        m.box(0x2e9a98, -2, 15, -4.3F, 4, 6, 1);
        m.box(0x213032, -3.5F, 26, -5.1F, 7, 3, 1);
        for (int side : new int[]{-1, 1}) {
            for (int y = 16; y <= 21; y += 2) {
                m.box(0x829e87, side < 0 ? -5 : 2, y, -4.6F, 3, 1, 1);
            }
            m.joint(0x285959, side * 6, 26, -0.5F, side < 0 ? -4 : 0, -19, -2, 4, 19, 5).sway(side * 0.3F);
            m.joint(0x1c3e44, side * 3.5F, 14, 0, -2.5F, -14, -2.5F, 5, 14, 5).sway(side * -0.26F);
            m.joint(0x438e87, side * 6.5F, 31, 0, side < 0 ? -5 : 0, -1, -0.5F, 5, 6, 1).wing(side * 0.3F);
        }
        return m;
    }

    private static VoxelModel glowSquid() {
        VoxelModel m = new VoxelModel();
        m.box(0x28827c, -5, 5, -5, 10, 12, 10);
        m.box(0x41a39a, -4.5F, 14, -4.5F, 9, 3, 9);
        m.eyes(0xa9f7d0, 2.3F, 8, -5.1F);
        m.box(0x9cf4c5, 2.9F, 12, -5.06F, 1, 1, 1);
        m.box(0x75cfb4, -3.7F, 13.5F, -5.05F, 1, 1, 1);
        m.box(0x6acaaf, -1.2F, 11.3F, -5.05F, 1, 1, 1);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4);
            Part tentacle = m.joint(0x4fae9e, MathHelper.cos(angle) * 4, 5, MathHelper.sin(angle) * 4,
                    -0.7F, -9, -0.5F, 1, 9, 1);
            tentacle.rotate(0, angle, 0);
            tentacle.sway(0.37F);
        }
        return m;
    }

    private static VoxelModel cod() {
        VoxelModel m = fish(0xb89465, 0xe1c699, 2, 4, 8);
        m.box(0x8f754e, -0.5F, 7, 0, 1, 2, 3);
        m.joint(0x9b7b54, 0, 5, 4, -0.5F, -2, 0, 1, 4, 3).tail(0.48F);
        return m;
    }

    private static VoxelModel salmon() {
        VoxelModel m = fish(0xc24f4e, 0xcfa69f, 3, 5, 12);
        m.box(0x687a64, -1.5F, 7, -5.5F, 3, 1, 10);
        m.box(0x576a60, -0.5F, 8, 0, 1, 2, 4);
        m.joint(0xa75658, 0, 5.5F, 6, -0.5F, -3, 0, 1, 6, 4).tail(0.53F);
        return m;
    }

    private static VoxelModel tropicalFish() {
        VoxelModel m = fish(0xec963c, 0xfac559, 2, 5, 5);
        m.box(0xffe3b0, -1.08F, 3.03F, -1, 2, 5, 1);
        m.box(0x5d544a, -1.1F, 3.05F, 1.1F, 2, 5, 1);
        m.box(0xa9713c, -0.5F, 7, -1, 1, 2, 3);
        m.joint(0xd17f3d, 0, 5.5F, 2, -0.5F, -2, 0, 1, 4, 3).tail(0.6F);
        return m;
    }

    private static VoxelModel fish(int body, int belly, int width, int height, int length) {
        VoxelModel m = new VoxelModel();
        float x = width / 2F;
        float z = length / 2F;
        m.box(body, -x, 3, -z, width, height, length);
        m.box(belly, -x, 2.9F, -z - 0.02F, width, 1, length);
        m.eyes(0x2d3134, Math.max(0.5F, x - 0.3F), 3 + height * 0.65F, -z - 0.05F);
        for (int side : new int[]{-1, 1}) {
            m.joint(belly, side * x, 4, -z / 2, side < 0 ? -2 : 0, -0.5F, 0, 2, 1, 2)
                    .rotate(0, side * 0.32F, side * -0.35F);
        }
        return m;
    }

    private static final class VoxelModel extends ModelBase {
        private final List<Part> parts = new ArrayList<>();

        void box(int color, float x, float y, float z, int width, int height, int depth) {
            joint(color, 0, 0, 0, x, y, z, width, height, depth);
        }

        void eyes(int color, float spacing, float y, float z) {
            box(color, -spacing - 0.5F, y, z, 1, 1, 1);
            box(color, spacing - 0.5F, y, z, 1, 1, 1);
        }

        Part joint(int color, float pivotX, float pivotY, float pivotZ, float x, float y, float z,
                   int width, int height, int depth) {
            ModelRenderer renderer = new ModelRenderer(this);
            renderer.setTextureOffset(0, 0).addBox(x, y, z, width, height, depth);
            renderer.setRotationPoint(pivotX, pivotY, pivotZ);
            Part part = new Part(renderer, color);
            parts.add(part);
            return part;
        }

        void child(Part parent, int color, float pivotX, float pivotY, float pivotZ, float x, float y, float z,
                   int width, int height, int depth) {
            Part part = joint(color, pivotX, pivotY, pivotZ, x, y, z, width, height, depth);
            parts.remove(part);
            parent.children.add(part);
        }
    }

    private static final class Part {
        private final ModelRenderer renderer;
        private final int color;
        private final List<Part> children = new ArrayList<>(1);
        private float pitch;
        private float yaw;
        private float roll;
        private float sway;
        private float tail;
        private float wing;

        private Part(ModelRenderer renderer, int color) {
            this.renderer = renderer;
            this.color = color;
        }

        void rotate(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }

        void sway(float amount) {
            sway = amount;
        }

        void tail(float amount) {
            tail = amount;
        }

        void wing(float side) {
            wing = side;
        }

        void draw(float age, float red, float green, float blue, float alpha) {
            renderer.rotateAngleX = pitch + sway * MathHelper.sin(age * 0.28F);
            renderer.rotateAngleY = yaw + tail * MathHelper.sin(age * 0.44F);
            renderer.rotateAngleZ = roll + wing * (0.2F + MathHelper.cos(age * 1.9F) * 0.46F);
            GlStateManager.color(red * (color >> 16 & 255) / 255F, green * (color >> 8 & 255) / 255F,
                    blue * (color & 255) / 255F, alpha);
            renderer.render(0.0625F);
            if (!children.isEmpty()) {
                GlStateManager.pushMatrix();
                renderer.postRender(0.0625F);
                for (Part child : children) child.draw(age, red, green, blue, alpha);
                GlStateManager.popMatrix();
            }
        }
    }
}

package ivorius.psychedelicraft.client.render.shader;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

final class EntityRenderState {
    private final FloatBuffer values = BufferUtils.createFloatBuffer(16);
    private final Matrix4f viewToWorld = new Matrix4f();
    private boolean hasView;

    void captureView() {
        values.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, values);
        viewToWorld.load(values);
        hasView = viewToWorld.invert() != null;
        // ShaderUniform uploads matrices from row-major storage.
        if (hasView) viewToWorld.transpose();
    }

    void clearView() {
        hasView = false;
    }

    boolean hasView() {
        return hasView;
    }

    void uploadView(ShaderManager shader) {
        ShaderUniform matrix = shader.getShaderUniformOrDefault("EntityViewToWorld");
        matrix.set(viewToWorld);
        matrix.upload();
    }

    void upload(ShaderManager shader) {
        uploadView(shader);
        uploadTextures(shader);
        if (GL11.glIsEnabled(GL11.GL_LIGHTING)) {
            boolean normalize = GL11.glIsEnabled(GL11.GL_NORMALIZE) || GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
            scalar(shader, "EntityLighting", normalize ? 2F : 1F);
            boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
            scalar(shader, "EntityColorMaterial", colorMaterial ? 1F : 0F);
            if (!colorMaterial) {
                values.clear();
                GL11.glGetMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, values);
                vector(shader, "EntityMaterialAmbient");
                values.clear();
                GL11.glGetMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, values);
                color(shader, "EntityMaterialDiffuse", values.get(3));
            }
            values.clear();
            GL11.glGetFloat(GL11.GL_LIGHT_MODEL_AMBIENT, values);
            vector(shader, "EntityAmbient");
            uploadLight(shader, GL11.GL_LIGHT0, "EntityLight0", "EntityDiffuse0");
            uploadLight(shader, GL11.GL_LIGHT1, "EntityLight1", "EntityDiffuse1");
        } else {
            scalar(shader, "EntityLighting", 0F);
        }
    }

    private void uploadTextures(ShaderManager shader) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            boolean textured = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            scalar(shader, "EntityTextured", textured ? 1F : 0F);
            if (textured && combiner(GL11.GL_REPLACE) && env(GL13.GL_SOURCE0_RGB) == GL13.GL_CONSTANT) {
                readTextureColor();
                color(shader, "EntityOutline", 1F);
            } else {
                ShaderUniform outline = shader.getShaderUniformOrDefault("EntityOutline");
                outline.set(0F, 0F, 0F, 0F);
                outline.upload();
            }

            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            boolean lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            scalar(shader, "LightmapMode", lightmap ? 1F : 0F);
            if (lightmap && combiner(GL13.GL_INTERPOLATE) && env(GL13.GL_SOURCE0_RGB) == GL13.GL_CONSTANT) {
                readTextureColor();
                color(shader, "EntityOverlay", values.get(3));
            } else {
                ShaderUniform overlay = shader.getShaderUniformOrDefault("EntityOverlay");
                overlay.set(0F, 0F, 0F, 0F);
                overlay.upload();
            }

            GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
            if (GL11.glIsEnabled(GL11.GL_TEXTURE_2D) && combiner(GL11.GL_MODULATE)
                    && env(GL13.GL_SOURCE1_RGB) == GL13.GL_CONSTANT) {
                readTextureColor();
                color(shader, "EntityTint", values.get(3));
            } else {
                ShaderUniform tint = shader.getShaderUniformOrDefault("EntityTint");
                tint.set(1F, 1F, 1F, 1F);
                tint.upload();
            }
        } finally {
            GlStateManager.setActiveTexture(active);
        }
    }

    private void uploadLight(ShaderManager shader, int light, String direction, String diffuse) {
        if (GL11.glIsEnabled(light)) {
            values.clear();
            GL11.glGetLight(light, GL11.GL_POSITION, values);
            vector(shader, direction);
            values.clear();
            GL11.glGetLight(light, GL11.GL_DIFFUSE, values);
            vector(shader, diffuse);
        } else {
            ShaderUniform uniform = shader.getShaderUniformOrDefault(diffuse);
            uniform.set(0F, 0F, 0F);
            uniform.upload();
        }
    }

    private static boolean combiner(int mode) {
        return env(GL11.GL_TEXTURE_ENV_MODE) == GL13.GL_COMBINE && env(GL13.GL_COMBINE_RGB) == mode;
    }

    private static int env(int parameter) {
        return GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV, parameter);
    }

    private void readTextureColor() {
        values.clear();
        GL11.glGetTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, values);
    }

    private static void scalar(ShaderManager shader, String name, float value) {
        ShaderUniform uniform = shader.getShaderUniformOrDefault(name);
        uniform.set(value);
        uniform.upload();
    }

    private void vector(ShaderManager shader, String name) {
        ShaderUniform uniform = shader.getShaderUniformOrDefault(name);
        uniform.set(values.get(0), values.get(1), values.get(2));
        uniform.upload();
    }

    private void color(ShaderManager shader, String name, float alpha) {
        ShaderUniform uniform = shader.getShaderUniformOrDefault(name);
        uniform.set(values.get(0), values.get(1), values.get(2), alpha);
        uniform.upload();
    }
}

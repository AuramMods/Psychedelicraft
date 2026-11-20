package ivorius.psychedelicraft.client.render.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

/** One frame of LOD depth and the projections that actually produced it. */
final class DistantDepth {
    record Projection(float z, float translation) {}

    private Projection opaque;
    private Projection translucent;
    private boolean iris;
    private boolean opaqueCopied;
    private int depthTexture;
    private int opaqueTexture;
    private int copyFramebuffer;
    private int copyWidth;
    private int copyHeight;

    void beginFrame() {
        opaque = translucent = null;
        iris = opaqueCopied = false;
        depthTexture = 0;
    }

    void observeDraw(Object params, boolean solid) {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        Projection projection = readProjection(program, solid ? "dhProjection" : "iris_ProjectionMatrix");
        if (projection == null) projection = readProjection(program, solid ? "iris_ProjectionMatrix" : "dhProjection");
        if (projection != null) {
            iris = true;
        } else if (GL20.glGetUniformLocation(program, "uCombinedMatrix") >= 0 && !iris) {
            projection = nativeProjection(params);
        }
        if (projection == null) return;
        if (solid) opaque = projection;
        else translucent = projection;
    }

    private static Projection readProjection(int program, String name) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) return null;
        try (var stack = MemoryStack.stackPush()) {
            var matrix = stack.mallocFloat(16);
            GL20.glGetUniformfv(program, location, matrix);
            return matrix.get(11) == -1F ? new Projection(matrix.get(10), matrix.get(14)) : null;
        }
    }

    private static Projection nativeProjection(Object params) {
        try {
            var matrix = params.getClass().getField("dhProjectionMatrix").get(params);
            var values = (float[])matrix.getClass().getMethod("getValuesAsArray").invoke(matrix);
            // DH's public array is row-major, unlike the GLSL matrix readback above.
            return new Projection(values[10], values[11]);
        } catch (ReflectiveOperationException error) {
            return null;
        }
    }

    void finish() {
        if (projection() == null) return;
        try {
            var delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            Object proxy = delayed.getField("renderProxy").get(null);
            if (proxy == null) return;
            var result = proxy.getClass().getMethod("getDhDepthTextureId").invoke(proxy);
            if (result.getClass().getField("payload").get(result) instanceof Integer texture && texture > 0) {
                depthTexture = texture;
            }
        } catch (ReflectiveOperationException ignored) {
            // Older optional DH APIs can still render with the native near-depth path.
        }
    }

    void copyBeforeTranslucency(int source, int width, int height) {
        if (opaqueCopied || !iris || opaque == null || source <= 0 || width <= 0 || height <= 0) return;
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (opaqueTexture == 0) {
            opaqueTexture = GL11.glGenTextures();
            copyFramebuffer = GL30.glGenFramebuffers();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, opaqueTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, copyFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, source, 0);
        GL11.glReadBuffer(GL11.GL_NONE);
        try {
            if (GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) return;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, opaqueTexture);
            if (width != copyWidth || height != copyHeight) {
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, 0, 0, width, height, 0);
                copyWidth = width;
                copyHeight = height;
            } else {
                GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            }
            opaqueCopied = true;
        } finally {
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousFramebuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }

    Projection projection() {
        return opaque == null ? translucent : opaque;
    }

    Projection translucentProjection() {
        return translucent == null ? projection() : translucent;
    }

    int texture() {
        return depthTexture;
    }

    int opaqueTexture() {
        return iris && opaqueCopied && translucent != null && !translucent.equals(opaque) ? opaqueTexture : 0;
    }

    void release() {
        if (opaqueTexture != 0) GL11.glDeleteTextures(opaqueTexture);
        if (copyFramebuffer != 0) GL30.glDeleteFramebuffers(copyFramebuffer);
        opaqueTexture = copyFramebuffer = copyWidth = copyHeight = 0;
        beginFrame();
    }
}

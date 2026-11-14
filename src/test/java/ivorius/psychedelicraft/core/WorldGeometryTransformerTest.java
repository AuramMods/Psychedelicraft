package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WorldGeometryTransformerTest {
    private static int opens;
    private static int closes;

    @Test
    public void forgeCloudWrapperKeepsReturnValuesAndClosesItsScopeOnExceptions() throws Exception {
        Class<?> transformed = transformCloudFixture();
        Object renderer = transformed.getConstructor().newInstance();
        Method render = transformed.getMethod("render", int.class, float.class);
        opens = closes = 0;
        assertTrue((boolean) render.invoke(renderer, 42, 0.5F));
        assertFalse((boolean) render.invoke(renderer, 0, 0.5F));
        assertEquals(2, closes);

        try {
            render.invoke(renderer, -1, 0.5F);
            fail("Expected the cloud renderer's exception");
        } catch (InvocationTargetException error) {
            assertEquals("cloud draw failed", error.getCause().getMessage());
        }
        assertEquals(3, opens);
        assertEquals(3, closes);
    }

    @Test
    public void allWeatherFlushesUseTheParticleShaderHook() throws Exception {
        String name = EntityRenderer.class.getName();
        ClassNode renderer = new ClassNode();
        new ClassReader(new WorldGeometryTransformer().transform(name, name, classBytes(EntityRenderer.class)))
                .accept(renderer, 0);
        int weatherDraws = 0;
        for (MethodNode method : renderer.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("drawParticles".equals(call.name)) {
                    assertEquals("renderRainSnow", method.name);
                    assertEquals("(Lnet/minecraft/client/renderer/Tessellator;)V", call.desc);
                    weatherDraws++;
                }
            }
        }
        assertEquals(3, weatherDraws);
    }

    @Test
    public void overworldSkyKeepsBothBufferPathsAndSeparatesCelestialDraws() throws Exception {
        String name = RenderGlobal.class.getName();
        ClassNode renderer = new ClassNode();
        new ClassReader(new WorldGeometryTransformer().transform(name, name, classBytes(RenderGlobal.class)))
                .accept(renderer, 0);

        int bodies = 0;
        int starBuffers = 0;
        int starLists = 0;
        int skyBuffers = 0;
        int skyLists = 0;
        for (MethodNode method : renderer.methods) {
            if (!"renderSky".equals(method.name) || !"(FI)V".equals(method.desc)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                boolean stars = call.getPrevious().getOpcode() == org.objectweb.asm.Opcodes.ICONST_1;
                if ("drawCelestial".equals(call.name)) bodies++;
                if ("drawSkyBuffer".equals(call.name)) {
                    if (stars) starBuffers++;
                    else skyBuffers++;
                }
                if ("drawSkyList".equals(call.name)) {
                    if (stars) starLists++;
                    else skyLists++;
                }
            }
        }
        assertEquals(2, bodies);
        assertEquals(1, starBuffers);
        assertEquals(1, starLists);
        assertEquals(2, skyBuffers);
        assertEquals(3, skyLists);
    }

    private static byte[] classBytes(Class<?> type) throws Exception {
        try (InputStream bytes = type.getResourceAsStream("/" + Type.getInternalName(type) + ".class")) {
            return ByteStreams.toByteArray(bytes);
        }
    }

    private static Class<?> transformCloudFixture() throws Exception {
        String name = CloudFixture.class.getName();
        byte[] wrapped = new WorldGeometryTransformer().transform(name, "net.minecraftforge.client.CloudRenderer",
                classBytes(CloudFixture.class));
        ClassNode type = new ClassNode();
        new ClassReader(wrapped).accept(type, 0);
        // Exercise the generated wrapper without creating an OpenGL context.
        for (MethodNode method : type.methods) {
            if (!"render".equals(method.name)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("beginForgeClouds".equals(call.name) || "endClouds".equals(call.name)) {
                    call.owner = Type.getInternalName(WorldGeometryTransformerTest.class);
                }
            }
        }
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return new FixtureLoader().loadFixture(name, writer.toByteArray());
    }

    public static Scope beginForgeClouds(float partialTicks, int mode) {
        assertEquals(0.5F, partialTicks, 0F);
        assertEquals(2, mode);
        opens++;
        return null;
    }

    public static void endClouds(Scope scope) {
        closes++;
    }

    public static class CloudFixture {
        private final int cloudMode = 2;

        public boolean render(int cloudTicks, float partialTicks) {
            if (cloudTicks < 0) throw new IllegalStateException("cloud draw failed");
            return cloudTicks == 42 && partialTicks == 0.5F;
        }
    }

    private static final class FixtureLoader extends ClassLoader {
        Class<?> loadFixture(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}

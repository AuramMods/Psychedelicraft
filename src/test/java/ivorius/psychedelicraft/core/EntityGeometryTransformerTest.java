package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderEnderCrystal;
import net.minecraft.client.renderer.entity.RenderFish;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.client.renderer.entity.RenderTNTPrimed;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerEndermanEyes;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class EntityGeometryTransformerTest {
    @Test
    public void bodyArmorGlintAndEyesUseTheSharedModelHook() throws Exception {
        assertEquals(1, modelDraws(RenderLivingBase.class));
        assertEquals(3, modelDraws(LayerArmorBase.class));
        assertEquals(1, modelDraws(LayerEndermanEyes.class));
    }

    @Test
    public void boatsUseTheShaderForTheHullAndWaterMask() throws Exception {
        assertEquals(1, modelDraws(RenderBoat.class));
        assertEquals(1, boatMasks(transform(RenderBoat.class)));

        ClassNode productionNames = new ClassNode();
        new ClassReader(classBytes(RenderBoat.class)).accept(productionNames, 0);
        for (MethodNode method : productionNames.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("net/minecraft/client/model/IMultipassModel".equals(call.owner) && "renderMultipass".equals(call.name)) {
                    call.name = "func_187054_b";
                }
            }
        }
        ClassWriter writer = new ClassWriter(0);
        productionNames.accept(writer);
        assertEquals(1, boatMasks(transform(RenderBoat.class, writer.toByteArray())));
    }

    @Test
    public void worldRenderersKeepSeparateModelsBlockDrawsAndQuads() throws Exception {
        assertEquals(2, modelDraws(RenderEnderCrystal.class));
        assertEquals(1, modelDraws(RenderMinecart.class));
        assertEquals(1, calls(transform(RenderMinecart.class), "renderCartContents", "drawWorldBlock"));
        assertEquals(3, calls(transform(RenderTNTPrimed.class), "doRender", "drawWorldBlock"));
        assertEquals(3, calls(transform(RenderArrow.class), "doRender", "drawWorldQuads"));
        assertEquals(2, calls(transform(RenderFish.class), "doRender", "drawWorldQuads"));
    }

    @Test
    public void shadowsAndFireLeaveTheDebugDrawAlone() throws Exception {
        ClassNode renderer = transform(Render.class);
        assertEquals(1, calls(renderer, "renderShadowSingle", "shadowSurface"));
        assertEquals(1, calls(renderer, "renderShadow", "drawShadow"));
        assertEquals(1, calls(renderer, "renderEntityOnFire", "drawWorldQuads"));
        assertEquals(1, calls(renderer, "renderOffsetAABB", "draw"));
    }

    @Test
    public void nameplateKeepsSeparateBackgroundAndTextPasses() throws Exception {
        ClassNode renderer = transform(EntityRenderer.class);
        assertEquals(1, calls(renderer, "drawNameplate", "drawNameplateBackground"));
        assertEquals(2, calls(renderer, "drawNameplate", "drawNameplateText"));
        int expectedPass = Opcodes.ICONST_1;
        for (MethodNode method : renderer.methods) {
            if (!"drawNameplate".equals(method.name)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!"drawNameplateText".equals(call.name)) continue;
                assertEquals("(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;IIIZ)I", call.desc);
                assertEquals(expectedPass, call.getPrevious().getOpcode());
                expectedPass = Opcodes.ICONST_0;
            }
        }
    }

    private static int calls(ClassNode renderer, String methodName, String callName) {
        int count = 0;
        for (MethodNode method : renderer.methods) {
            if (!methodName.equals(method.name)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode && callName.equals(((MethodInsnNode) instruction).name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int boatMasks(ClassNode renderer) {
        int masks = 0;
        for (MethodNode method : renderer.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("drawBoatWaterMask".equals(call.name)) {
                    assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
                    assertEquals("renderMultipass", method.name);
                    assertEquals("(Lnet/minecraft/client/model/IMultipassModel;Lnet/minecraft/entity/Entity;FFFFFF)V",
                            call.desc);
                    masks++;
                }
            }
        }
        return masks;
    }

    private static int modelDraws(Class<?> type) throws Exception {
        int draws = 0;
        for (MethodNode method : transform(type).methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if ("drawEntityModel".equals(call.name)) {
                    assertEquals("(Lnet/minecraft/client/model/ModelBase;Lnet/minecraft/entity/Entity;FFFFFFZ)V",
                            call.desc);
                    assertEquals("renderEnchantedGlint".equals(method.name) ? Opcodes.ICONST_1 : Opcodes.ICONST_0,
                            instruction.getPrevious().getOpcode());
                    draws++;
                }
            }
        }
        return draws;
    }

    private static ClassNode transform(Class<?> type) throws Exception {
        return transform(type, classBytes(type));
    }

    private static ClassNode transform(Class<?> type, byte[] bytes) {
        ClassNode renderer = new ClassNode();
        byte[] transformed = new EntityGeometryTransformer().transform(type.getName(), type.getName(), bytes);
        new ClassReader(transformed).accept(renderer, 0);
        return renderer;
    }

    private static byte[] classBytes(Class<?> type) throws Exception {
        try (InputStream bytes = type.getResourceAsStream("/" + Type.getInternalName(type) + ".class")) {
            return ByteStreams.toByteArray(bytes);
        }
    }
}

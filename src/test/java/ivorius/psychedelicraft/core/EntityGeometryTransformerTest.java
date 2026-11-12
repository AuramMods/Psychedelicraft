package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerEndermanEyes;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
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

    private static int modelDraws(Class<?> type) throws Exception {
        ClassNode renderer = new ClassNode();
        try (InputStream bytes = type.getResourceAsStream("/" + Type.getInternalName(type) + ".class")) {
            byte[] transformed = new EntityGeometryTransformer().transform(type.getName(), type.getName(),
                    ByteStreams.toByteArray(bytes));
            new ClassReader(transformed).accept(renderer, 0);
        }
        int draws = 0;
        for (MethodNode method : renderer.methods) {
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
}

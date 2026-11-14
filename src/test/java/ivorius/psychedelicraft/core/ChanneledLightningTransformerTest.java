package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import net.minecraft.entity.effect.EntityLightningBolt;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChanneledLightningTransformerTest {
    @Test
    public void recordsVictimsThroughTheNativeStrikeLoopAfterTheForgeVeto() throws Exception {
        String name = EntityLightningBolt.class.getName();
        ClassNode lightning = new ClassNode();
        try (InputStream stream = EntityLightningBolt.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            assertNotNull(stream);
            byte[] patched = new ChanneledLightningTransformer().transform(name, name, ByteStreams.toByteArray(stream));
            new ClassReader(patched).accept(lightning, 0);
        }
        MethodNode update = ClientEffectsTransformer.method(lightning, "onUpdate", "func_70071_h_", "()V");
        MethodInsnNode select = findCall(update, "beginStrike");
        MethodInsnNode forge = findCall(update, "onEntityStruckByLightning");
        MethodInsnNode strike = findCall(update, "strike");
        assertEquals(Opcodes.INVOKESTATIC, select.getOpcode());
        assertEquals("(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;",
                select.desc);
        assertEquals(Opcodes.INVOKESTATIC, strike.getOpcode());
        assertEquals("(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/effect/EntityLightningBolt;)V", strike.desc);
        assertTrue(update.instructions.indexOf(select) < update.instructions.indexOf(forge));
        assertEquals(Opcodes.IFNE, forge.getNext().getOpcode());
        JumpInsnNode canceled = (JumpInsnNode) forge.getNext();
        assertTrue(update.instructions.indexOf(canceled) < update.instructions.indexOf(strike));
        assertTrue(update.instructions.indexOf(strike) < update.instructions.indexOf(canceled.label));
    }

    private static MethodInsnNode findCall(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode && name.equals(((MethodInsnNode) instruction).name)) {
                return (MethodInsnNode) instruction;
            }
        }
        throw new AssertionError("Missing " + name + " in " + method.name);
    }
}

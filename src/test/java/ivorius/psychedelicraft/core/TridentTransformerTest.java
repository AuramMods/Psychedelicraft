package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TridentTransformerTest {
    @Test
    public void nativePlayerSizeUsesThePoseHeightAndStillFiresTheForgeTick() throws Exception {
        ClassNode player = patchedPlayer();
        MethodNode size = ClientEffectsTransformer.method(player, "updateSize", "func_184808_cD", "()V");
        MethodInsnNode pose = findCall(size, "desiredHeight");
        assertEquals("(Lnet/minecraft/entity/player/EntityPlayer;FF)F", pose.desc);
        assertEquals(Opcodes.FSTORE, pose.getNext().getOpcode());
        assertEquals(((VarInsnNode) pose.getPrevious()).var, ((VarInsnNode) pose.getNext()).var);
        assertNotNull(findCall(size, "onPlayerPostTick"));
    }

    @Test
    public void nativeEyeHeightPassesTheChosenValueThroughTheCompactPoseHook() throws Exception {
        MethodNode eye = ClientEffectsTransformer.method(patchedPlayer(), "getEyeHeight", "func_70047_e", "()F");
        MethodInsnNode hook = findCall(eye, "eyeHeight");
        assertEquals("(FLnet/minecraft/entity/player/EntityPlayer;)F", hook.desc);
        assertEquals(Opcodes.FRETURN, hook.getNext().getOpcode());
    }

    private static ClassNode patchedPlayer() throws Exception {
        String name = EntityPlayer.class.getName();
        try (InputStream stream = EntityPlayer.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            assertNotNull(stream);
            byte[] patched = new TridentTransformer().transform(name, name, ByteStreams.toByteArray(stream));
            ClassNode node = new ClassNode();
            new ClassReader(patched).accept(node, 0);
            return node;
        }
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

package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityPistonRenderer;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class TileGeometryTransformerTest {
    @Test
    public void bothBeaconBeamPassesUseTheDedicatedHook() throws Exception {
        assertHooks(TileEntityBeaconRenderer.class, "drawBeaconBeam",
                "(Lnet/minecraft/client/renderer/Tessellator;)V", 2, false);
    }

    @Test
    public void pistonHookReceivesTheMovingTile() throws Exception {
        assertHooks(TileEntityPistonRenderer.class, "drawPiston",
                "(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/tileentity/TileEntityPiston;)V", 1, true);
    }

    private static void assertHooks(Class<?> renderer, String hook, String descriptor, int expectedDraws,
                                    boolean takesTile) throws Exception {
        ClassNode transformed = new ClassNode();
        String name = renderer.getName();
        try (InputStream bytes = renderer.getResourceAsStream("/" + Type.getInternalName(renderer) + ".class")) {
            new ClassReader(new TileGeometryTransformer().transform(name, name, ByteStreams.toByteArray(bytes)))
                    .accept(transformed, 0);
        }

        int draws = 0;
        for (MethodNode method : transformed.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!"ivorius/psychedelicraft/client/render/DrugRenderer".equals(call.owner)) continue;
                assertEquals(hook, call.name);
                assertEquals(descriptor, call.desc);
                assertEquals(INVOKESTATIC, call.getOpcode());
                if (takesTile) {
                    VarInsnNode tile = (VarInsnNode) call.getPrevious();
                    assertEquals(ALOAD, tile.getOpcode());
                    assertEquals(1, tile.var);
                }
                draws++;
            }
        }
        assertEquals(expectedDraws, draws);
    }
}

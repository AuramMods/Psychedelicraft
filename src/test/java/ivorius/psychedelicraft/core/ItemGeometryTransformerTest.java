package ivorius.psychedelicraft.core;

import com.google.common.io.ByteStreams;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.client.ForgeHooksClient;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ItemGeometryTransformerTest {
    @Test
    public void nativeAndForgeBatchesUseTheSameItemScope() throws Exception {
        assertEquals(1, countHooks(RenderItem.class.getName(), "drawItemQuads"));
        assertEquals(1, countHooks(ForgeHooksClient.class.getName(), "drawItemQuads"));
        assertEquals(1, countHooks(RenderItem.class.getName(), "renderBuiltInItem"));
    }

    @Test
    public void firstPersonAndBuiltInDrawsHaveIndependentHooks() throws Exception {
        assertEquals(1, countHooks(EntityRenderer.class.getName(), "renderFirstPersonItems"));
        assertEquals(4, countHooks(RenderPlayer.class.getName(), "drawHandPart"));
        assertEquals(4, countHooks(ModelRenderer.class.getName(), "drawBuiltInModelList"));
        assertEquals(1, countHooks(ItemRenderer.class.getName(), "drawMapQuads"));
        assertEquals(2, countHooks("net.minecraft.client.gui.MapItemRenderer$Instance", "drawMapQuads"));
    }

    @Test
    public void firstPersonTremorWrapsForgeAndVanillaHandDrawsOnce() throws Exception {
        String className = ItemRenderer.class.getName();
        ClassNode renderer = new ClassNode();
        try (InputStream bytes = ItemGeometryTransformerTest.class
                .getResourceAsStream("/" + className.replace('.', '/') + ".class")) {
            byte[] transformed = new CameraEffectsTransformer().transform(className, className,
                    ByteStreams.toByteArray(bytes));
            new ClassReader(transformed).accept(renderer, 0);
        }
        List<String> calls = new ArrayList<>();
        for (MethodNode method : renderer.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals("ivorius/psychedelicraft/client/render/CameraEffects")
                        || method.name.equals("renderItemInFirstPerson") && method.desc.equals("(F)V")
                        && (call.name.equals("rotateArm") || call.name.equals("renderSpecificFirstPersonHand")
                        || call.name.equals("renderItemInFirstPerson") || call.name.equals("disableRescaleNormal"))) {
                    calls.add(call.name);
                }
            }
        }
        assertEquals(Arrays.asList("rotateArm", "beginHands", "renderSpecificFirstPersonHand", "renderItemInFirstPerson",
                "renderSpecificFirstPersonHand", "renderItemInFirstPerson", "endHands", "disableRescaleNormal"), calls);
    }

    private static int countHooks(String className, String hook) throws Exception {
        ClassNode renderer = new ClassNode();
        try (InputStream bytes = ItemGeometryTransformerTest.class
                .getResourceAsStream("/" + className.replace('.', '/') + ".class")) {
            byte[] transformed = new ItemGeometryTransformer().transform(className, className,
                    ByteStreams.toByteArray(bytes));
            new ClassReader(transformed).accept(renderer, 0);
        }
        int draws = 0;
        for (MethodNode method : renderer.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode && hook.equals(((MethodInsnNode) instruction).name)) draws++;
            }
        }
        return draws;
    }
}

package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class GlitchLightingTransformer implements IClassTransformer {
    private static final String RENDERER = "net.minecraft.client.renderer.BlockModelRenderer";
    private static final String MODEL_RENDER = "(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !RENDERER.equals(transformedName)) return bytes;

        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode render = ClientEffectsTransformer.method(type, "renderModel", "func_187493_a", MODEL_RENDER);
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        boolean smooth = false;
        boolean flat = false;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!type.name.equals(call.owner) || !MODEL_RENDER.equals(remapper.mapMethodDesc(call.desc))) continue;
            String called = remapper.mapMethodName(call.owner, call.name, call.desc);
            String hook;
            if ("renderModelSmooth".equals(called) || "func_187498_b".equals(called)) {
                hook = "renderSmooth";
                smooth = true;
            } else if ("renderModelFlat".equals(called) || "func_187497_c".equals(called)) {
                hook = "renderFlat";
                flat = true;
            } else {
                continue;
            }
            render.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "ivorius/psychedelicraft/client/render/block/GlitchLighting", hook,
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
        }
        if (!smooth || !flat) throw new IllegalStateException("Could not locate the Forge block lighting calls");
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }
}

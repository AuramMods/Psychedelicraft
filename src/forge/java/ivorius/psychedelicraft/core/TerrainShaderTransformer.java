package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class TerrainShaderTransformer implements IClassTransformer {
    private static final String LAYER_DRAW = "(Lnet/minecraft/util/BlockRenderLayer;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.client.renderer.RenderGlobal".equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode layer = ClientEffectsTransformer.method(type, "renderBlockLayer", "func_174982_a", LAYER_DRAW);
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : layer.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            String method = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if ("net/minecraft/client/renderer/ChunkRenderContainer".equals(remapper.map(draw.owner))
                    && ("renderChunkLayer".equals(method) || "func_178001_a".equals(method))
                    && LAYER_DRAW.equals(remapper.mapMethodDesc(draw.desc))) {
                String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(draw.owner),
                        Type.getArgumentTypes(draw.desc)[0]);
                layer.instructions.set(draw, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "ivorius/psychedelicraft/client/render/DrugRenderer", "drawTerrain", descriptor, false));
                ClassWriter writer = new ClassWriter(0);
                type.accept(writer);
                return writer.toByteArray();
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 chunk layer draw");
    }
}

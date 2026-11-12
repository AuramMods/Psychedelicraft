package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class WorldGeometryTransformer implements IClassTransformer {
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String FORGE_CLOUDS = "net.minecraftforge.client.CloudRenderer";
    private static final String LAYER_DRAW = "(Lnet/minecraft/util/BlockRenderLayer;)V";
    private static final String VANILLA_CLOUD_DRAW = "(FIDDD)V";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/DrugRenderer";
    private static final String SCOPE = "ivorius/psychedelicraft/client/render/shader/WorldGeometryShader$Scope";
    private static final String SCOPE_TYPE = "L" + SCOPE + ";";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !RENDER_GLOBAL.equals(transformedName) && !FORGE_CLOUDS.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (RENDER_GLOBAL.equals(transformedName)) {
            patchTerrain(method(type, "renderBlockLayer", "func_174982_a", LAYER_DRAW));
            patchFastClouds(method(type, "renderClouds", "func_180447_b", VANILLA_CLOUD_DRAW));
            wrapClouds(type, method(type, "renderCloudsFancy", "func_180445_c", VANILLA_CLOUD_DRAW), false);
        } else {
            wrapClouds(type, method(type, "render", "render", "(IF)Z"), true);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchTerrain(MethodNode layer) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : layer.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if ("net/minecraft/client/renderer/ChunkRenderContainer".equals(remapper.map(draw.owner))
                    && ("renderChunkLayer".equals(name) || "func_178001_a".equals(name))
                    && LAYER_DRAW.equals(remapper.mapMethodDesc(draw.desc))) {
                String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(draw.owner),
                        Type.getArgumentTypes(draw.desc)[0]);
                layer.instructions.set(draw, hook("drawTerrain", descriptor));
                return;
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 chunk layer draw");
    }

    private static void patchFastClouds(MethodNode render) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if ("net/minecraft/client/renderer/Tessellator".equals(remapper.map(draw.owner))
                    && ("draw".equals(name) || "func_78381_a".equals(name)) && "()V".equals(draw.desc)) {
                render.instructions.insertBefore(draw, new VarInsnNode(Opcodes.DLOAD, 5));
                render.instructions.set(draw, hook("drawFastClouds", "(L" + draw.owner + ";D)V"));
                return;
            }
        }
        throw new IllegalStateException("Could not locate the fast cloud draw");
    }

    private static void wrapClouds(ClassNode type, MethodNode render, boolean forge) {
        MethodNode wrapper = new MethodNode(Opcodes.ASM5, render.access, render.name, render.desc, null, null);
        type.methods.add(wrapper);
        render.name = "psychedelicraft$renderClouds";
        render.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        InsnList code = wrapper.instructions;
        int scopeSlot = forge ? 3 : 9;
        if (forge) {
            code.add(new VarInsnNode(Opcodes.FLOAD, 2));
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, "cloudMode", "I"));
            code.add(hook("beginForgeClouds", "(FI)" + SCOPE_TYPE));
        } else {
            code.add(new VarInsnNode(Opcodes.DLOAD, 5));
            code.add(new InsnNode(Opcodes.ICONST_1));
            code.add(hook("beginVanillaClouds", "(DZ)" + SCOPE_TYPE));
        }
        code.add(new VarInsnNode(Opcodes.ASTORE, scopeSlot));

        // Scope both color and depth draws together, including restoration when rendering throws.
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        code.add(start);
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (Type arg : Type.getArgumentTypes(render.desc)) {
            code.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
            slot += arg.getSize();
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, type.name, render.name, render.desc, false));
        if (forge) code.add(new VarInsnNode(Opcodes.ISTORE, scopeSlot + 1));
        code.add(end);
        closeClouds(code, scopeSlot);
        if (forge) code.add(new VarInsnNode(Opcodes.ILOAD, scopeSlot + 1));
        code.add(new InsnNode(forge ? Opcodes.IRETURN : Opcodes.RETURN));
        code.add(handler);
        Object[] locals = forge ? new Object[]{type.name, Opcodes.INTEGER, Opcodes.FLOAT, SCOPE}
                : new Object[]{type.name, Opcodes.FLOAT, Opcodes.INTEGER, Opcodes.DOUBLE, Opcodes.DOUBLE, Opcodes.DOUBLE, SCOPE};
        code.add(new FrameNode(Opcodes.F_FULL, locals.length, locals, 1, new Object[]{"java/lang/Throwable"}));
        closeClouds(code, scopeSlot);
        code.add(new InsnNode(Opcodes.ATHROW));
        wrapper.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
    }

    private static void closeClouds(InsnList code, int scopeSlot) {
        code.add(new VarInsnNode(Opcodes.ALOAD, scopeSlot));
        code.add(hook("endClouds", "(" + SCOPE_TYPE + ")V"));
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}

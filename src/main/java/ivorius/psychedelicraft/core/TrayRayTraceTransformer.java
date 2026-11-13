package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;

public final class TrayRayTraceTransformer implements IClassTransformer {
    private static final String HOOKS = "ivorius/psychedelicraft/block/TrayRayTrace";
    private static final String VECTOR = "Lnet/minecraft/util/math/Vec3d;";
    private static final String HIT = "Lnet/minecraft/util/math/RayTraceResult;";
    private static final String STATE_AT = "(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.world.World".equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, ClassReader.EXPAND_FRAMES);
        MethodNode rayTrace = ClientEffectsTransformer.method(type, "rayTraceBlocks", "func_147447_a",
                "(" + VECTOR + VECTOR + "ZZZ)" + HIT);
        Type[] arguments = Type.getArgumentTypes(rayTrace.desc);
        Type result = Type.getReturnType(rayTrace.desc);
        int candidateSlot = rayTrace.maxLocals++;

        InsnList initialize = new InsnList();
        initialize.add(new InsnNode(Opcodes.ACONST_NULL));
        initialize.add(new VarInsnNode(Opcodes.ASTORE, candidateSlot));
        rayTrace.instructions.insert(initialize);

        int visits = 0;
        for (AbstractInsnNode instruction : rayTrace.instructions.toArray()) {
            if (instruction instanceof FrameNode) {
                FrameNode frame = (FrameNode) instruction;
                frame.local = new ArrayList<>(frame.local);
                int slots = 0;
                for (Object local : frame.local) {
                    slots += Opcodes.LONG.equals(local) || Opcodes.DOUBLE.equals(local) ? 2 : 1;
                }
                for (; slots < candidateSlot; slots++) frame.local.add(Opcodes.TOP);
                frame.local.add(result.getInternalName());
            } else if (instruction instanceof MethodInsnNode && isBlockLookup((MethodInsnNode) instruction)) {
                MethodInsnNode lookup = (MethodInsnNode) instruction;
                Type pos = Type.getArgumentTypes(lookup.desc)[0];
                InsnList visit = new InsnList();
                visit.add(new InsnNode(Opcodes.DUP2));
                visit.add(new VarInsnNode(Opcodes.ALOAD, 1));
                visit.add(new VarInsnNode(Opcodes.ALOAD, 2));
                visit.add(new VarInsnNode(Opcodes.ALOAD, candidateSlot));
                visit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "visitCell",
                        Type.getMethodDescriptor(result, Type.getObjectType(type.name), pos, arguments[0], arguments[1], result), false));
                visit.add(new VarInsnNode(Opcodes.ASTORE, candidateSlot));
                rayTrace.instructions.insertBefore(instruction, visit);
                visits++;
            } else if (instruction.getOpcode() == Opcodes.ARETURN) {
                InsnList nearest = new InsnList();
                nearest.add(new VarInsnNode(Opcodes.ALOAD, 2));
                nearest.add(new VarInsnNode(Opcodes.ALOAD, candidateSlot));
                nearest.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "nearest",
                        Type.getMethodDescriptor(result, result, arguments[1], result), false));
                rayTrace.instructions.insertBefore(instruction, nearest);
            }
        }
        if (visits != 2) throw new IllegalStateException("Could not locate the two Forge 1.12 ray trace block lookups");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isBlockLookup(MethodInsnNode call) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
        return ("getBlockState".equals(mapped) || "func_180495_p".equals(mapped))
                && STATE_AT.equals(remapper.mapMethodDesc(call.desc));
    }
}

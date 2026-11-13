package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class IntegratedShutdownTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.server.integrated.IntegratedServer".equals(transformedName)) return bytes;

        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode shutdown = ClientEffectsTransformer.method(type, "initiateShutdown", "func_71263_m", "()V");
        for (AbstractInsnNode instruction : shutdown.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() != Opcodes.INVOKESTATIC || !"com/google/common/util/concurrent/Futures".equals(call.owner)
                    || !"getUnchecked".equals(call.name) || !"(Ljava/util/concurrent/Future;)Ljava/lang/Object;".equals(call.desc)) continue;

            shutdown.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 0));
            shutdown.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "ivorius/psychedelicraft/client/IntegratedShutdown", "await",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(java.util.concurrent.Future.class),
                            Type.getObjectType(type.superName)), false));
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            type.accept(writer);
            return writer.toByteArray();
        }
        throw new IllegalStateException("Could not locate the integrated-server shutdown wait");
    }
}

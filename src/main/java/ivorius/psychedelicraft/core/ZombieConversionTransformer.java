package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class ZombieConversionTransformer implements IClassTransformer {
    private static final String LIVING = "net.minecraft.entity.EntityLivingBase";
    private static final String ZOMBIE = "net.minecraft.entity.monster.EntityZombie";
    private static final String RENDERER = "net.minecraft.client.renderer.entity.RenderLivingBase";
    private static final String HOOK = "ivorius/psychedelicraft/entity/ZombieConversion";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(LIVING.equals(transformedName) || ZOMBIE.equals(transformedName) || RENDERER.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (LIVING.equals(transformedName)) {
            MethodNode breathe = ClientEffectsTransformer.method(type, "canBreatheUnderwater", "func_70648_aU", "()Z");
            for (AbstractInsnNode instruction : breathe.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.IRETURN) continue;
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "breathesUnderwater",
                        "(L" + type.name + ";)Z", false));
                hook.add(new InsnNode(Opcodes.IOR));
                breathe.instructions.insertBefore(instruction, hook);
            }
        } else if (ZOMBIE.equals(transformedName)) {
            MethodNode initialize = ClientEffectsTransformer.method(type, "<clinit>", "<clinit>", "()V");
            for (AbstractInsnNode instruction : initialize.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.RETURN) {
                    initialize.instructions.insertBefore(instruction,
                            new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "reserveSyncedState", "()V", false));
                }
            }
        } else {
            MethodNode rotate = ClientEffectsTransformer.method(type, "applyRotations", "func_77043_a",
                    "(Lnet/minecraft/entity/EntityLivingBase;FFF)V");
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.FLOAD, 3));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "shake",
                    "(F" + Type.getArgumentTypes(rotate.desc)[0].getDescriptor() + ")F", false));
            hook.add(new VarInsnNode(Opcodes.FSTORE, 3));
            rotate.instructions.insert(hook);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }
}

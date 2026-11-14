package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class CampfireProjectileTransformer implements IClassTransformer {
    private static final String HOOKS = "ivorius/psychedelicraft/block/CampfireBlock";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        switch (transformedName) {
            case "net.minecraft.entity.projectile.EntityArrow":
            case "net.minecraft.entity.projectile.EntityFireball":
            case "net.minecraft.entity.projectile.EntityThrowable":
            case "net.minecraft.entity.projectile.EntityLlamaSpit":
                return patchImpact(bytes);
            default:
                return bytes;
        }
    }

    private static byte[] patchImpact(byte[] bytes) {
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode update = method(type, "onUpdate", "func_70071_h_", "()V");
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!"net/minecraftforge/event/ForgeEventFactory".equals(call.owner) || !"onProjectileImpact".equals(call.name)) continue;
            Type ray = Type.getArgumentTypes(call.desc)[1];
            String descriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getObjectType(type.superName), ray, Type.BOOLEAN_TYPE);
            update.instructions.insertBefore(call, new InsnNode(Opcodes.DUP2));
            update.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "afterProjectileImpact", descriptor, false));
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            type.accept(writer);
            return writer.toByteArray();
        }
        throw new IllegalStateException("Could not locate the Forge projectile impact hook in " + type.name);
    }
}

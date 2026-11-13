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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;

public final class SmokingPoseTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.client.model.ModelBiped".equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, ClassReader.EXPAND_FRAMES);
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            String mappedName = remapper.mapMethodName(type.name, method.name, method.desc);
            if (!("setRotationAngles".equals(mappedName) || "func_78087_a".equals(mappedName))
                    || !"(FFFFFFLnet/minecraft/entity/Entity;)V".equals(remapper.mapMethodDesc(method.desc))) continue;

            Type entityType = Type.getArgumentTypes(method.desc)[6];
            String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(type.name), entityType);
            // The release client drops the original entity argument from later stack frames.
            int entitySlot = method.maxLocals++;
            InsnList preserveEntity = new InsnList();
            preserveEntity.add(new VarInsnNode(Opcodes.ALOAD, 7));
            preserveEntity.add(new VarInsnNode(Opcodes.ASTORE, entitySlot));
            method.instructions.insert(preserveEntity);
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FrameNode) {
                    FrameNode frame = (FrameNode) instruction;
                    frame.local = new ArrayList<>(frame.local);
                    int slots = 0;
                    for (Object local : frame.local) {
                        slots += Opcodes.LONG.equals(local) || Opcodes.DOUBLE.equals(local) ? 2 : 1;
                    }
                    for (; slots < entitySlot; slots++) frame.local.add(Opcodes.TOP);
                    frame.local.add(entityType.getInternalName());
                }
                if (instruction.getOpcode() != Opcodes.RETURN) continue;
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new VarInsnNode(Opcodes.ALOAD, entitySlot));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "ivorius/psychedelicraft/client/render/SmokingPose", "apply", descriptor, false));
                method.instructions.insertBefore(instruction, call);
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            type.accept(writer);
            return writer.toByteArray();
        }
        throw new IllegalStateException("Could not locate Forge 1.12 ModelBiped.setRotationAngles");
    }
}

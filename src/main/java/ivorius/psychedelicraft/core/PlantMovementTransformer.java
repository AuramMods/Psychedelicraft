package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PlantMovementTransformer implements IClassTransformer {
    private static final String ENTITY = "net.minecraft.entity.Entity";
    private static final String MOVEMENT = "ivorius/psychedelicraft/entity/PlantMovement";
    private static final String PLANT = "psychedelicraft$inPlant";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !ENTITY.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        type.interfaces.add(MOVEMENT);
        type.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, PLANT, "Z", null, null));
        addPlantContact(type);
        patchWebMovement(type);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void addPlantContact(ClassNode type) {
        FieldNode web = ClientEffectsTransformer.field(type, "isInWeb", "field_70134_J");
        FieldNode fallDistance = ClientEffectsTransformer.field(type, "fallDistance", "field_70143_R");
        MethodNode contact = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "psychedelicraft$slowNextMove", "()V", null, null);
        contact.instructions.add(setFlag(type, PLANT, true));
        contact.instructions.add(setFlag(type, web.name, true));
        contact.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        contact.instructions.add(new InsnNode(Opcodes.FCONST_0));
        contact.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, type.name, fallDistance.name, fallDistance.desc));
        contact.instructions.add(new InsnNode(Opcodes.RETURN));
        type.methods.add(contact);

        MethodNode webContact = ClientEffectsTransformer.method(type, "setInWeb", "func_70110_aj", "()V");
        webContact.instructions.insert(setFlag(type, PLANT, false));
    }

    private static void patchWebMovement(ClassNode type) {
        MethodNode move = ClientEffectsTransformer.method(type, "move", "func_70091_d", "(Lnet/minecraft/entity/MoverType;DDD)V");
        FieldNode web = ClientEffectsTransformer.field(type, "isInWeb", "field_70134_J");
        int replaced = 0;
        boolean clearingWeb = false;
        for (AbstractInsnNode instruction : move.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.PUTFIELD && web.name.equals(((FieldInsnNode) instruction).name)) {
                clearingWeb = true;
            }
            if (!clearingWeb || !(instruction instanceof LdcInsnNode)) continue;
            Object value = ((LdcInsnNode) instruction).cst;
            String multiplier;
            if (Double.valueOf(0.25D).equals(value)) multiplier = "horizontalMultiplier";
            else if (Double.valueOf((double) 0.05F).equals(value)) multiplier = "verticalMultiplier";
            else continue;

            // Reuse the native one-shot slowdown and velocity reset, including its movement ordering.
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, PLANT, "Z"));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MOVEMENT, multiplier, "(Z)D", true));
            if (++replaced == 3) hook.add(setFlag(type, PLANT, false));
            move.instructions.insertBefore(instruction, hook);
            move.instructions.remove(instruction);
            if (replaced == 3) return;
        }
        throw new IllegalStateException("Could not locate the three Forge 1.12 cobweb movement multipliers");
    }

    private static InsnList setFlag(ClassNode type, String field, boolean value) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new InsnNode(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        hook.add(new FieldInsnNode(Opcodes.PUTFIELD, type.name, field, "Z"));
        return hook;
    }
}

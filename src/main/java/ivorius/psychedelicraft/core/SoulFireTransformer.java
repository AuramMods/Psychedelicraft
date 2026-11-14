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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class SoulFireTransformer implements IClassTransformer {
    private static final String WORLD = "net.minecraft.world.World";
    private static final String ENTITY = "net.minecraft.entity.Entity";
    private static final String PORTAL_SIZE = "net.minecraft.block.BlockPortal$Size";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String HOOKS = "ivorius/psychedelicraft/block/SoulFireBlock";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(WORLD.equals(transformedName) || ENTITY.equals(transformedName)
                || PORTAL_SIZE.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (WORLD.equals(transformedName)) {
            patchIgnition(type);
            MethodNode extinguish = method(type, "extinguishFire", "func_175719_a",
                    "(Lnet/minecraft/entity/player/EntityPlayer;" + POS + "Lnet/minecraft/util/EnumFacing;)Z");
            replaceFireComparison(extinguish, Type.getReturnType(findGetBlock(extinguish).desc));
        } else if (ENTITY.equals(transformedName)) {
            patchContact(type);
        } else {
            MethodNode empty = method(type, "isEmptyBlock", "func_150857_a", "(Lnet/minecraft/block/Block;)Z");
            replaceFireComparison(empty, Type.getArgumentTypes(empty.desc)[0]);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchIgnition(ClassNode type) {
        MethodNode set = method(type, "setBlockState", "func_180501_a", "(" + POS + STATE + "I)Z");
        Type[] args = Type.getArgumentTypes(set.desc);
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "stateForIgnition",
                Type.getMethodDescriptor(args[1], Type.getObjectType(type.name), args[0], args[1]), false));
        hook.add(new VarInsnNode(Opcodes.ASTORE, 2));
        set.instructions.insert(hook);
    }

    private static MethodInsnNode findGetBlock(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if ("()Lnet/minecraft/block/Block;".equals(REMAPPER.mapMethodDesc(call.desc))) return call;
        }
        throw new IllegalStateException("Could not locate the block read in " + method.name);
    }

    private static void replaceFireComparison(MethodNode method, Type block) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != Opcodes.GETSTATIC) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            if (!"net/minecraft/init/Blocks".equals(REMAPPER.map(field.owner))) continue;
            String name = REMAPPER.mapFieldName(field.owner, field.name, field.desc);
            if (!("FIRE".equals(name) || "field_150480_ab".equals(name))) continue;
            AbstractInsnNode comparison = field.getNext();
            while (comparison.getOpcode() < 0) comparison = comparison.getNext();
            int opcode = comparison.getOpcode();
            if (opcode != Opcodes.IF_ACMPNE && opcode != Opcodes.IF_ACMPEQ) break;
            method.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "isOpenFire",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, block), false));
            method.instructions.set(comparison, new JumpInsnNode(opcode == Opcodes.IF_ACMPNE ? Opcodes.IFEQ : Opcodes.IFNE,
                    ((JumpInsnNode) comparison).label));
            return;
        }
        throw new IllegalStateException("Could not locate the fire block check in " + method.name);
    }

    private static void patchContact(ClassNode type) {
        MethodNode move = method(type, "move", "func_70091_d", "(Lnet/minecraft/entity/MoverType;DDD)V");
        MethodInsnNode scan = null;
        for (AbstractInsnNode instruction : move.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = REMAPPER.mapMethodName(call.owner, call.name, call.desc);
            if ("isFlammableWithin".equals(name) || "func_147470_e".equals(name)) scan = call;
            if (scan == null || !("dealFireDamage".equals(name) || "func_70081_e".equals(name))) continue;
            AbstractInsnNode amount = call.getPrevious();
            if (amount.getOpcode() != Opcodes.ICONST_1) break;

            // Keep the exact shrunken bounding box passed to the native scan.
            int slot = move.maxLocals++;
            InsnList saveBox = new InsnList();
            saveBox.add(new InsnNode(Opcodes.DUP));
            saveBox.add(new VarInsnNode(Opcodes.ASTORE, slot));
            move.instructions.insertBefore(scan, saveBox);

            InsnList damage = new InsnList();
            FieldInsnNode world = findWorldRead(scan);
            damage.add(new VarInsnNode(Opcodes.ALOAD, 0));
            damage.add(new FieldInsnNode(Opcodes.GETFIELD, world.owner, world.name, world.desc));
            damage.add(new VarInsnNode(Opcodes.ALOAD, slot));
            damage.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "contactDamage",
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(world.desc), Type.getArgumentTypes(scan.desc)[0]), false));
            move.instructions.insertBefore(amount, damage);
            move.instructions.remove(amount);
            return;
        }
        throw new IllegalStateException("Could not locate the entity fire contact damage");
    }

    private static FieldInsnNode findWorldRead(MethodInsnNode scan) {
        for (AbstractInsnNode instruction = scan.getPrevious(); instruction != null; instruction = instruction.getPrevious()) {
            if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != Opcodes.GETFIELD) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            if (scan.owner.equals(Type.getType(field.desc).getInternalName())) return field;
        }
        throw new IllegalStateException("Could not locate the world for entity fire contact");
    }
}

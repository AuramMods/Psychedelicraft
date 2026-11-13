package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.field;
import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class LatticePistonTransformer implements IClassTransformer {
    private static final String HOOKS = "ivorius/psychedelicraft/block/LatticePiston";
    private static final String NBT = "Lnet/minecraft/nbt/NBTTagCompound;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String PLACE = "(Lnet/minecraft/util/math/BlockPos;" + STATE + "I)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.tileentity.TileEntityPiston".equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        patchWrite(type);
        patchRead(type);
        patchFinish(method(type, "update", "func_73660_a", "()V"));
        patchFinish(method(type, "clearPistonTileEntity", "func_145866_f", "()V"));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchWrite(ClassNode type) {
        MethodNode write = method(type, "writeToNBT", "func_189515_b", "(" + NBT + ")" + NBT);
        FieldNode moving = field(type, "pistonState", "field_174932_a");
        String tag = write.desc.substring(write.desc.indexOf(')') + 1);
        for (AbstractInsnNode instruction : write.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.ARETURN) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, moving.name, moving.desc));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "write", "(" + tag + moving.desc + ")" + tag, false));
            write.instructions.insertBefore(instruction, hook);
        }
    }

    private static void patchRead(ClassNode type) {
        MethodNode read = method(type, "readFromNBT", "func_145839_a", "(" + NBT + ")V");
        FieldNode moving = field(type, "pistonState", "field_174932_a");
        String tag = read.desc.substring(1, read.desc.indexOf(')'));
        for (AbstractInsnNode instruction : read.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.PUTFIELD || !moving.name.equals(((FieldInsnNode) instruction).name)) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "read", "(" + moving.desc + tag + ")" + moving.desc, false));
            read.instructions.insertBefore(instruction, hook);
            return;
        }
        throw new IllegalStateException("Could not locate the saved piston state assignment");
    }

    private static void patchFinish(MethodNode finish) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : finish.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!("setBlockState".equals(name) || "func_180501_a".equals(name))
                    || !PLACE.equals(remapper.mapMethodDesc(call.desc))) continue;
            finish.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "finish",
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
            return;
        }
        throw new IllegalStateException("Could not locate the final piston block placement");
    }
}

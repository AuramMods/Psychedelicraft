package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class BubbleColumnTransformer implements IClassTransformer {
    private static final String BOAT = "net.minecraft.entity.item.EntityBoat";
    private static final String MAGMA = "net.minecraft.block.BlockMagma";
    private static final String LIVING = "net.minecraft.entity.EntityLivingBase";
    private static final String RENDER = "net.minecraft.client.renderer.entity.RenderBoat";
    private static final String WORLD = "net.minecraft.world.World";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String TICK = "(Lnet/minecraft/world/World;" + POS + STATE + "Ljava/util/Random;)V";
    private static final String BLOCK_HOOKS = "ivorius/psychedelicraft/block/BubbleColumnBlock";
    private static final String BOAT_STATE = "ivorius/psychedelicraft/entity/BoatBubbleColumn";
    private static final String STATE_TYPE = "L" + BOAT_STATE + ";";
    private static final String COLUMN = "psychedelicraft$bubbleColumn";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(BOAT.equals(transformedName) || WORLD.equals(transformedName)
                || MAGMA.equals(transformedName) || LIVING.equals(transformedName)
                || (FMLLaunchHandler.side() == Side.CLIENT && RENDER.equals(transformedName)))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        switch (transformedName) {
            case BOAT:
                patchBoat(type);
                break;
            case WORLD:
                patchBlockChanges(type);
                break;
            case LIVING:
                patchBreathing(type);
                break;
            case MAGMA:
                MethodNode magma = method(type, "updateTick", "func_180650_b", TICK);
                magma.instructions.clear();
                magma.tryCatchBlocks.clear();
                magma.localVariables = null;
                addSupportTick(magma);
                break;
            case RENDER:
                patchRendering(type);
                break;
            default:
                throw new AssertionError(transformedName);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void addSupportTick(MethodNode tick) {
        Type[] args = Type.getArgumentTypes(tick.desc);
        tick.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        tick.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        tick.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BLOCK_HOOKS, "updateAbove",
                Type.getMethodDescriptor(Type.VOID_TYPE, args[0], args[1]), false));
        tick.instructions.add(new InsnNode(Opcodes.RETURN));
    }

    private static void patchBlockChanges(ClassNode type) {
        MethodNode notify = method(type, "markAndNotifyBlock", "markAndNotifyBlock",
                "(" + POS + "Lnet/minecraft/world/chunk/Chunk;" + STATE + STATE + "I)V");
        Type[] args = Type.getArgumentTypes(notify.desc);
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 3));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 4));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BLOCK_HOOKS, "onWaterChanged",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(type.name), args[0], args[2], args[3]), false));
        notify.instructions.insert(hook);
    }

    private static void patchBoat(ClassNode type) {
        type.interfaces.add(BOAT_STATE + "$Carrier");
        type.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, COLUMN, STATE_TYPE, null, null));
        Type boat = Type.getObjectType(type.name);
        MethodNode init = method(type, "entityInit", "func_70088_a", "()V");
        InsnList register = new InsnList();
        register.add(new VarInsnNode(Opcodes.ALOAD, 0));
        register.add(new TypeInsnNode(Opcodes.NEW, BOAT_STATE));
        register.add(new InsnNode(Opcodes.DUP));
        register.add(new VarInsnNode(Opcodes.ALOAD, 0));
        register.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, BOAT_STATE, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, boat), false));
        register.add(new FieldInsnNode(Opcodes.PUTFIELD, type.name, COLUMN, STATE_TYPE));
        for (AbstractInsnNode instruction : init.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.RETURN) continue;
            init.instructions.insertBefore(instruction, register);
            break;
        }

        MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, COLUMN, "()" + STATE_TYPE, null, null);
        loadColumn(type, getter.instructions);
        getter.instructions.add(new InsnNode(Opcodes.ARETURN));
        type.methods.add(getter);

        MethodNode update = method(type, "onUpdate", "func_70071_h_", "()V");
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (!matches(instruction, "doBlockCollisions", "func_145775_I", "()V")) continue;
            InsnList tick = new InsnList();
            loadColumn(type, tick);
            tick.add(new VarInsnNode(Opcodes.ALOAD, 0));
            tick.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BOAT_STATE, "tick",
                    Type.getMethodDescriptor(Type.VOID_TYPE, boat), false));
            update.instructions.insert(instruction, tick);
            return;
        }
        throw new IllegalStateException("Could not locate Forge boat collision tick");
    }

    private static void loadColumn(ClassNode type, InsnList body) {
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, COLUMN, STATE_TYPE));
    }

    private static void patchBreathing(ClassNode type) {
        MethodNode update = method(type, "onEntityUpdate", "func_70030_z", "()V");
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (!matches(instruction, "isInsideOfMaterial", "func_70055_a", "(Lnet/minecraft/block/material/Material;)Z")) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            update.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, BLOCK_HOOKS, "needsAir",
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
            return;
        }
        throw new IllegalStateException("Could not locate Forge underwater breathing");
    }

    private static void patchRendering(ClassNode type) {
        MethodNode rotation = method(type, "setupRotation", "func_188311_a",
                "(Lnet/minecraft/entity/item/EntityBoat;FF)V");
        Type boat = Type.getArgumentTypes(rotation.desc)[0];
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.FLOAD, 2));
        hook.add(new VarInsnNode(Opcodes.FLOAD, 3));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/client/render/entity/BoatBubbleRendering",
                "rotate", Type.getMethodDescriptor(Type.VOID_TYPE, boat, Type.FLOAT_TYPE, Type.FLOAT_TYPE), false));
        rotation.instructions.insert(hook);
    }

    private static boolean matches(AbstractInsnNode instruction, String name, String srg, String descriptor) {
        if (!(instruction instanceof MethodInsnNode)) return false;
        MethodInsnNode call = (MethodInsnNode) instruction;
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
        return (name.equals(mapped) || srg.equals(mapped)) && descriptor.equals(remapper.mapMethodDesc(call.desc));
    }
}

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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.field;
import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class BoatFlotationTransformer implements IClassTransformer {
    private static final String BOAT = "net.minecraft.entity.item.EntityBoat";
    private static final String LIVING = "net.minecraft.entity.EntityLivingBase";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String STATUS = "()Lnet/minecraft/entity/item/EntityBoat$Status;";
    private static final String HOOKS = "ivorius/psychedelicraft/entity/BoatFlotation";
    private static final String WATERLOGGING = "ivorius/psychedelicraft/fluid/physical/Waterlogging";
    private static final String POSITION = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(BOAT.equals(transformedName) || LIVING.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (BOAT.equals(transformedName)) patchFlotation(type);
        else patchDismount(type);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchFlotation(ClassNode type) {
        String boat = Type.getObjectType(type.name).getDescriptor();

        MethodNode above = method(type, "getWaterLevelAbove", "func_184451_k", "()F");
        FieldNode lastMotion = field(type, "lastYd", "field_184473_aH");
        InsnList height = new InsnList();
        height.add(new VarInsnNode(Opcodes.ALOAD, 0));
        height.add(new VarInsnNode(Opcodes.ALOAD, 0));
        height.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, lastMotion.name, lastMotion.desc));
        height.add(hook("waterLevelAbove", "(" + boat + "D)F"));
        height.add(new InsnNode(Opcodes.FRETURN));
        replaceBody(above, height);

        MethodNode underwater = method(type, "getUnderwaterStatus", "func_184444_v", STATUS);
        InsnList submersion = new InsnList();
        submersion.add(new VarInsnNode(Opcodes.ALOAD, 0));
        submersion.add(hook("underwaterStatus", "(" + boat + ")" + Type.getReturnType(underwater.desc)));
        submersion.add(new InsnNode(Opcodes.ARETURN));
        replaceBody(underwater, submersion);

        FieldNode waterLevel = field(type, "waterLevel", "field_184465_aD");
        MethodNode contact = method(type, "checkInWater", "func_184446_u", "()Z");
        InsnList surface = new InsnList();
        surface.add(new VarInsnNode(Opcodes.ALOAD, 0));
        surface.add(new VarInsnNode(Opcodes.ALOAD, 0));
        surface.add(hook("waterLevel", "(" + boat + ")D"));
        surface.add(new FieldInsnNode(Opcodes.PUTFIELD, type.name, waterLevel.name, waterLevel.desc));
        surface.add(new VarInsnNode(Opcodes.ALOAD, 0));
        surface.add(new VarInsnNode(Opcodes.ALOAD, 0));
        surface.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, waterLevel.name, waterLevel.desc));
        surface.add(hook("touchesWater", "(" + boat + "D)Z"));
        surface.add(new InsnNode(Opcodes.IRETURN));
        replaceBody(contact, surface);

        patchFallState(method(type, "updateFallState", "func_184231_a", "(DZ" + STATE + POSITION + ")V"));
    }

    private static void patchDismount(ClassNode type) {
        MethodNode dismount = method(type, "dismountEntity", "func_110145_l", "(" + ENTITY + ")V");
        LabelNode legacy = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/entity/BoatDismount", "placePassenger",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getObjectType(type.name),
                        Type.getArgumentTypes(dismount.desc)[0]), false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, legacy));
        hook.add(new InsnNode(Opcodes.RETURN));
        hook.add(legacy);
        hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        dismount.instructions.insert(hook);
    }

    private static void patchFallState(MethodNode method) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!"net/minecraft/world/World".equals(remapper.map(call.owner))
                    || !("getBlockState".equals(name) || "func_180495_p".equals(name))
                    || !("(" + POSITION + ")" + STATE).equals(remapper.mapMethodDesc(call.desc))) continue;
            String descriptor = Type.getMethodDescriptor(Type.getReturnType(call.desc),
                    Type.getObjectType(call.owner), Type.getArgumentTypes(call.desc)[0]);
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, WATERLOGGING,
                    "waterState", descriptor, false));
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 boat fall fluid check");
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }

    private static void replaceBody(MethodNode method, InsnList body) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.instructions.add(body);
    }
}

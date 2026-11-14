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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class TridentTransformer implements IClassTransformer {
    private static final String PLAYER = "net.minecraft.entity.player.EntityPlayer";
    private static final String PLAYER_RENDERER = "net.minecraft.client.renderer.entity.RenderPlayer";
    private static final String LIVING_RENDERER = "net.minecraft.client.renderer.entity.RenderLivingBase";
    private static final String MODEL = "net.minecraft.client.model.ModelBiped";
    private static final String HAND_RENDERER = "net.minecraft.client.renderer.ItemRenderer";
    private static final String REPAIR = "net.minecraft.item.crafting.RecipeRepairItem";
    private static final String RIPTIDE = "ivorius/psychedelicraft/entity/Riptide";
    private static final String POSES = "ivorius/psychedelicraft/client/render/TridentPoses";
    private static final String STACK = "Lnet/minecraft/item/ItemStack;";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(PLAYER.equals(transformedName) || PLAYER_RENDERER.equals(transformedName)
                || LIVING_RENDERER.equals(transformedName) || MODEL.equals(transformedName)
                || HAND_RENDERER.equals(transformedName) || REPAIR.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        switch (transformedName) {
            case PLAYER:
                append(method(type, "<clinit>", "<clinit>", "()V"), () -> call(
                        RIPTIDE, "reserveSyncedState", "()V"));
                patchSize(type);
                break;
            case PLAYER_RENDERER:
                MethodNode init = method(type, "<init>", "<init>", "(Lnet/minecraft/client/renderer/entity/RenderManager;Z)V");
                append(init, () -> call(POSES, "attach", "(L" + type.name + ";)V", ref(0)));
                break;
            case MODEL:
                MethodNode angles = method(type, "setRotationAngles", "func_78087_a", "(FFFFFFLnet/minecraft/entity/Entity;)V");
                append(angles, () -> call(POSES, "throwArm", "(L" + type.name + ";" + argument(angles, 6) + ")V", ref(0), ref(7)));
                break;
            case HAND_RENDERER:
                patchHands(type);
                break;
            case LIVING_RENDERER:
                patchRotation(type);
                break;
            case REPAIR:
                MethodNode craft = method(type, "getCraftingResult", "func_77572_b", "(Lnet/minecraft/inventory/InventoryCrafting;)" + STACK);
                for (AbstractInsnNode instruction : craft.instructions.toArray()) {
                    if (instruction.getOpcode() == Opcodes.ARETURN) {
                        String stack = Type.getReturnType(craft.desc).getDescriptor();
                        craft.instructions.insertBefore(instruction, call("ivorius/psychedelicraft/item/TridentItem",
                                "keepRepairCurses", "(" + stack + argument(craft, 0) + ")" + stack, ref(1)));
                    }
                }
                break;
            default:
                throw new AssertionError(transformedName);
        }
        // Vanilla drops some method arguments from its terminal frames before the render hooks need them again.
        int flags = MODEL.equals(transformedName) || HAND_RENDERER.equals(transformedName)
                ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
        ClassWriter writer = new ClassWriter(flags);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchSize(ClassNode type) {
        String player = "L" + type.name + ";";
        MethodNode eye = method(type, "getEyeHeight", "func_70047_e", "()F");
        for (AbstractInsnNode instruction : eye.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.FRETURN) {
                eye.instructions.insertBefore(instruction, call(RIPTIDE, "eyeHeight", "(F" + player + ")F", ref(0)));
            }
        }

        MethodNode size = method(type, "updateSize", "func_184808_cD", "()V");
        for (AbstractInsnNode instruction : size.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != Opcodes.GETFIELD) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            String name = REMAPPER.mapFieldName(field.owner, field.name, field.desc);
            if (!"width".equals(name) && !"field_70130_N".equals(name)) continue;

            // The first comparison follows the branches that select the vanilla width and height.
            AbstractInsnNode compare = instruction.getPrevious().getPrevious();
            if (compare.getOpcode() != Opcodes.FLOAD || instruction.getPrevious().getOpcode() != Opcodes.ALOAD) break;
            int width = ((VarInsnNode) compare).var;
            InsnList pose = call(RIPTIDE, "desiredHeight", "(" + player + "FF)F",
                    ref(0), new VarInsnNode(Opcodes.FLOAD, width), new VarInsnNode(Opcodes.FLOAD, width + 1));
            pose.add(new VarInsnNode(Opcodes.FSTORE, width + 1));
            size.instructions.insertBefore(compare, pose);
            return;
        }
        throw new IllegalStateException("Could not locate the player size comparison");
    }

    private static void patchHands(ClassNode type) {
        MethodNode hand = method(type, "renderItemInFirstPerson", "func_187457_a",
                "(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;F" + STACK + "F)V");
        for (AbstractInsnNode instruction : hand.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode) || !matches((MethodInsnNode) instruction, "renderItemSide", "func_187462_a",
                    "(Lnet/minecraft/entity/EntityLivingBase;" + STACK + "Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V")) continue;
            // The native renderer has completed the normal holding transform at this point.
            hand.instructions.insertBefore(instruction, call(POSES, "firstPerson",
                    "(" + argument(hand, 0) + argument(hand, 3) + argument(hand, 5) + "F)V",
                    ref(1), ref(4), ref(6), new VarInsnNode(Opcodes.FLOAD, 2)));
            return;
        }
        throw new IllegalStateException("Could not locate the first-person held item render");
    }

    private static void patchRotation(ClassNode type) {
        MethodNode rotate = method(type, "applyRotations", "func_77043_a", "(Lnet/minecraft/entity/EntityLivingBase;FFF)V");
        for (AbstractInsnNode instruction : rotate.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode) || !matches((MethodInsnNode) instruction, "rotate", "func_179114_b", "(FFFF)V")) continue;
            rotate.instructions.insert(instruction, call(POSES, "spinPlayer", "(" + argument(rotate, 0) + "F)V",
                    ref(1), new VarInsnNode(Opcodes.FLOAD, 2)));
            return;
        }
        throw new IllegalStateException("Could not locate the living entity yaw rotation");
    }

    private static boolean matches(MethodInsnNode method, String name, String srg, String descriptor) {
        String mapped = REMAPPER.mapMethodName(method.owner, method.name, method.desc);
        return (name.equals(mapped) || srg.equals(mapped)) && descriptor.equals(REMAPPER.mapMethodDesc(method.desc));
    }

    private static String argument(MethodNode method, int index) {
        return Type.getArgumentTypes(method.desc)[index].getDescriptor();
    }

    private static VarInsnNode ref(int index) {
        return new VarInsnNode(Opcodes.ALOAD, index);
    }

    private static InsnList call(String owner, String name, String descriptor, AbstractInsnNode... args) {
        InsnList code = new InsnList();
        for (AbstractInsnNode arg : args) code.add(arg);
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, descriptor, false));
        return code;
    }

    private static void append(MethodNode method, java.util.function.Supplier<InsnList> hook) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.RETURN) method.instructions.insertBefore(instruction, hook.get());
        }
    }
}

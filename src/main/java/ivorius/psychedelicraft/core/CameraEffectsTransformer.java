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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class CameraEffectsTransformer implements IClassTransformer {
    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String ITEM_RENDERER = "net.minecraft.client.renderer.ItemRenderer";
    private static final String RENDER_LIVING = "net.minecraft.client.renderer.entity.RenderLivingBase";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/CameraEffects";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String LIVING = "Lnet/minecraft/entity/EntityLivingBase;";
    private static final String FRUSTUM = "Lnet/minecraft/client/renderer/culling/ICamera;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !ENTITY_RENDERER.equals(transformedName) && !RENDER_GLOBAL.equals(transformedName)
                && !ITEM_RENDERER.equals(transformedName) && !RENDER_LIVING.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (ENTITY_RENDERER.equals(transformedName)) {
            patchBodyView(method(type, "renderHand", "func_78476_b", "(FI)V"));
            patchMouse(type);
        } else if (RENDER_GLOBAL.equals(transformedName)) {
            patchBodyView(method(type, "renderEntities", "func_180446_a", "(" + ENTITY + FRUSTUM + "F)V"));
            patchBodyView(method(type, "isOutlineActive", "func_184383_a", "(" + ENTITY + ENTITY + FRUSTUM + ")Z"));
        } else if (ITEM_RENDERER.equals(transformedName)) {
            patchHands(type);
        } else {
            patchPose(type);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchBodyView(MethodNode render) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        boolean patched = false;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.GETFIELD) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            String name = remapper.mapFieldName(field.owner, field.name, field.desc);
            if ("net/minecraft/client/settings/GameSettings".equals(remapper.map(field.owner))
                    && ("thirdPersonView".equals(name) || "field_74320_O".equals(name))) {
                render.instructions.insert(field, hook("bodyView", "(I)I"));
                patched = true;
            }
        }
        if (!patched) throw new IllegalStateException("Could not locate Forge 1.12 render view setting");
    }

    private static void patchMouse(ClassNode type) {
        MethodNode update = method(type, "updateCameraAndRender", "func_181560_a", "(FJ)V");
        int patched = 0;
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!matches(call, "turn", "func_70082_c", "(FF)V")) continue;
            // Both branches consume the already focused player's turn after the vanilla input processing.
            update.instructions.insertBefore(call, new VarInsnNode(Opcodes.FLOAD, 1));
            update.instructions.set(call, hook("turn", "(L" + call.owner + ";FFF)V"));
            patched++;
        }
        if (patched != 2) throw new IllegalStateException("Could not locate both Forge 1.12 mouse turn calls");
    }

    private static void patchHands(ClassNode type) {
        MethodNode hands = method(type, "renderItemInFirstPerson", "func_78440_a", "(F)V");
        boolean began = false;
        boolean ended = false;
        for (AbstractInsnNode instruction : hands.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!"net/minecraft/client/renderer/GlStateManager".equals(FMLDeobfuscatingRemapper.INSTANCE.map(call.owner))) continue;
            if (matches(call, "enableRescaleNormal", "func_179091_B", "()V")) {
                // Arm rotation is ready here, and Forge has yet to dispatch either hand.
                InsnList begin = new InsnList();
                begin.add(new VarInsnNode(Opcodes.FLOAD, 1));
                begin.add(hook("beginHands", "(F)V"));
                hands.instructions.insert(call, begin);
                began = true;
            } else if (matches(call, "disableRescaleNormal", "func_179101_C", "()V")) {
                hands.instructions.insertBefore(call, hook("endHands", "()V"));
                ended = true;
            }
        }
        if (!began || !ended) throw new IllegalStateException("Could not locate Forge 1.12 first-person hand pass");
    }

    private static void patchPose(ClassNode type) {
        MethodNode render = method(type, "doRender", "func_76986_a", "(" + LIVING + "DDDFF)V");
        FieldNode model = ClientEffectsTransformer.field(type, "mainModel", "field_77045_g");
        Type living = Type.getArgumentTypes(render.desc)[0];
        String parameters = "(" + model.desc + living.getDescriptor();
        boolean applied = false;
        boolean restored = false;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (matches(call, "setRotationAngles", "func_78087_a", "(FFFFFF" + ENTITY + ")V")) {
                InsnList pose = modelAndEntity(type, model);
                pose.add(new VarInsnNode(Opcodes.FLOAD, 9));
                pose.add(hook("posePlayer", parameters + "F)V"));
                render.instructions.insert(call, pose);
                applied = true;
            } else if (call.getOpcode() == Opcodes.INVOKESPECIAL && type.superName.equals(call.owner)
                    && matches(call, "doRender", "func_76986_a", "(" + ENTITY + "DDDFF)V")) {
                InsnList restore = modelAndEntity(type, model);
                restore.add(hook("restorePose", parameters + ")V"));
                render.instructions.insertBefore(call, restore);
                restored = true;
            }
        }
        if (!applied || !restored) throw new IllegalStateException("Could not locate Forge 1.12 player pose scope");
    }

    private static InsnList modelAndEntity(ClassNode type, FieldNode model) {
        InsnList args = new InsnList();
        args.add(new VarInsnNode(Opcodes.ALOAD, 0));
        args.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, model.name, model.desc));
        args.add(new VarInsnNode(Opcodes.ALOAD, 1));
        return args;
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }

    private static boolean matches(MethodInsnNode call, String name, String srg, String descriptor) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
        return (name.equals(mapped) || srg.equals(mapped)) && descriptor.equals(remapper.mapMethodDesc(call.desc));
    }
}

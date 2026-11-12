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

public final class ClientEffectsTransformer implements IClassTransformer {
    private static final String SOUND_MANAGER = "net.minecraft.client.audio.SoundManager";
    private static final String CHAT = "net.minecraft.client.gui.GuiNewChat";
    private static final String SOUND_HOOK = "ivorius/psychedelicraft/client/ClientSoundEffects";
    private static final String CHAT_HOOK = "ivorius/psychedelicraft/client/ClientChatEffects";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !SOUND_MANAGER.equals(transformedName) && !CHAT.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (SOUND_MANAGER.equals(transformedName)) patchSound(type);
        else patchChat(type);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchSound(ClassNode type) {
        MethodNode volume = method(type, "getVolume", "func_188769_a", "(Lnet/minecraft/util/SoundCategory;)F");
        for (AbstractInsnNode instruction : volume.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.FRETURN) {
                volume.instructions.insertBefore(instruction, new MethodInsnNode(Opcodes.INVOKESTATIC, SOUND_HOOK,
                        "scaleVolume", "(F)F", false));
            }
        }

        MethodNode update = method(type, "updateAllSounds", "func_148605_d", "()V");
        FieldNode soundSystem = field(type, "sndSystem", "field_148620_e");
        FieldNode options = field(type, "options", "field_148619_d");
        FieldNode playing = field(type, "playingSounds", "field_148629_h");
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.RETURN) continue;
            InsnList hook = new InsnList();
            loadField(hook, type, soundSystem);
            loadField(hook, type, options);
            loadField(hook, type, playing);
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SOUND_HOOK, "refreshPlaying",
                    "(Lpaulscode/sound/SoundSystem;" + options.desc + playing.desc + ")V", false));
            update.instructions.insertBefore(instruction, hook);
        }
    }

    private static void patchChat(ClassNode type) {
        MethodNode setLine = method(type, "setChatLine", "func_146237_a", "(Lnet/minecraft/util/text/ITextComponent;IIZ)V");
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : setLine.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!"net/minecraft/client/gui/GuiUtilRenderComponents".equals(remapper.map(call.owner))
                    || !("splitText".equals(name) || "func_178908_a".equals(name))) continue;

            // Replace just the wrapping call's text argument. The stored chat line still uses the original component.
            Type[] arguments = Type.getArgumentTypes(call.desc);
            InsnList hook = new InsnList();
            int firstLocal = setLine.maxLocals;
            for (int index = arguments.length - 1; index > 0; index--) {
                hook.add(new VarInsnNode(arguments[index].getOpcode(Opcodes.ISTORE), firstLocal + index - 1));
            }
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CHAT_HOOK, "distortIncoming",
                    Type.getMethodDescriptor(arguments[0], arguments[0]), false));
            for (int index = 1; index < arguments.length; index++) {
                hook.add(new VarInsnNode(arguments[index].getOpcode(Opcodes.ILOAD), firstLocal + index - 1));
            }
            setLine.maxLocals += arguments.length - 1;
            setLine.instructions.insertBefore(call, hook);
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 chat line wrapping");
    }

    private static void loadField(InsnList hook, ClassNode type, FieldNode field) {
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, field.name, field.desc));
    }

    private static MethodNode method(ClassNode type, String name, String srg, String descriptor) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            String mapped = remapper.mapMethodName(type.name, method.name, method.desc);
            if ((name.equals(mapped) || srg.equals(mapped)) && descriptor.equals(remapper.mapMethodDesc(method.desc))) {
                return method;
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 method " + type.name + "." + name);
    }

    private static FieldNode field(ClassNode type, String name, String srg) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (FieldNode field : type.fields) {
            String mapped = remapper.mapFieldName(type.name, field.name, field.desc);
            if (name.equals(mapped) || srg.equals(mapped)) return field;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 field " + type.name + "." + name);
    }
}

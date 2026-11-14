package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class ChanneledLightningTransformer implements IClassTransformer {
    private static final String LIGHTNING = "net.minecraft.entity.effect.EntityLightningBolt";
    private static final String HOOK = "ivorius/psychedelicraft/entity/ChanneledLightningBolt";
    private static final String TARGETS = "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;";
    private static final String STRIKE = "(Lnet/minecraft/entity/effect/EntityLightningBolt;)V";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformed, byte[] bytes) {
        if (bytes == null || !LIGHTNING.equals(transformed)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode update = method(type, "onUpdate", "func_70071_h_", "()V");
        boolean foundTargets = false;
        boolean foundStrike = false;
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String mappedName = REMAPPER.mapMethodName(call.owner, call.name, call.desc);
            String descriptor = REMAPPER.mapMethodDesc(call.desc);
            if (("getEntitiesWithinAABBExcludingEntity".equals(mappedName) || "func_72839_b".equals(mappedName))
                    && TARGETS.equals(descriptor)) {
                replace(update, call, "beginStrike");
                foundTargets = true;
            } else if (("onStruckByLightning".equals(mappedName) || "func_70077_a".equals(mappedName))
                    && STRIKE.equals(descriptor)) {
                replace(update, call, "strike");
                foundStrike = true;
            }
        }
        if (!foundTargets || !foundStrike) throw new IllegalStateException("Could not locate the native lightning strike loop");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void replace(MethodNode update, MethodInsnNode call, String hook) {
        update.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, hook,
                "(L" + call.owner + ";" + call.desc.substring(1), false));
    }
}

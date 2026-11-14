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

public final class ExplosionDropTransformer implements IClassTransformer {
    private static final String EXPLOSION = "net.minecraft.world.Explosion";
    private static final String FIREBALL = "net.minecraft.entity.projectile.EntityLargeFireball";
    private static final String HOOKS = "ivorius/psychedelicraft/world/ExplosionDropRules";
    private static final String DECAY_FIELD = "psychedelicraft$decaysDrops";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(EXPLOSION.equals(transformedName) || FIREBALL.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (EXPLOSION.equals(transformedName)) {
            rememberSourceRule(type);
            applyDropRule(type);
            applyDamageSource(type);
        } else {
            passFireballSource(type);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void rememberSourceRule(ClassNode type) {
        type.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, DECAY_FIELD, "Z", null, null));
        MethodNode constructor = ClientEffectsTransformer.method(type, "<init>", "<init>",
                "(" + WORLD + ENTITY + "DDDFZZ)V");
        Type world = Type.getArgumentTypes(constructor.desc)[0];
        FieldNode source = ClientEffectsTransformer.field(type, "exploder", "field_77283_e");
        for (AbstractInsnNode instruction : constructor.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.PUTFIELD || !source.name.equals(((FieldInsnNode) instruction).name)) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "decays", "(" + world + source.desc + ")Z", false));
            hook.add(new FieldInsnNode(Opcodes.PUTFIELD, type.name, DECAY_FIELD, "Z"));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "nativeSource", "(" + source.desc + ")" + source.desc, false));
            constructor.instructions.insertBefore(instruction, hook);
            return;
        }
        throw new IllegalStateException("Could not locate the explosion source assignment");
    }

    private static void applyDropRule(ClassNode type) {
        MethodNode destroy = ClientEffectsTransformer.method(type, "doExplosionB", "func_77279_a", "(Z)V");
        for (AbstractInsnNode instruction : destroy.instructions.toArray()) {
            if (!isCall(instruction, "dropBlockAsItemWithChance", "func_180653_a",
                    "(" + WORLD + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;FI)V")) continue;
            AbstractInsnNode fortune = previousInstruction(instruction);
            if (fortune == null || fortune.getOpcode() != Opcodes.ICONST_0) break;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, DECAY_FIELD, "Z"));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "dropChance", "(FZ)F", false));
            destroy.instructions.insertBefore(fortune, hook);
            return;
        }
        throw new IllegalStateException("Could not locate the explosion block drop call");
    }

    private static void applyDamageSource(ClassNode type) {
        MethodNode damage = ClientEffectsTransformer.method(type, "doExplosionA", "func_77278_a", "()V");
        for (AbstractInsnNode instruction : damage.instructions.toArray()) {
            if (!isCall(instruction, "causeExplosionDamage", "func_94539_a",
                    "(Lnet/minecraft/world/Explosion;)Lnet/minecraft/util/DamageSource;")) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            FieldNode source = ClientEffectsTransformer.field(type, "exploder", "field_77283_e");
            InsnList exploder = new InsnList();
            exploder.add(new VarInsnNode(Opcodes.ALOAD, 0));
            exploder.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, source.name, source.desc));
            damage.instructions.insertBefore(call, exploder);
            damage.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "ivorius/psychedelicraft/PSDamageTypes", "explosionDamage",
                    call.desc.replace(")", source.desc + ")"), false));
            return;
        }
        throw new IllegalStateException("Could not locate the explosion damage source call");
    }

    private static void passFireballSource(ClassNode type) {
        MethodNode impact = ClientEffectsTransformer.method(type, "onImpact", "func_70227_a",
                "(Lnet/minecraft/util/math/RayTraceResult;)V");
        for (AbstractInsnNode instruction : impact.instructions.toArray()) {
            if (!isCall(instruction, "newExplosion", "func_72885_a", "(" + ENTITY + "DDDFZZ)Lnet/minecraft/world/Explosion;")) continue;
            for (AbstractInsnNode argument = previousInstruction(instruction); argument != null; argument = previousInstruction(argument)) {
                if (argument.getOpcode() == Opcodes.ACONST_NULL) {
                    impact.instructions.set(argument, new VarInsnNode(Opcodes.ALOAD, 0));
                    return;
                }
            }
        }
        throw new IllegalStateException("Could not locate the large fireball explosion source");
    }

    private static AbstractInsnNode previousInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static boolean isCall(AbstractInsnNode instruction, String mcp, String srg, String descriptor) {
        if (!(instruction instanceof MethodInsnNode)) return false;
        MethodInsnNode call = (MethodInsnNode) instruction;
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String name = remapper.mapMethodName(call.owner, call.name, call.desc);
        return (mcp.equals(name) || srg.equals(name)) && descriptor.equals(remapper.mapMethodDesc(call.desc));
    }
}

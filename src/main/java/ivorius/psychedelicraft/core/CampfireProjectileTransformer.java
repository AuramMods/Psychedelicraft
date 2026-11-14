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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class CampfireProjectileTransformer implements IClassTransformer {
    private static final String HOOKS = "ivorius/psychedelicraft/block/CampfireBlock";
    private static final String PROJECTILES = "ivorius/psychedelicraft/entity/CampfireProjectile";
    private static final String HIT = "net/minecraft/util/math/RayTraceResult";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        switch (transformedName) {
            case "net.minecraft.entity.projectile.EntityArrow":
            case "net.minecraft.entity.projectile.EntityFireball":
            case "net.minecraft.entity.projectile.EntityThrowable":
            case "net.minecraft.entity.projectile.EntityLlamaSpit":
                return patchImpact(bytes);
            case "net.minecraft.entity.projectile.EntityFishHook":
                return patchBobber(bytes);
            case "net.minecraft.entity.item.EntityFireworkRocket":
                return patchRocket(bytes);
            case "net.minecraft.item.ItemFirework":
                return patchFireworkItem(bytes);
            default:
                return bytes;
        }
    }

    private static byte[] patchBobber(byte[] bytes) {
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode collision = method(type, "checkCollision", "func_190624_r", "()V");
        for (AbstractInsnNode instruction : collision.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != Opcodes.GETFIELD) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            String mapped = REMAPPER.mapFieldName(field.owner, field.name, field.desc);
            if (!HIT.equals(REMAPPER.map(field.owner)) || !("typeOfHit".equals(mapped) || "field_72313_a".equals(mapped))) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new InsnNode(Opcodes.SWAP));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PROJECTILES, "checkBobberHit",
                    "(L" + type.superName + ";L" + field.owner + ";)" + field.desc, false));
            collision.instructions.insertBefore(field, hook);
            collision.instructions.remove(field);
            // The first check excludes misses before the native hook or in-ground branch.
            return write(type);
        }
        throw new IllegalStateException("Could not locate the fishing bobber hit check");
    }

    private static byte[] patchRocket(byte[] bytes) {
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode update = method(type, "onUpdate", "func_70071_h_", "()V");
        InsnList attached = new InsnList();
        attached.add(new VarInsnNode(Opcodes.ALOAD, 0));
        attached.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PROJECTILES, "checkAttachedFireworkHit",
                "(L" + type.superName + ";)V", false));
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!matches(call, "move", "func_70091_d", "(Lnet/minecraft/entity/MoverType;DDD)V")) continue;
            // Both movement branches rejoin immediately after the free rocket's move.
            AbstractInsnNode join = call.getNext();
            while (join.getOpcode() < 0) join = join.getNext();
            update.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, PROJECTILES, "moveFirework",
                    "(L" + type.superName + ";" + call.desc.substring(1), false));
            update.instructions.insertBefore(join, attached);
            return write(type);
        }
        throw new IllegalStateException("Could not locate firework movement");
    }

    private static byte[] patchFireworkItem(byte[] bytes) {
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode place = method(type, "onItemUse", "func_180614_a",
                "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
                        + "Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)Lnet/minecraft/util/EnumActionResult;");
        MethodNode boost = method(type, "onItemRightClick", "func_77659_a",
                "(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/ActionResult;");
        recordRocketOwner(place, 1, Type.getArgumentTypes(place.desc)[0]);
        recordRocketOwner(boost, 2, Type.getArgumentTypes(boost.desc)[1]);
        return write(type);
    }

    private static void recordRocketOwner(MethodNode launch, int playerSlot, Type player) {
        for (AbstractInsnNode instruction : launch.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!matches(call, "spawnEntity", "func_72838_d", "(Lnet/minecraft/entity/Entity;)Z")) continue;
            InsnList hook = new InsnList();
            hook.add(new InsnNode(Opcodes.DUP));
            hook.add(new VarInsnNode(Opcodes.ALOAD, playerSlot));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PROJECTILES, "setFireworkOwner",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(call.desc)[0], player), false));
            launch.instructions.insertBefore(call, hook);
            return;
        }
        throw new IllegalStateException("Could not locate firework spawning in " + launch.name);
    }

    private static boolean matches(MethodInsnNode call, String name, String srg, String desc) {
        String mapped = REMAPPER.mapMethodName(call.owner, call.name, call.desc);
        return (name.equals(mapped) || srg.equals(mapped)) && desc.equals(REMAPPER.mapMethodDesc(call.desc));
    }

    private static byte[] write(ClassNode type) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchImpact(byte[] bytes) {
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode update = method(type, "onUpdate", "func_70071_h_", "()V");
        for (AbstractInsnNode instruction : update.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!"net/minecraftforge/event/ForgeEventFactory".equals(call.owner) || !"onProjectileImpact".equals(call.name)) continue;
            Type ray = Type.getArgumentTypes(call.desc)[1];
            String descriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getObjectType(type.superName), ray, Type.BOOLEAN_TYPE);
            update.instructions.insertBefore(call, new InsnNode(Opcodes.DUP2));
            update.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "afterProjectileImpact", descriptor, false));
            return write(type);
        }
        throw new IllegalStateException("Could not locate the Forge projectile impact hook in " + type.name);
    }
}

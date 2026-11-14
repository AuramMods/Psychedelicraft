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

public final class ProjectileGatewayTransformer implements IClassTransformer {
    private static final String GATEWAY = "net.minecraft.tileentity.TileEntityEndGateway";
    private static final String WORLD = "net/minecraft/world/World";
    private static final String ENTITY = "net/minecraft/entity/Entity";
    private static final String HOOKS = "ivorius/psychedelicraft/entity/ProjectileGateway";
    private static final String QUERY = "(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !GATEWAY.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        replace(ClientEffectsTransformer.method(type, "update", "func_73660_a", "()V"),
                WORLD, "getEntitiesWithinAABB", "func_72872_a", QUERY, "find");
        replace(ClientEffectsTransformer.method(type, "teleportEntity", "func_184306_a", "(L" + ENTITY + ";)V"),
                ENTITY, "setPositionAndUpdate", "func_70634_a", "(DDD)V", "teleport");
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void replace(MethodNode method, String owner, String name, String srg, String descriptor, String hook) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (owner.equals(remapper.map(call.owner)) && descriptor.equals(remapper.mapMethodDesc(call.desc))
                    && (name.equals(mapped) || srg.equals(mapped))) {
                method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook,
                        "(L" + call.owner + ";" + call.desc.substring(1), false));
                return;
            }
        }
        throw new IllegalStateException("Could not locate the End gateway " + name + " call");
    }
}

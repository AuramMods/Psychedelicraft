package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class TreasureMapTransformer implements IClassTransformer {
    private static final String TILE = "net.minecraft.tileentity.TileEntityLockableLoot";
    private static final String DECORATION = "net.minecraft.world.storage.MapDecoration";
    private static final String LOOT_TABLE = "net/minecraft/world/storage/loot/LootTable";
    private static final String FILL_INVENTORY = "(Lnet/minecraft/inventory/IInventory;Ljava/util/Random;"
            + "Lnet/minecraft/world/storage/loot/LootContext;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;
        boolean render = DECORATION.equals(transformedName) && FMLLaunchHandler.side() == Side.CLIENT;
        if (!render && !TILE.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (render) patchRender(type);
        else patchLoot(type);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchRender(ClassNode type) {
        MethodNode method = ClientEffectsTransformer.method(type, "render", "render", "(I)Z");
        method.instructions.clear();
        method.localVariables = null;
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "ivorius/psychedelicraft/client/render/TreasureMapMarker", "render", "(L" + type.name + ";I)Z", false));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
    }

    private static void patchLoot(ClassNode type) {
        MethodNode fill = ClientEffectsTransformer.method(type, "fillWithLoot", "func_184281_d",
                "(Lnet/minecraft/entity/player/EntityPlayer;)V");
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : fill.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String mappedName = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!LOOT_TABLE.equals(remapper.map(call.owner)) || !FILL_INVENTORY.equals(remapper.mapMethodDesc(call.desc))
                    || !("fillInventory".equals(mappedName) || "func_186460_a".equals(mappedName))) continue;
            String descriptor = "(L" + call.owner + ";" + call.desc.substring(1);
            fill.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "ivorius/psychedelicraft/world/gen/TreasureMapLoot", "fillFromContainer", descriptor, false));
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 container loot generation");
    }
}

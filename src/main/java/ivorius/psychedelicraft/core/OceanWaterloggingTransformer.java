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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class OceanWaterloggingTransformer implements IClassTransformer {
    private static final String TILE = "net.minecraft.tileentity.TileEntity";
    private static final String ITEM = "net.minecraft.item.ItemBlock";
    private static final String CHEST = "ivorius/psychedelicraft/fluid/physical/ChestWaterlogging";
    private static final String WATERLOGGING = "ivorius/psychedelicraft/fluid/physical/Waterlogging";
    private static final String PACKET = "Lnet/minecraft/network/play/server/SPacketUpdateTileEntity;";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformed, byte[] bytes) {
        if (bytes == null || !(TILE.equals(transformed) || ITEM.equals(transformed))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (TILE.equals(transformed)) {
            patchChestPackets(type);
        } else {
            patchPlacement(ClientEffectsTransformer.method(type, "placeBlockAt", "placeBlockAt",
                    "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;" + WORLD + POS
                            + "Lnet/minecraft/util/EnumFacing;FFF" + STATE + ")Z"));
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchChestPackets(ClassNode type) {
        MethodNode get = ClientEffectsTransformer.method(type, "getUpdatePacket", "func_189518_D_", "()" + PACKET);
        for (AbstractInsnNode instruction : get.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.ACONST_NULL) continue;
            get.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, 0));
            get.instructions.set(instruction, new MethodInsnNode(Opcodes.INVOKESTATIC, CHEST, "packet",
                    "(L" + type.name + ";)" + Type.getReturnType(get.desc).getDescriptor(), false));
        }
        if (FMLLaunchHandler.side() != Side.CLIENT) return;
        MethodNode receive = ClientEffectsTransformer.method(type, "onDataPacket", "onDataPacket",
                "(Lnet/minecraft/network/NetworkManager;" + PACKET + ")V");
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CHEST, "receive",
                "(L" + type.name + ";" + Type.getArgumentTypes(receive.desc)[1].getDescriptor() + ")V", false));
        receive.instructions.insert(hook);
    }

    private static void patchPlacement(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = REMAPPER.mapMethodName(call.owner, call.name, call.desc);
            if (!"net/minecraft/world/World".equals(REMAPPER.map(call.owner))
                    || !("(" + POS + STATE + "I)Z").equals(REMAPPER.mapMethodDesc(call.desc))
                    || !("setBlockState".equals(name) || "func_180501_a".equals(name))) continue;
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, WATERLOGGING, "placeInSource",
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 block placement in " + method.name);
    }
}

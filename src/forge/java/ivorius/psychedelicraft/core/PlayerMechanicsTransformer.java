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
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PlayerMechanicsTransformer implements IClassTransformer {
    private static final String PLAYER = "net.minecraft.entity.player.EntityPlayer";
    private static final String SERVER_PLAYER = "net.minecraft.entity.player.EntityPlayerMP";
    private static final String CONTAINER = "net.minecraft.inventory.Container";
    private static final String FOOD_STATS = "net/minecraft/util/FoodStats";
    private static final String STOMACH_STATS = "ivorius/psychedelicraft/entity/drug/StomachFoodStats";
    private static final String PAPER_BAG = "ivorius/psychedelicraft/item/PaperBagItem";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(PLAYER.equals(transformedName) || SERVER_PLAYER.equals(transformedName)
                || CONTAINER.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (PLAYER.equals(transformedName)) replaceFoodStats(type);
        else if (SERVER_PLAYER.equals(transformedName)) syncRawFoodStats(type);
        else addPaperBagClicks(type);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void replaceFoodStats(ClassNode type) {
        boolean replaced = false;
        for (MethodNode method : type.methods) {
            if (!"<init>".equals(method.name)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.NEW) {
                    TypeInsnNode constructor = (TypeInsnNode) instruction;
                    if (FOOD_STATS.equals(REMAPPER.map(constructor.desc))) constructor.desc = STOMACH_STATS;
                } else if (instruction.getOpcode() == Opcodes.INVOKESPECIAL) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (FOOD_STATS.equals(REMAPPER.map(call.owner)) && "<init>".equals(call.name) && "()V".equals(call.desc)) {
                        call.owner = STOMACH_STATS;
                        replaced = true;
                    }
                }
            }
        }
        if (!replaced) throw new IllegalStateException("Could not locate EntityPlayer's FoodStats constructor");
    }

    private static void syncRawFoodStats(ClassNode type) {
        for (MethodNode method : type.methods) {
            if (!"()V".equals(method.desc) || !matchesMethod(type, method, "onUpdateEntity", "func_71127_g")) continue;
            int reads = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    String name = REMAPPER.mapFieldName(field.owner, field.name, field.desc);
                    if ("lastHealthScore".equals(name) || "field_130068_bO".equals(name)) break;
                }
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!FOOD_STATS.equals(REMAPPER.map(call.owner))) continue;
                String name = REMAPPER.mapMethodName(call.owner, call.name, call.desc);
                String hook;
                if ("getFoodLevel".equals(name) || "func_75116_a".equals(name)) hook = "packetFoodLevel";
                else if ("getSaturationLevel".equals(name) || "func_75115_e".equals(name)) hook = "packetSaturation";
                else continue;
                method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, STOMACH_STATS,
                        hook, "(L" + call.owner + ";" + call.desc.substring(1), false));
                reads++;
            }
            if (reads > 0) return;
        }
        throw new IllegalStateException("Could not locate EntityPlayerMP's food synchronization");
    }

    private static void addPaperBagClicks(ClassNode type) {
        String descriptor = "(IILnet/minecraft/inventory/ClickType;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;";
        for (MethodNode method : type.methods) {
            if (!descriptor.equals(REMAPPER.mapMethodDesc(method.desc)) || !matchesMethod(type, method, "slotClick", "func_184996_a")) continue;
            LabelNode vanilla = new LabelNode();
            InsnList click = new InsnList();
            click.add(new VarInsnNode(Opcodes.ALOAD, 0));
            click.add(new VarInsnNode(Opcodes.ILOAD, 1));
            click.add(new VarInsnNode(Opcodes.ILOAD, 2));
            click.add(new VarInsnNode(Opcodes.ALOAD, 3));
            click.add(new VarInsnNode(Opcodes.ALOAD, 4));
            click.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PAPER_BAG, "handleClick",
                    "(L" + type.name + ";" + method.desc.substring(1), false));
            click.add(new InsnNode(Opcodes.DUP));
            click.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
            click.add(new InsnNode(Opcodes.ARETURN));
            click.add(vanilla);
            click.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1,
                    new Object[]{Type.getReturnType(method.desc).getInternalName()}));
            click.add(new InsnNode(Opcodes.POP));
            method.instructions.insert(click);
            return;
        }
        throw new IllegalStateException("Could not locate Container.slotClick");
    }

    private static boolean matchesMethod(ClassNode owner, MethodNode method, String mcp, String srg) {
        String mapped = REMAPPER.mapMethodName(owner.name, method.name, method.desc);
        return mcp.equals(mapped) || srg.equals(mapped);
    }
}

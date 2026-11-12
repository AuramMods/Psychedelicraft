package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class FurnaceRecipeTransformer implements IClassTransformer {
    private static final String FURNACE_RECIPES = "net.minecraft.item.crafting.FurnaceRecipes";
    private static final String FURNACE_TILE = "net.minecraft.tileentity.TileEntityFurnace";
    private static final String RESULT_DESCRIPTOR = "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;";
    private static final String HEATING = "ivorius/psychedelicraft/recipe/CoffeeHeating";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(FURNACE_RECIPES.equals(transformedName) || FURNACE_TILE.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (FURNACE_RECIPES.equals(transformedName)) patchRecipes(type);
        else patchFuelConsumption(type);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchRecipes(ClassNode type) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            String mappedName = remapper.mapMethodName(type.name, method.name, method.desc);
            if (("getSmeltingResult".equals(mappedName) || "func_151395_a".equals(mappedName))
                    && RESULT_DESCRIPTOR.equals(remapper.mapMethodDesc(method.desc))) {
                addCoffeeLookup(method);
                return;
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 FurnaceRecipes.getSmeltingResult");
    }

    private static void patchFuelConsumption(ClassNode type) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            String name = remapper.mapMethodName(type.name, method.name, method.desc);
            if (!"()V".equals(method.desc) || !("update".equals(name) || "func_73660_a".equals(name))) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                String target = remapper.mapMethodName(call.owner, call.name, call.desc);
                if ("net/minecraft/item/ItemStack".equals(remapper.map(call.owner)) && "(I)V".equals(call.desc)
                        && ("shrink".equals(target) || "func_190918_g".equals(target))) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/entity/drug/SmokeEffects",
                            "fuelConsumed", "(L" + type.name + ";)V", false));
                    method.instructions.insertBefore(instruction, hook);
                    return;
                }
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 furnace fuel consumption");
    }

    private static void addCoffeeLookup(MethodNode method) {
        LabelNode vanilla = new LabelNode();
        String stackType = Type.getReturnType(method.desc).getInternalName();
        InsnList lookup = new InsnList();
        lookup.add(new VarInsnNode(Opcodes.ALOAD, 1));
        lookup.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HEATING, "getResult", method.desc, false));
        lookup.add(new InsnNode(Opcodes.DUP));
        lookup.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
        lookup.add(new InsnNode(Opcodes.ARETURN));
        lookup.add(vanilla);
        lookup.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{stackType}));
        lookup.add(new InsnNode(Opcodes.POP));
        method.instructions.insert(lookup);
    }
}

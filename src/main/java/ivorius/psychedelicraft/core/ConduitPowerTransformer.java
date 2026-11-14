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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class ConduitPowerTransformer implements IClassTransformer {
    private static final String LIVING = "net.minecraft.entity.EntityLivingBase";
    private static final String PLAYER = "net.minecraft.entity.player.EntityPlayer";
    private static final String RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String POWER = "ivorius/psychedelicraft/entity/ConduitPower";
    private static final String VISION = "ivorius/psychedelicraft/client/ConduitVision";
    private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformed, byte[] bytes) {
        if (bytes == null || !(LIVING.equals(transformed) || PLAYER.equals(transformed)
                || RENDERER.equals(transformed) && FMLLaunchHandler.side() == Side.CLIENT)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (LIVING.equals(transformed)) {
            patchPotion(ClientEffectsTransformer.method(type, "onEntityUpdate", "func_70030_z", "()V"),
                    "WATER_BREATHING", "field_76427_o", POWER, "allowsBreathing", null);
            patchMining(ClientEffectsTransformer.method(type, "getArmSwingAnimationEnd", "func_82166_i", "()I"));
        } else if (PLAYER.equals(transformed)) {
            patchMining(ClientEffectsTransformer.method(type, "getDigSpeed", "getDigSpeed",
                    "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;)F"));
        } else {
            patchVision(type);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchMining(MethodNode method) {
        patchPotion(method, "HASTE", "field_76422_e", POWER, "hasMiningBoost", "miningBoost");
    }

    private static void patchVision(ClassNode type) {
        patchPotion(ClientEffectsTransformer.method(type, "updateLightmap", "func_78472_g", "(F)V"),
                "NIGHT_VISION", "field_76439_r", VISION, "hasVision", null);
        patchPotion(ClientEffectsTransformer.method(type, "updateFogColor", "func_78466_h", "(F)V"),
                "NIGHT_VISION", "field_76439_r", VISION, "hasVision", null);
        patchPotion(ClientEffectsTransformer.method(type, "setupFog", "func_78468_a", "(IF)V"),
                "WATER_BREATHING", "field_76427_o", POWER, "allowsBreathing", null);
        MethodNode brightness = ClientEffectsTransformer.method(type, "getNightVisionBrightness", "func_180438_a",
                "(Lnet/minecraft/entity/EntityLivingBase;F)F");
        brightness.instructions.clear();
        brightness.localVariables = null;
        brightness.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        brightness.instructions.add(new VarInsnNode(Opcodes.FLOAD, 2));
        brightness.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, VISION, "brightness", brightness.desc, false));
        brightness.instructions.add(new InsnNode(Opcodes.FRETURN));
    }

    private static void patchPotion(MethodNode method, String fieldName, String srg, String owner,
                                    String active, String effect) {
        int patched = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != Opcodes.GETSTATIC) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            String fieldId = REMAPPER.mapFieldName(field.owner, field.name, field.desc);
            if (!"net/minecraft/init/MobEffects".equals(REMAPPER.map(field.owner))
                    || !(fieldName.equals(fieldId) || srg.equals(fieldId)) || !(field.getNext() instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) field.getNext();
            String name = REMAPPER.mapMethodName(call.owner, call.name, call.desc);
            String replacement = null;
            if ("isPotionActive".equals(name) || "func_70644_a".equals(name)) replacement = active;
            if ("getActivePotionEffect".equals(name) || "func_70660_b".equals(name)) replacement = effect;
            if (replacement == null) continue;
            String base = LIVING.replace('.', '/');
            if (!REMAPPER.map(call.owner).equals(call.owner)) base = REMAPPER.unmap(base);
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, owner, replacement,
                    Type.getMethodDescriptor(Type.getReturnType(call.desc), Type.getObjectType(base), Type.getType(field.desc)), false));
            patched++;
        }
        if (patched == 0) throw new IllegalStateException("Could not locate " + fieldName + " in " + method.name);
    }
}

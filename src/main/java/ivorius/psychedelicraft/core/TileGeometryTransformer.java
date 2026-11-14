package ivorius.psychedelicraft.core;

import com.google.common.collect.ImmutableSet;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Set;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;
import static ivorius.psychedelicraft.core.EntityGeometryTransformer.patchCalls;

public final class TileGeometryTransformer implements IClassTransformer {
    private static final String PREFIX = "net.minecraft.client.renderer.tileentity.TileEntity";
    private static final String MODELS = "net/minecraft/client/model/";
    private static final String TESSELLATOR = "net/minecraft/client/renderer/Tessellator";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/DrugRenderer";
    private static final Set<String> RENDERERS = ImmutableSet.of("Banner", "Beacon", "Bed", "Chest",
            "EnchantmentTable", "EndPortal", "EnderChest", "Piston", "ShulkerBox", "Sign", "Skull", "Structure");

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null || !transformedName.startsWith(PREFIX)
                || !transformedName.endsWith("Renderer")) return bytes;
        String tile = transformedName.substring(PREFIX.length(), transformedName.length() - "Renderer".length());
        if (!RENDERERS.contains(tile)) return bytes;

        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        switch (tile) {
            case "Banner":
                patchCalls(type.methods, MODELS + "ModelBanner", "renderBanner", "func_178687_a", "()V", "drawBanner");
                break;
            case "Bed":
                patchCalls(type.methods, MODELS + "ModelBed", "render", "func_193771_b", "()V", "drawBed");
                break;
            case "Chest":
            case "EnderChest":
                patchCalls(type.methods, MODELS + "ModelChest", "renderAll", "func_78231_a", "()V", "drawChest");
                break;
            case "EnchantmentTable":
            case "Skull":
                patchTileModel(type);
                break;
            case "ShulkerBox":
                patchCalls(type.methods, MODELS + "ModelRenderer", "render", "func_78785_a", "(F)V", "drawTilePart");
                break;
            case "Sign":
                patchCalls(type.methods, MODELS + "ModelSign", "renderSign", "func_78164_a", "()V", "drawSign");
                patchCalls(type.methods, "net/minecraft/client/gui/FontRenderer", "drawString", "func_78276_b",
                        "(Ljava/lang/String;III)I", "drawSignText");
                break;
            case "Beacon":
                patchCalls(type.methods, TESSELLATOR, "draw", "func_78381_a", "()V", "drawBeaconBeam");
                break;
            case "EndPortal":
                patchCalls(type.methods, TESSELLATOR, "draw", "func_78381_a", "()V", "drawPortalLayer");
                break;
            case "Piston":
                patchPiston(method(type, "render", "func_192841_a", "(Lnet/minecraft/tileentity/TileEntityPiston;DDDFIF)V"));
                break;
            case "Structure":
                patchCalls(type.methods, TESSELLATOR, "draw", "func_78381_a", "()V", "drawWorldQuads");
                break;
            default:
                throw new AssertionError(tile);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchTileModel(ClassNode type) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                String owner = remapper.map(call.owner);
                String name = remapper.mapMethodName(call.owner, call.name, call.desc);
                if (!owner.startsWith(MODELS) || !("render".equals(name) || "func_78088_a".equals(name))
                        || !"(Lnet/minecraft/entity/Entity;FFFFFF)V".equals(remapper.mapMethodDesc(call.desc))) continue;
                String model = owner.equals(call.owner) ? MODELS + "ModelBase" : remapper.unmap(MODELS + "ModelBase");
                method.instructions.set(call, hook("drawTileModel", "(L" + model + ";"
                        + call.desc.substring(1)));
                return;
            }
        }
        throw new IllegalStateException("Could not locate the block entity model in " + type.name);
    }

    private static void patchPiston(MethodNode render) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!TESSELLATOR.equals(remapper.map(call.owner)) || !("draw".equals(name) || "func_78381_a".equals(name))
                    || !"()V".equals(call.desc)) continue;
            render.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 1));
            render.instructions.set(call, hook("drawPiston", "(L" + call.owner + ";"
                    + Type.getArgumentTypes(render.desc)[0].getDescriptor() + ")V"));
            return;
        }
        throw new IllegalStateException("Could not locate the piston mesh draw");
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}

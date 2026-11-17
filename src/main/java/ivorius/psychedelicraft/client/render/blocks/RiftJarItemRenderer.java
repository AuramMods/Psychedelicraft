package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.item.RiftJarItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
public final class RiftJarItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final RiftJarBlockEntity jar=new RiftJarBlockEntity(BlockPos.ZERO,PSBlocks.RIFT_JAR.get().defaultBlockState());
    public RiftJarItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay){jar.setAmount(RiftJarItem.getRiftFraction(stack));jar.setPreviewAge((int)((System.currentTimeMillis()%500)/100));Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(jar,pose,buffers,light,overlay);}
}

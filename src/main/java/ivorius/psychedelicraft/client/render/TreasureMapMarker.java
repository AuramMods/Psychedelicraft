package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.world.gen.TreasureMapLoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapDecoration;
import org.lwjgl.opengl.GL11;

public final class TreasureMapMarker {
    private static final ResourceLocation TEXTURE = Psychedelicraft.id("textures/map/red_x.png");

    private TreasureMapMarker() {
    }

    public static boolean render(MapDecoration decoration, int index) {
        if (decoration.getType() != TreasureMapLoot.redX()) return false;
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(decoration.getX() / 2F + 64F, decoration.getY() / 2F + 64F, -0.02F);
        GlStateManager.rotate(decoration.getRotation() * 22.5F, 0F, 0F, 1F);
        GlStateManager.scale(4F, 4F, 3F);
        GlStateManager.translate(-0.125F, 0.125F, 0F);
        double depth = -0.001F * index;
        Tessellator draw = Tessellator.getInstance();
        BufferBuilder quad = draw.getBuffer();
        quad.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        quad.pos(-1, 1, depth).tex(0, 0).endVertex();
        quad.pos(1, 1, depth).tex(1, 0).endVertex();
        quad.pos(1, -1, depth).tex(1, 1).endVertex();
        quad.pos(-1, -1, depth).tex(0, 1).endVertex();
        draw.draw();
        GlStateManager.popMatrix();
        return true;
    }
}

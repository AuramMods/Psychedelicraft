package ivorius.psychedelicraft.client.render.block;

import java.util.List;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.JuniperSignBlock;
import ivorius.psychedelicraft.block.JuniperSignBlock.Form;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import ivorius.psychedelicraft.block.entity.JuniperSignText;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

public final class JuniperSignRenderer extends TileEntitySpecialRenderer<JuniperSignBlockEntity> {
    public static final ResourceLocation TEXTURE = Psychedelicraft.id("textures/entity/signs/juniper.png");
    private static final ResourceLocation HANGING_TEXTURE = Psychedelicraft.id("textures/entity/signs/hanging/juniper.png");
    private static final float MODEL_SCALE = 2F / 3F;
    private static final float TEXT_SCALE = MODEL_SCALE / 64F;
    private static final int[] DYE_TEXT_COLORS = {
            16777215, 16738335, 16711935, 10141901, 16776960, 12582656, 16738740, 8421504,
            13882323, 65535, 10494192, 255, 9127187, 65280, 16711680, 0
    };
    private final ModelSign model = new ModelSign();
    private final JuniperHangingSignModel hangingModel = new JuniperHangingSignModel();

    @Override
    public void render(JuniperSignBlockEntity sign, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!(sign.getBlockType() instanceof JuniperSignBlock)) {
            return;
        }
        JuniperSignBlock block = (JuniperSignBlock) sign.getBlockType();
        Form form = block.form();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + (form.isHanging() ? 0.625 : 0.5), z + 0.5);
        GlStateManager.rotate(-block.rotationDegrees(sign.getWorld().getBlockState(sign.getPos())), 0, 1, 0);
        if (form == Form.WALL) {
            GlStateManager.translate(0, -0.3125, -0.4375);
        }
        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4, 2, 1);
            GlStateManager.translate(0.0625, 0.0625, 0.0625);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        } else {
            bindTexture(form.isHanging() ? HANGING_TEXTURE : TEXTURE);
        }
        GlStateManager.color(1, 1, 1, alpha);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        renderModel(form, sign.hasAttachedChains());
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        if (destroyStage >= 0) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        } else {
            boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            if (lighting) {
                GlStateManager.disableLighting();
            }
            GlStateManager.depthMask(false);
            boolean nearby = x * x + y * y + z * z < 256;
            drawFace(form, sign.text(true), true, nearby);
            drawFace(form, sign.text(false), false, nearby);
            GlStateManager.depthMask(true);
            if (lighting) {
                GlStateManager.enableLighting();
            }
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private void renderModel(Form form, boolean attached) {
        if (form.isHanging()) {
            GlStateManager.scale(1, -1, -1);
            boolean culling = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            if (culling) {
                GlStateManager.disableCull();
            }
            hangingModel.render(form == Form.HANGING_WALL, form == Form.HANGING_CEILING && attached);
            if (culling) {
                GlStateManager.enableCull();
            }
        } else {
            GlStateManager.scale(MODEL_SCALE, -MODEL_SCALE, -MODEL_SCALE);
            model.signStick.showModel = form == Form.STANDING;
            model.renderSign();
        }
    }

    private void drawFace(Form form, JuniperSignText text, boolean front, boolean nearby) {
        if (!text.hasText()) {
            return;
        }
        GlStateManager.pushMatrix();
        if (!front) {
            GlStateManager.rotate(180, 0, 1, 0);
        }
        float scale = TEXT_SCALE;
        if (form.isHanging()) {
            GlStateManager.translate(0, -0.32F, 0.073F);
            scale = 0.9F / 64F;
        } else {
            GlStateManager.translate(0, 1F / 3F, 0.046666667F);
        }
        GlStateManager.scale(scale, -scale, scale);
        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;
        if (text.isGlowing()) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        }
        FontRenderer font = getFontRenderer();
        int color = textColor(text.color(), text.isGlowing());
        boolean outline = text.isGlowing() && (nearby || text.color() == EnumDyeColor.BLACK);
        for (int index = 0; index < JuniperSignText.LINE_COUNT; index++) {
            List<ITextComponent> parts = GuiUtilRenderComponents.splitText(text.line(index), form.textWidth(), font, false, true);
            if (!parts.isEmpty()) {
                String line = parts.get(0).getFormattedText();
                int x = -font.getStringWidth(line) / 2;
                int y = (index - 2) * form.textLineHeight();
                if (outline) {
                    drawOutline(font, line, x, y, text.color());
                }
                font.drawString(line, x, y, color);
            }
        }
        if (text.isGlowing()) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        }
        GlStateManager.popMatrix();
    }

    private static void drawOutline(FontRenderer font, String line, int x, int y, EnumDyeColor dye) {
        String plain = TextFormatting.getTextWithoutFormattingCodes(line);
        int color = dye == EnumDyeColor.BLACK ? 0xF0EBCC : textColor(dye, false);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    font.drawString(plain, x + dx, y + dy, color);
                }
            }
        }
    }

    public static int textColor(EnumDyeColor dye, boolean glowing) {
        int rgb = DYE_TEXT_COLORS[dye.getMetadata()];
        if (glowing) {
            return rgb;
        }
        int red = (int) ((rgb >> 16 & 255) * 0.4);
        int green = (int) ((rgb >> 8 & 255) * 0.4);
        int blue = (int) ((rgb & 255) * 0.4);
        return red << 16 | green << 8 | blue;
    }
}

package ivorius.psychedelicraft.client.screen;

import java.io.IOException;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.JuniperSignBlock.Form;
import ivorius.psychedelicraft.block.entity.JuniperSignText;
import ivorius.psychedelicraft.client.render.block.JuniperSignRenderer;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.network.INetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

public final class JuniperSignScreen extends GuiScreen {
    private static final int BOARD_WIDTH = 144;
    private static final int BOARD_HEIGHT = 72;
    private static final int LINE_HEIGHT = 13;
    private static final ResourceLocation HANGING_BACKGROUND = Psychedelicraft.id("textures/gui/hanging_signs/juniper.png");

    private final INetHandler connection;
    private final BlockPos pos;
    private final int dimension;
    private final long token;
    private final boolean front;
    private final Form form;
    private final EnumDyeColor color;
    private final boolean glowing;
    private final String[] lines;
    private final GuiTextField[] fields = new GuiTextField[JuniperSignText.LINE_COUNT];
    private int selected;
    private int boardTop;

    private JuniperSignScreen(INetHandler connection, BlockPos pos, int dimension, long token, boolean front,
                              Form form, EnumDyeColor color, boolean glowing, String[] lines) {
        this.connection = connection;
        this.pos = pos;
        this.dimension = dimension;
        this.token = token;
        this.front = front;
        this.form = form;
        this.color = color;
        this.glowing = glowing;
        this.lines = lines;
    }

    public static void open(INetHandler connection, BlockPos pos, int dimension, long token, boolean front,
                            Form form, EnumDyeColor color, boolean glowing, String[] lines) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() == connection && client.world != null && client.world.provider.getDimension() == dimension) {
            client.displayGuiScreen(new JuniperSignScreen(connection, pos, dimension, token, front, form, color, glowing, lines));
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        boardTop = height / 2 - 57;
        int maxWidth = form.textWidth();
        int textTop = boardTop + (form.isHanging() ? 29 : 13);
        int lineHeight = lineHeight();
        for (int index = 0; index < fields.length; index++) {
            if (fields[index] != null) {
                lines[index] = fields[index].getText();
            }
            GuiTextField field = new GuiTextField(index, fontRenderer, width / 2, textTop + index * lineHeight, maxWidth, lineHeight);
            field.setMaxStringLength(JuniperSignText.MAX_LINE_LENGTH);
            field.setValidator(value -> fontRenderer.getStringWidth(value) <= maxWidth);
            field.setEnableBackgroundDrawing(false);
            field.setTextColor(JuniperSignRenderer.textColor(color, glowing));
            field.setText(fontRenderer.trimStringToWidth(lines[index], maxWidth));
            fields[index] = field;
        }
        select(selected);
        buttonList.clear();
        int buttonOffset = form.isHanging() ? 95 : 121;
        addButton(new GuiButton(0, width / 2 - 100, Math.min(height - 27, boardTop + buttonOffset), I18n.format("gui.done")));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (mc.getConnection() == connection && mc.world != null && mc.world.provider.getDimension() == dimension) {
            for (int index = 0; index < fields.length; index++) {
                if (fields[index] != null) {
                    lines[index] = fields[index].getText();
                }
            }
            Channel.submitSignEdit(pos, dimension, token, lines);
        }
    }

    @Override
    public void updateScreen() {
        fields[selected].updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && button.enabled) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        switch (key) {
            case Keyboard.KEY_UP:
                select((selected + fields.length - 1) % fields.length);
                return;
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                select((selected + 1) % fields.length);
                return;
            default:
                if (!fields[selected].textboxKeyTyped(character, key)) {
                    super.keyTyped(character, key);
                }
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        int boardWidth = form.isHanging() ? 72 : BOARD_WIDTH;
        if (button != 0 || x < width / 2 - boardWidth / 2 || x >= width / 2 + boardWidth / 2) {
            return;
        }
        for (int index = 0; index < fields.length; index++) {
            GuiTextField field = fields[index];
            if (y >= field.y && y < field.y + lineHeight()) {
                select(index);
                if (x < field.x) {
                    field.setCursorPositionZero();
                } else if (!field.mouseClicked(x, y, button)) {
                    field.setCursorPositionEnd();
                }
                field.setFocused(true);
                break;
            }
        }
    }

    private int lineHeight() {
        return form.isHanging() ? form.textLineHeight() : LINE_HEIGHT;
    }

    private void select(int index) {
        fields[selected].setFocused(false);
        selected = index;
        fields[selected].setFocused(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        String title = I18n.format(form.isHanging() ? "gui.psychedelicraft.hanging_sign.edit" : "sign.edit");
        drawCenteredString(fontRenderer, title, width / 2, Math.max(8, boardTop - 21), 0xFFFFFF);
        GlStateManager.color(1, 1, 1, 1);
        if (form.isHanging()) {
            mc.getTextureManager().bindTexture(HANGING_BACKGROUND);
            drawScaledCustomSizeModalRect(width / 2 - 36, boardTop, 0, 0, 16, 16, 72, 72, 16, 16);
        } else {
            mc.getTextureManager().bindTexture(JuniperSignRenderer.TEXTURE);
            if (form == Form.STANDING) {
                drawScaledCustomSizeModalRect(width / 2 - 6, boardTop + BOARD_HEIGHT, 2, 16, 2, 14, 12, 40, 64, 32);
            }
            drawScaledCustomSizeModalRect(width / 2 - BOARD_WIDTH / 2, boardTop, front ? 2 : 28, 2,
                    24, 12, BOARD_WIDTH, BOARD_HEIGHT, 64, 32);
        }
        for (GuiTextField field : fields) {
            field.x = width / 2 - fontRenderer.getStringWidth(field.getText()) / 2;
            field.drawTextBox();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

package ivorius.psychedelicraft.block.entity;

import com.google.gson.JsonParseException;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.event.ClickEvent;

public final class JuniperSignText {
    public static final int LINE_COUNT = 4;
    public static final int MAX_LINE_LENGTH = 384;

    private final ITextComponent[] lines = new ITextComponent[LINE_COUNT];
    private EnumDyeColor color = EnumDyeColor.BLACK;
    private boolean glowing;

    public JuniperSignText() {
        for (int index = 0; index < lines.length; index++) {
            lines[index] = new TextComponentString("");
        }
    }

    public ITextComponent line(int index) {
        return lines[index];
    }

    public boolean hasText() {
        for (ITextComponent line : lines) {
            if (!line.getUnformattedText().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasClickCommands() {
        for (ITextComponent line : lines) {
            ClickEvent click = line.getStyle().getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND) {
                return true;
            }
        }
        return false;
    }

    void resolve(ICommandSender source) {
        for (int index = 0; index < lines.length; index++) {
            try {
                lines[index] = TextComponentUtils.processComponent(source, lines[index], null);
            } catch (CommandException ignored) {
                // Keep the authored component if the referenced entity or score cannot be resolved.
            }
        }
    }

    boolean isEditable() {
        for (ITextComponent line : lines) {
            if (!(line instanceof TextComponentString)) {
                return false;
            }
        }
        return true;
    }

    public EnumDyeColor color() {
        return color;
    }

    boolean setColor(EnumDyeColor value) {
        if (color == value) {
            return false;
        }
        color = value;
        return true;
    }

    public boolean isGlowing() {
        return glowing;
    }

    boolean setGlowing(boolean value) {
        if (glowing == value) {
            return false;
        }
        glowing = value;
        return true;
    }

    void setPlainLines(String[] text) {
        for (int index = 0; index < lines.length; index++) {
            String line = ChatAllowedCharacters.filterAllowedCharacters(text[index]);
            lines[index] = new TextComponentString(line.substring(0, Math.min(line.length(), MAX_LINE_LENGTH)));
        }
    }

    NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        for (int index = 0; index < lines.length; index++) {
            tag.setString("Text" + (index + 1), ITextComponent.Serializer.componentToJson(lines[index]));
        }
        tag.setByte("Color", (byte) color.getMetadata());
        tag.setBoolean("Glowing", glowing);
        return tag;
    }

    void read(NBTTagCompound tag) {
        for (int index = 0; index < lines.length; index++) {
            ITextComponent line = null;
            try {
                line = ITextComponent.Serializer.jsonToComponent(tag.getString("Text" + (index + 1)));
            } catch (JsonParseException ignored) {
                // A malformed line leaves this slot blank and the other lines intact.
            }
            lines[index] = line == null ? new TextComponentString("") : line;
        }
        int metadata = tag.getByte("Color");
        color = tag.hasKey("Color", 99) && metadata >= 0 && metadata < 16
                ? EnumDyeColor.byMetadata(metadata) : EnumDyeColor.BLACK;
        glowing = tag.getBoolean("Glowing");
    }
}

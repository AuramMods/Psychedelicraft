package ivorius.psychedelicraft.block.entity;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JuniperSignTextTest {
    @Test
    public void roundTripsLinesColorAndGlow() {
        JuniperSignText source = new JuniperSignText();
        source.setPlainLines(new String[]{"Juniper", "Zażółć", "", "Trail"});
        source.setColor(EnumDyeColor.CYAN);
        source.setGlowing(true);

        JuniperSignText restored = new JuniperSignText();
        restored.read(source.write());
        assertEquals("Juniper", restored.line(0).getUnformattedText());
        assertEquals("Zażółć", restored.line(1).getUnformattedText());
        assertEquals("", restored.line(2).getUnformattedText());
        assertEquals("Trail", restored.line(3).getUnformattedText());
        assertEquals(EnumDyeColor.CYAN, restored.color());
        assertTrue(restored.isGlowing());
    }

    @Test
    public void playerEditsStayLiteralAndStripIllegalCharacters() {
        JuniperSignText text = new JuniperSignText();
        String line = "{\"text\":\"label\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"say hi\"}}";
        text.setPlainLines(new String[]{line, "bad\u0000\u00a7char", "", ""});
        assertEquals(line, text.line(0).getUnformattedText());
        assertNull(text.line(0).getStyle().getClickEvent());
        assertEquals("badchar", text.line(1).getUnformattedText());
    }

    @Test
    public void malformedStoredLineDoesNotEraseOtherLinesOrChangeDefaults() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Text1", "{");
        tag.setString("Text2", "{\"text\":\"still here\"}");
        tag.setByte("Color", (byte) 33);
        JuniperSignText restored = new JuniperSignText();
        restored.read(tag);
        assertEquals("", restored.line(0).getUnformattedText());
        assertEquals("still here", restored.line(1).getUnformattedText());
        assertEquals(EnumDyeColor.BLACK, restored.color());
        assertFalse(restored.isGlowing());
    }
}

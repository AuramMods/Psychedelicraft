package ivorius.psychedelicraft.client;

import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MessageDistorterTest {
    private static MessageDistorter effect(float alcohol,float zero,float cannabis){return new MessageDistorter(RandomSource.create(821),alcohol,zero,cannabis,key->false,key->key);}

    @Test public void soberTextPreservesUnicodeFormattingAndLineBreaks(){
        String text="\u00a7aA\uD83D\uDE00B\nC\r zażółć";
        assertEquals(text,effect(0,0,0).distort(text,true));
    }

    @Test public void fullZeroProducesOneBinaryDigitPerVisibleCodePoint(){
        assertTrue(effect(0,1,0).distort("\u00a7aA\uD83D\uDE00B\nC",true).matches("\u00a7a[01]{3}\n[01]"));
    }

    @Test public void outgoingTextCannotBecomeACommandOrSplitASurrogatePair(){
        assertTrue(ClientChatEffects.startsWithSlash(" \t/list"));assertTrue(ClientChatEffects.startsWithSlash("\u00a0/list"));
        assertEquals(" \t?list",ClientChatEffects.keepAsChat(" \t/list"));assertEquals("\u00a0?list",ClientChatEffects.keepAsChat("\u00a0/list"));
        assertFalse(ClientChatEffects.startsWithSlash("ordinary /chat"));assertEquals("ordinary /chat",ClientChatEffects.keepAsChat("ordinary /chat"));
        String prefix="a".repeat(255);assertEquals(prefix,ClientChatEffects.keepAsChat(prefix+"\uD83D\uDE00b"));
        assertEquals(256,ClientChatEffects.keepAsChat("a".repeat(256)+"more").length());
        assertEquals(256,ClientChatEffects.keepAsChat("a".repeat(254)+"\uD83D\uDE00more").length());
        assertEquals("",ClientChatEffects.keepAsChat(""));
    }

    @Test public void cannabisUsesLocalizedStartAndMidSentenceVariants(){
        RandomSource random=new LegacyRandomSource(0){@Override public float nextFloat(){return 0;}@Override public int nextInt(int bound){return 0;}};
        Map<String,String> translations=Map.of("distortion.psychedelicraft.start_filler_words.0","Człeku,","distortion.psychedelicraft.filler_words.0",", no,");
        var effect=new MessageDistorter(random,0,0,1,translations::containsKey,translations::get);
        assertEquals("Człeku, hello, no, world",effect.distort("hello world",true));
        assertEquals("hello, no, world",effect.distort("hello world",false));
    }

    @Test public void alcoholSlursSyllablesAndEventuallyFinishesRewinds(){
        String result=effect(.8F,0,0).distort("ssss some sober sounding syllables ssss",true);
        assertNotEquals("ssss some sober sounding syllables ssss",result);assertTrue(result.contains("sh")||result.contains("Sh"));
        // A long input exercises rewinds and repetitions while the final outgoing packet stays bounded.
        String longResult=effect(1,0,0).distort("some syllables ".repeat(18),true);
        assertFalse(longResult.isEmpty());assertTrue(ClientChatEffects.keepAsChat(longResult).length()<=256);
    }

    @Test public void styledComponentsKeepInheritedClickHoverFontAndInsertion(){
        var click=new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/help");
        var hover=new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal("unchanged hover"));
        var font=new ResourceLocation("minecraft:uniform");
        Style root=Style.EMPTY.withColor(0x123456).withClickEvent(click).withHoverEvent(hover).withInsertion("unchanged insertion").withFont(font);
        Component input=Component.literal("AB").setStyle(root).append(Component.literal("\uD83D\uDE00C").withStyle(net.minecraft.ChatFormatting.BOLD));
        Component output=ClientChatEffects.distort(input,effect(0,1,0));
        assertTrue(output.getString().matches("[01]{4}"));assertEquals("AB\uD83D\uDE00C",input.getString());
        List<Style> styles=new ArrayList<>();output.visit((Style style,String value)->{if(!value.isEmpty())styles.add(style);return Optional.empty();},Style.EMPTY);
        assertEquals(2,styles.size());
        for(Style style:styles){assertEquals(root.getColor(),style.getColor());assertEquals(click,style.getClickEvent());assertEquals(hover,style.getHoverEvent());assertEquals(font,style.getFont());assertEquals("unchanged insertion",style.getInsertion());}
        assertFalse(styles.get(0).isBold());assertTrue(styles.get(1).isBold());
    }
}

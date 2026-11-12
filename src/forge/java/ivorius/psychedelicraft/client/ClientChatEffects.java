package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class ClientChatEffects {
    private static final int MAX_CHAT_LENGTH = 256;

    private ClientChatEffects() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChatSend(ClientChatEvent event) {
        String message = event.getMessage();
        if (!PSConfig.balancing.messageDistortion.outgoing || startsWithSlash(message)) return;
        MessageDistorter effect = activeDistorter();
        if (effect != null) event.setMessage(keepAsChat(effect.distort(message, true)));
    }

    public static ITextComponent distortIncoming(ITextComponent message) {
        if (!PSConfig.balancing.messageDistortion.incoming) return message;
        MessageDistorter effect = activeDistorter();
        if (effect == null) return message;

        ITextComponent result = new TextComponentString("");
        boolean first = true;
        for (ITextComponent part : message) {
            String text = part.getUnformattedComponentText();
            if (text.isEmpty()) continue;
            result.appendSibling(new TextComponentString(effect.distort(text, first)).setStyle(part.getStyle().createDeepCopy()));
            first = false;
        }
        return result;
    }

    @Nullable
    private static MessageDistorter activeDistorter() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        DrugProperties drugs = player == null ? null : DrugCapability.get(player);
        if (drugs == null) return null;
        float alcohol = (float) Math.min(drugs.getActiveValue(DrugType.ALCOHOL) + drugs.getActiveValue(DrugType.KAVA), 1);
        float zero = (float) drugs.getActiveValue(DrugType.ZERO);
        float cannabis = (float) drugs.getActiveValue(DrugType.CANNABIS);
        return alcohol > 0 || zero > 0 || cannabis > 0 ? new MessageDistorter(player.getRNG(), alcohol, zero, cannabis) : null;
    }

    private static boolean startsWithSlash(String text) {
        int first = firstNonWhitespace(text);
        return first < text.length() && text.charAt(first) == '/';
    }

    static String keepAsChat(String text) {
        int limit = Math.min(text.length(), MAX_CHAT_LENGTH);
        if (limit < text.length() && Character.isHighSurrogate(text.charAt(limit - 1))
                && Character.isLowSurrogate(text.charAt(limit))) limit--;
        String trimmed = text.substring(0, limit);
        int first = firstNonWhitespace(trimmed);
        // The chat packet treats a slash at the start as a command, even after trimming whitespace.
        return first < trimmed.length() && trimmed.charAt(first) == '/'
                ? trimmed.substring(0, first) + '?' + trimmed.substring(first + 1) : trimmed;
    }

    private static int firstNonWhitespace(String text) {
        int index = 0;
        while (index < text.length()
                && (Character.isWhitespace(text.charAt(index)) || Character.isSpaceChar(text.charAt(index)))) index++;
        return index;
    }
}

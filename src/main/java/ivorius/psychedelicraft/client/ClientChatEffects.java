/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nullable;
import java.util.Optional;

/** Transform outgoing text before native signing and incoming presentation after native validation. */
@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT)
public final class ClientChatEffects {
    private static final int MAX_CHAT_LENGTH=256;
    private ClientChatEffects(){}

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void send(ClientChatEvent event){
        if(!PSConfig.DISTORT_OUTGOING_MESSAGES.get()||startsWithSlash(event.getMessage()))return;
        var effect=activeDistorter();
        if(effect!=null)event.setMessage(keepAsChat(effect.distort(event.getMessage(),true)));
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void receive(ClientChatReceivedEvent event){
        // The accepted hook acts on chat lines, not action-bar/HUD status messages.
        if(event instanceof ClientChatReceivedEvent.System system&&system.isOverlay())return;
        event.setMessage(distortIncoming(event.getMessage()));
    }

    /** Also used for local Reggie dialogue, which does not pass through network chat events. */
    public static Component distortIncoming(Component message){
        if(!PSConfig.DISTORT_INCOMING_MESSAGES.get())return message;
        var effect=activeDistorter();
        return effect==null?message:distort(message,effect);
    }

    static Component distort(Component message,MessageDistorter effect){
        MutableComponent result=Component.empty();
        boolean[] first={true};
        message.visit((Style style,String text)->{
            if(!text.isEmpty()){
                result.append(Component.literal(effect.distort(text,first[0])).setStyle(style));
                first[0]=false;
            }
            return Optional.empty();
        },Style.EMPTY);
        return result;
    }

    @Nullable private static MessageDistorter activeDistorter(){
        var player=Minecraft.getInstance().player;
        if(!ClientDrugState.alive(player))return null;
        var drugs=ClientDrugState.get(player);
        float alcohol=(float)Math.min(drugs.getActiveValue(DrugType.ALCOHOL)+drugs.getActiveValue(DrugType.KAVA),1);
        float zero=(float)drugs.getActiveValue(DrugType.ZERO),cannabis=(float)drugs.getActiveValue(DrugType.CANNABIS);
        return alcohol>0||zero>0||cannabis>0?new MessageDistorter(player.getRandom(),alcohol,zero,cannabis):null;
    }

    static boolean startsWithSlash(String text){int first=firstNonWhitespace(text);return first<text.length()&&text.charAt(first)=='/';}
    static String keepAsChat(String text){
        int limit=Math.min(text.length(),MAX_CHAT_LENGTH);
        if(limit<text.length()&&Character.isHighSurrogate(text.charAt(limit-1))&&Character.isLowSurrogate(text.charAt(limit)))limit--;
        String trimmed=text.substring(0,limit);
        int first=firstNonWhitespace(trimmed);
        return first<trimmed.length()&&trimmed.charAt(first)=='/'?trimmed.substring(0,first)+'?'+trimmed.substring(first+1):trimmed;
    }
    private static int firstNonWhitespace(String text){int i=0;while(i<text.length()&&(Character.isWhitespace(text.charAt(i))||Character.isSpaceChar(text.charAt(i))))i++;return i;}
}

/* Copyright (c) 2014, Lukas Tenbrink. Modern integration originally by Sollace. */
package ivorius.psychedelicraft.client.render;
import ivorius.psychedelicraft.client.ClientDrugState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT)
public final class ContextualHallucinations {
    private static ContextualHallucinations current;
    private final LocalPlayer player;
    private final ClientLevel world;
    private final List<RastaHeadHallucination> heads=new ArrayList<>();
    private final List<EntityApparition> apparitions=new ArrayList<>();
    private final List<IdentitySwapHallucination> swaps=new ArrayList<>();
    private float naturalStrength;
    private int forcedTicks,previousForcedTicks;
    private ContextualHallucinations(LocalPlayer player,ClientLevel world){this.player=player;this.world=world;}
    public static void track(@Nullable LocalPlayer player,@Nullable ClientLevel world){
        if(player==null||world==null)current=null;
        else if(current==null||current.player!=player||current.world!=world)current=new ContextualHallucinations(player,world);
    }
    public static void receive(Connection connection,DrugNetwork.ForcedHallucination message){
        var client=Minecraft.getInstance();var player=client.player;
        if(client.getConnection()==null||client.getConnection().getConnection()!=connection||client.level==null||!ClientDrugState.alive(player)||message.type()==null
                ||player.getId()!=message.entityId()||!player.getUUID().equals(message.player())||!client.level.dimension().location().equals(message.dimension()))return;
        track(player,client.level);
        current.add(message.type(),message.position()==null?null:Vec3.atCenterOf(message.position()));
        current.forcedTicks=current.previousForcedTicks=HallucinationTiming.FORCED_TICKS;
    }
    public static void tick(DrugProperties drugs,float strength){
        var state=current;if(state==null)return;
        state.naturalStrength=strength;state.previousForcedTicks=state.forcedTicks;if(state.forcedTicks>0)state.forcedTicks--;
        float chance=strength*.05F;var random=state.player.getRandom();
        if(chance>0&&random.nextInt(Math.max(1,(int)(1/chance)))==0)state.add(pickNatural(random,(float)drugs.getActiveValue(DrugType.CANNABIS),!state.heads.isEmpty()),null);
        boolean visible=state.forcedAlpha(1)>0||strength>0;
        state.heads.removeIf(head->head.tick(visible));state.apparitions.removeIf(EntityApparition::tick);state.swaps.removeIf(IdentitySwapHallucination::tick);
    }
    static HallucinationType pickNatural(RandomSource random,float cannabis,boolean existingHead){
        if(random.nextFloat()<=.1F&&cannabis>.4F&&!existingHead)return HallucinationType.RASTA_HEAD;
        if(random.nextFloat()<=.5F)return HallucinationType.MULTIPLE_ENTITY;
        // Preserve the pinned registry order, which reaches the friendly swap before the solo apparition.
        return HallucinationType.FRIENDLY_ZOMBIES;
    }
    private void add(HallucinationType type,@Nullable Vec3 position){
        switch(type){
            case RASTA_HEAD->heads.add(new RastaHeadHallucination(player,position==null?player.position():position));
            case SINGLE_ENTITY,MULTIPLE_ENTITY->apparitions.add(new EntityApparition(player,type,position));
            case HOSTILE_VILLAGERS,FRIENDLY_ZOMBIES->swaps.add(new IdentitySwapHallucination(type));
        }
    }
    private float forcedAlpha(float partial){return HallucinationTiming.forcedAlpha(previousForcedTicks,forcedTicks,partial,player.tickCount);}
    @Nullable private static ContextualHallucinations forClient(){var client=Minecraft.getInstance();return current!=null&&ClientDrugState.alive(client.player)&&current.player==client.player&&current.world==client.level?current:null;}
    public static Entity forRender(Entity original){
        var state=forClient();if(state==null||original.level()!=state.world||ApparitionRender.isRendering())return original;
        for(var swap:state.swaps){Entity replacement=swap.disguise(original);if(replacement!=null)return replacement;}
        return original;
    }
    public static void render(Minecraft client,float partial,PoseStack poses){
        var state=forClient();if(state==null)return;
        float alpha=HallucinationTiming.strength(state.naturalStrength,state.forcedAlpha(partial));
        state.heads.forEach(head->head.render(client,partial,alpha,poses));
        var iterator=state.apparitions.iterator();while(iterator.hasNext())try{iterator.next().render(client,partial,alpha,poses);}catch(RuntimeException failure){iterator.remove();LogUtils.getLogger().warn("Could not render detached apparition",failure);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void chat(ClientChatReceivedEvent.Player event){
        var state=forClient();if(state==null)return;
        String sender=event.getBoundChatType().name().getString();state.heads.forEach(head->head.chat.receive(sender,event.getMessage()));
    }
}

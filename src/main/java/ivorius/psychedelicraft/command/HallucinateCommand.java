/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class HallucinateCommand {
    private static final DynamicCommandExceptionType UNKNOWN=new DynamicCommandExceptionType(id->Component.translatable("commands.psychedelicraft.hallucinate.unknown",id));
    private HallucinateCommand(){}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("hallucinate").requires(source->source.hasPermission(2))
                .then(Commands.argument("type",ResourceLocationArgument.id()).suggests((context,builder)->SharedSuggestionProvider.suggest(Arrays.stream(HallucinationType.values()).flatMap(type->java.util.stream.Stream.of(type.id().toString(),type.id().getPath())),builder))
                        .executes(context->run(context,List.of(context.getSource().getPlayerOrException()),null))
                        .then(Commands.argument("position",BlockPosArgument.blockPos()).executes(context->run(context,List.of(context.getSource().getPlayerOrException()),BlockPosArgument.getBlockPos(context,"position"))))
                        .then(Commands.argument("targets",EntityArgument.players()).executes(context->run(context,EntityArgument.getPlayers(context,"targets"),null))
                                .then(Commands.argument("position",BlockPosArgument.blockPos()).executes(context->run(context,EntityArgument.getPlayers(context,"targets"),BlockPosArgument.getBlockPos(context,"position")))))));
    }
    private static int run(CommandContext<CommandSourceStack> context,Collection<ServerPlayer> players,BlockPos position)throws CommandSyntaxException{
        var id=ResourceLocationArgument.getId(context,"type");var type=HallucinationType.byName(id.getNamespace().equals("minecraft")?id.getPath():id.toString());if(type==null)throw UNKNOWN.create(id);
        for(var player:players)DrugNetwork.hallucinate(player,type,position);
        context.getSource().sendSuccess(()->Component.translatable("commands.psychedelicraft.hallucinate.success"),true);return players.size();
    }
}

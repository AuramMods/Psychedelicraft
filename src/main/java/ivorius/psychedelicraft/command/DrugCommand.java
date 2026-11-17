package ivorius.psychedelicraft.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/** Brigadier adapter for the accepted command behavior and donor command by Sollace. */
@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class DrugCommand {
    private static final String KEY="commands.psychedelicraft.drug.";
    private DrugCommand() {}

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var target=Commands.argument("target",EntityArgument.players());
        target.then(Commands.literal("get").executes(context -> get(context,null))
                .then(drug(false).executes(context -> get(context,resolve(drugId(context))))));
        for (String action : List.of("set","add","lock")) {
            var choice=drug(true);
            if (action.equals("lock")) choice.then(Commands.argument("locked",BoolArgumentType.bool()).executes(context -> change(context,action)));
            else choice.then(Commands.argument("value",DoubleArgumentType.doubleArg(0,1)).executes(context -> change(context,action)));
            target.then(Commands.literal(action).then(choice));
        }
        event.getDispatcher().register(Commands.literal("drug").requires(source -> source.hasPermission(2)).then(target));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,ResourceLocation> drug(boolean includeAll) {
        return Commands.argument("drug",ResourceLocationArgument.id()).suggests((context,builder) ->
                SharedSuggestionProvider.suggest(Stream.concat(includeAll?Stream.of("all"):Stream.empty(),DrugType.values().stream()
                        .flatMap(type -> Stream.of(type.id(),path(type)))),builder));
    }

    /** Keep the explicit namespace, while bare IDs retain the accepted mod namespace. */
    static String drugId(CommandContext<CommandSourceStack> context) {
        return context.getNodes().stream().filter(node->node.getNode() instanceof com.mojang.brigadier.tree.ArgumentCommandNode<?,?>
                        &&node.getNode().getName().equals("drug"))
                .findFirst().orElseThrow().getRange().get(context.getInput());
    }

    private static DrugType resolve(String id) throws CommandSyntaxException {
        String full=id.contains(":")?id:Psychedelicraft.MOD_ID+":"+id;
        return DrugType.byId(full).orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable(KEY+"unknown",id)).create());
    }
    private static String path(DrugType type) { return type.id().substring(type.id().indexOf(':')+1); }

    private static int change(CommandContext<CommandSourceStack> context,String action) throws CommandSyntaxException {
        String id=drugId(context);
        boolean all=id.equals("all");
        Collection<DrugType> types=all?DrugType.values():List.of(resolve(id));
        var players=EntityArgument.getPlayers(context,"target");
        for(ServerPlayer player:players) {
            DrugProperties properties=DrugCapability.get(player);
            String operation=action;
            Object[] arguments;
            if(action.equals("lock")) {
                boolean locked=BoolArgumentType.getBool(context,"locked");
                types.forEach(type -> properties.setDrugLocked(type,locked));
                operation=locked?"lock":"unlock";
                arguments=all?new Object[0]:new Object[]{path(types.iterator().next())};
            } else {
                double value=DoubleArgumentType.getDouble(context,"value");
                types.forEach(type -> {
                    if(action.equals("set"))properties.setDrugValue(type,value);
                    else properties.addToDrug(type,value);
                });
                arguments=all?new Object[]{value}:new Object[]{path(types.iterator().next()),value};
            }
            report(context.getSource(),player,operation+(all?".all":""),arguments);
        }
        return players.size();
    }

    private static int get(CommandContext<CommandSourceStack> context,DrugType selected) throws CommandSyntaxException {
        var players=EntityArgument.getPlayers(context,"target");
        for(ServerPlayer player:players) {
            DrugProperties properties=DrugCapability.get(player);
            if(selected!=null) level(context.getSource(),player,properties,selected);
            else {
                var active=DrugType.values().stream().filter(type -> properties.getActiveValue(type)>0).toList();
                context.getSource().sendSuccess(() -> active.isEmpty()?Component.translatable(KEY+"sober",player.getDisplayName())
                        :Component.translatable(KEY+"active",player.getDisplayName(),active.size()),false);
                active.forEach(type -> level(context.getSource(),player,properties,type));
            }
        }
        return players.size();
    }
    private static void level(CommandSourceStack source,ServerPlayer player,DrugProperties properties,DrugType type) {
        source.sendSuccess(() -> source.getEntity()==player?Component.translatable(KEY+"level.self",path(type),properties.getActiveValue(type))
                :Component.translatable(KEY+"level",player.getDisplayName(),path(type),properties.getActiveValue(type)),false);
    }
    private static void report(CommandSourceStack source,ServerPlayer player,String action,Object[] arguments) {
        Object[] named=new Object[arguments.length+1];named[0]=player.getDisplayName();System.arraycopy(arguments,0,named,1,arguments.length);
        source.sendSuccess(() -> Component.translatable(KEY+action+(source.getEntity()==player?".self":""),source.getEntity()==player?arguments:named),true);
        if(source.getEntity()!=player&&source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            player.sendSystemMessage(Component.translatable(KEY+action+".changed",arguments));
        }
    }
}

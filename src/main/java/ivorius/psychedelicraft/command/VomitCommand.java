package ivorius.psychedelicraft.command;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class VomitCommand {
    private VomitCommand(){}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("vomit").requires(source->source.hasPermission(2))
            .executes(context->{var player=context.getSource().getPlayerOrException();DrugCapability.get(player).getStomach().vomit(player);return 1;})
            .then(Commands.argument("target",EntityArgument.player()).executes(context->{var player=EntityArgument.getPlayer(context,"target");DrugCapability.get(player).getStomach().vomit(player);return 1;})));}
}

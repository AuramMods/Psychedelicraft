package ivorius.psychedelicraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import org.junit.Test;
import static org.junit.Assert.*;

public class DrugCommandTest {
    @Test public void resourceArgumentAcceptsNamespacesAndDoesNotReadTheRootLiteral()throws Exception{
        for(String id:new String[]{"caffeine","psychedelicraft:caffeine","minecraft:caffeine","all"}){
            var dispatcher=new CommandDispatcher<CommandSourceStack>();
            dispatcher.register(Commands.literal("drug").then(Commands.argument("target",StringArgumentType.word())
                    .then(Commands.literal("get").then(Commands.argument("drug",ResourceLocationArgument.id())
                            .executes(context->{assertEquals(id,DrugCommand.drugId(context));return 1;})))));
            assertEquals(1,dispatcher.execute("drug TestPlayer get "+id,null));
        }
    }
}

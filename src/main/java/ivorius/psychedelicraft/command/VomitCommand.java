package ivorius.psychedelicraft.command;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public final class VomitCommand extends CommandBase {
    @Override
    public String getName() {
        return "vomit";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.psychedelicraft.vomit.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 1) throw new net.minecraft.command.WrongUsageException(getUsage(sender));
        EntityPlayerMP player = args.length == 0 ? getCommandSenderAsPlayer(sender) : getPlayer(server, sender, args[0]);
        DrugProperties properties = DrugCapability.get(player);
        if (properties != null) properties.getStomach().vomit(player);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}

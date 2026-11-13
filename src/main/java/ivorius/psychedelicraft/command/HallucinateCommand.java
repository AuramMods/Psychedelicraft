package ivorius.psychedelicraft.command;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class HallucinateCommand extends CommandBase {
    @Override
    public String getName() {
        return "hallucinate";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.psychedelicraft.hallucinate.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1 && args.length != 2 && args.length != 4) throw new WrongUsageException(getUsage(sender));
        HallucinationType type = HallucinationType.byName(args[0]);
        if (type == null) throw new CommandException("commands.psychedelicraft.hallucinate.unknown", args[0]);
        List<EntityPlayerMP> players = args.length == 2
                ? getPlayers(server, sender, args[1]) : Collections.singletonList(getCommandSenderAsPlayer(sender));
        BlockPos position = args.length == 4 ? parseBlockPos(sender, args, 1, false) : null;
        for (EntityPlayerMP player : players) Channel.hallucinate(player, type, position);
        notifyCommandListener(sender, this, "commands.psychedelicraft.hallucinate.success");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            List<String> ids = new ArrayList<>();
            for (HallucinationType type : HallucinationType.values()) {
                String id = type.id();
                ids.add(id.substring(id.indexOf(':') + 1));
                ids.add(id);
            }
            return getListOfStringsMatchingLastWord(args, ids);
        }
        if (args.length == 2) {
            List<String> choices = new ArrayList<>(Arrays.asList(server.getOnlinePlayerNames()));
            choices.add("~");
            if (pos != null) choices.add(Integer.toString(pos.getX()));
            return getListOfStringsMatchingLastWord(args, choices);
        }
        return args.length <= 4 ? getTabCompletionCoordinate(args, 1, pos) : Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return args.length == 2 && index == 1;
    }
}

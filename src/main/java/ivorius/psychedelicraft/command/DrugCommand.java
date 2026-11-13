package ivorius.psychedelicraft.command;

import com.google.common.collect.ObjectArrays;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Legacy command adapter for the donor command by Sollace. */
public final class DrugCommand extends CommandBase {
    private static final String KEY = "commands.psychedelicraft.drug.";

    @Override
    public String getName() {
        return "drug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return KEY + "usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new WrongUsageException(getUsage(sender));
        EntityPlayerMP player = getPlayer(server, sender, args[0]);
        DrugProperties drugs = DrugCapability.get(player);
        if (drugs == null) throw new CommandException("commands.generic.exception");
        String action = args[1];
        if ("get".equals(action)) {
            if (args.length == 2) sendActive(sender, player, drugs);
            else if (args.length == 3) {
                DrugType type = resolve(args[2]);
                double value = drugs.getActiveValue(type);
                sender.sendMessage(sender.getCommandSenderEntity() == player
                        ? new TextComponentTranslation(KEY + "level.self", path(type), value)
                        : new TextComponentTranslation(KEY + "level", player.getDisplayName(), path(type), value));
            } else throw new WrongUsageException(getUsage(sender));
            return;
        }

        if (args.length != 4) throw new WrongUsageException(getUsage(sender));
        boolean all = "all".equals(args[2]);
        DrugType effect = all ? null : resolve(args[2]);
        Iterable<DrugType> types = all ? DrugType.values() : Collections.singleton(effect);
        switch (action) {
            case "set":
            case "add":
                double amount = parseDouble(args[3], 0, 1);
                for (DrugType type : types) {
                    if ("set".equals(action)) drugs.setDrugValue(type, amount);
                    else drugs.addToDrug(type, amount);
                }
                if (all) report(sender, player, action + ".all", amount);
                else report(sender, player, action, path(effect), amount);
                break;
            case "lock":
                boolean locked = parseBoolean(args[3]);
                for (DrugType type : types) drugs.setDrugLocked(type, locked);
                String change = locked ? "lock" : "unlock";
                if (all) report(sender, player, change + ".all");
                else report(sender, player, change, path(effect));
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private void report(ICommandSender sender, EntityPlayerMP player, String action, Object... arguments) {
        String key = KEY + action;
        boolean self = sender.getCommandSenderEntity() == player;
        // Keep the named message in operator broadcasts and the server log.
        notifyCommandListener(sender, this, self ? 1 : 0, key, ObjectArrays.concat(player.getDisplayName(), arguments));
        if (sender.getEntityWorld().getGameRules().getBoolean("sendCommandFeedback")) {
            if (self) sender.sendMessage(new TextComponentTranslation(key + ".self", arguments));
            else player.sendMessage(new TextComponentTranslation(key + ".changed", arguments));
        }
    }

    private static DrugType resolve(String value) throws CommandException {
        String id = value.indexOf(':') < 0 ? Psychedelicraft.MOD_ID + ":" + value : value;
        return DrugType.byId(id).orElseThrow(() -> new CommandException(KEY + "unknown", value));
    }

    private static String path(DrugType type) {
        return type.id().substring(type.id().indexOf(':') + 1);
    }

    private static void sendActive(ICommandSender sender, EntityPlayerMP player, DrugProperties drugs) {
        List<DrugType> active = new ArrayList<>();
        for (DrugType type : DrugType.values()) {
            if (drugs.getActiveValue(type) > 0) active.add(type);
        }
        if (active.isEmpty()) {
            sender.sendMessage(new TextComponentTranslation(KEY + "sober", player.getDisplayName()));
            return;
        }
        sender.sendMessage(new TextComponentTranslation(KEY + "active", player.getDisplayName(), active.size()));
        for (DrugType type : active) {
            sender.sendMessage(new TextComponentTranslation(KEY + "level", player.getDisplayName(), path(type),
                    drugs.getActiveValue(type)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, "get", "set", "add", "lock");
        if (args.length == 3 && Arrays.asList("get", "set", "add", "lock").contains(args[1])) {
            List<String> names = new ArrayList<>();
            if (!"get".equals(args[1])) names.add("all");
            for (DrugType type : DrugType.values()) {
                names.add(path(type));
                names.add(type.id());
            }
            return getListOfStringsMatchingLastWord(args, names);
        }
        if (args.length == 4 && "lock".equals(args[1])) return getListOfStringsMatchingLastWord(args, "true", "false");
        return Collections.emptyList();
    }
}

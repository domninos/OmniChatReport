package net.omni.chatreport.commands;

import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {

    private final OmniChatReport plugin;
    private final String[] help;

    public ReportCommand(OmniChatReport plugin) {
        this.plugin = plugin;

        this.help = new String[]{
                "{report.use} &b/report <player> &8>> &7Open Reports GUI.",
                "{report.usechat} &b/report <player> <reason> &8>> &7Report a player with a reason.",
        };
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("report.use"))
            return noPerms(sender);

        if (args.length == 0) {
            return sendHelp(sender);
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!(sender.hasPermission("report.reload")))
                    return noPerms(sender);

                plugin.reloadConfig();
                plugin.updateConfig();
                plugin.sendMessage(sender, "&aReloaded config.");
                return true;
            }

            if (!(sender instanceof Player player))
                return playerOnly(sender);

            if (!sender.hasPermission("report.use"))
                return noPerms(sender);

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null || !target.isOnline()) {
                plugin.sendMessage(sender, "&cERROR! Player is not online.");
                return true;
            }

            plugin.getGuiManager().openGUI(player);
        } else if (args.length == 2) {
            // the one with reason

            if (!sender.hasPermission("report.usechat"))
                return noPerms(sender);

            // TODO
            //  /report <player> <reason> -
        }
        return sendHelp(sender);
    }

    private boolean noPerms(CommandSender sender) {
        plugin.sendMessage(sender, "&cNo permission.");
        return true;
    }

    private boolean sendHelp(CommandSender sender) {
        StringBuilder toSend = new StringBuilder();

        for (String text : help) {
            if (!text.startsWith("{report.")) {
                toSend.append(text).append("\n");
                continue;
            }

            String firstWord = text.split(" ")[0];

            if (sender.hasPermission(firstWord) || sender.isOp()) {
                String remaining = text.substring(firstWord.length()).trim();

                toSend.append(remaining).append("\n");
            }
        }

        plugin.sendMessage(sender, toSend.toString(), false);
        return true;
    }

    private boolean playerOnly(CommandSender sender) {
        plugin.sendMessage(sender, "&cOnly players can use this command.");
        return true;
    }

    public void register() {
        PluginCommand command = plugin.getCommand("report");

        if (command != null)
            command.setExecutor(this);
    }
}

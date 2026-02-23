package net.omni.chatreport.commands;

import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class MuteCommand implements CommandExecutor {
    private final OmniChatReport plugin;

    private final String[] help;

    public MuteCommand(OmniChatReport plugin) {
        this.plugin = plugin;

        this.help = new String[]{
                "{mute.use} &b/mute <player> <time> <reason> &8>> &7Basic mute command with time and reason.",
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mute.use"))
            return noPerms(sender);

        if (args.length < 3)
            return sendHelp(sender);
        else {
            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                plugin.sendMessage(sender, "&cPlayer '" + args[1] + "' not found.");
                return true;
            }

            String timeString = args[2];

            StringBuilder builder = new StringBuilder();

            for (int i = 1; i < args.length; i++)
                builder.append(args[i]).append(" ");

            String reason = builder.toString().trim();

            if (plugin.getMuteManager().mutePlayer(sender.getName(), target, timeString, reason)) {
                long timeMillis = TimeUtil.parseDuration(timeString);
                String formattedTime = TimeUtil.getTimeRemainingString(timeMillis);

                plugin.sendMessage(sender, "&aSuccessfully muted " + target.getName()
                        + " for " + reason + " (" + formattedTime + ").");
            }
        }

        return true;
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

    public void register() {
        PluginCommand command = plugin.getCommand("mute");

        if (command != null)
            command.setExecutor(this);
    }
}

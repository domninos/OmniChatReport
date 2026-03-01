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
            String targetName = args[0];

            Player target = Bukkit.getPlayer(targetName);

            String timeString = args[1];

            StringBuilder builder = new StringBuilder();

            for (int i = 2; i < args.length; i++)
                builder.append(args[i]).append(" ");

            String reason = builder.toString().trim();

            long timeMillis = TimeUtil.parseDuration(timeString);
            String formattedTime = TimeUtil.getTimeRemainingString(timeMillis);

            if (target == null) {
                plugin.sendMessage(sender, "&cPlayer '" + targetName + "' not found. Trying on another server...");

                plugin.getRedisHandler().publishMute(sender.getName(), targetName, timeMillis, reason, plugin.checkServer());
            } else {
                plugin.getMuteManager().mutePlayer(sender.getName(), target, plugin.checkServer(), timeString, reason).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.sendMessage(sender, "&aSuccessfully muted " + target.getName()
                                + " for " + reason + ". Duration: " + formattedTime);

                        // broadcast
                        plugin.sendMessage(target, "&cYou have been muted by " + sender.getName() + " for " + formattedTime + ".");
                        plugin.sendMessage(target, "&cReason: " + reason);
                    });
                });
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
            if (!text.startsWith("{mute.")) {
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

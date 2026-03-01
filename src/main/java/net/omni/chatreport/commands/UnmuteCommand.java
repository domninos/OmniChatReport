package net.omni.chatreport.commands;

import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class UnmuteCommand implements CommandExecutor {
    private final OmniChatReport plugin;
    private final String[] help;

    public UnmuteCommand(OmniChatReport plugin) {
        this.plugin = plugin;

        this.help = new String[]{
                "{unmute.use} &b/unmute <player> &8>> &7Unmute player.",
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender.hasPermission("unmute.use")))
                return noPerms(sender);

            return sendHelp(sender);
        } else if (args.length == 1) {
            if (!(sender.hasPermission("unmute.use")))
                return noPerms(sender);

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null || !target.isOnline()) {
                plugin.sendMessage(sender, "&cPlayer not found.");
                return true;
            }

            plugin.getMuteManager().isMuted(target).thenAccept(isMuted -> {
                if (!isMuted) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.sendMessage(sender, "&c" + target.getName() + " is not muted.")
                    );
                    return;
                }

                plugin.getMuteManager().unmutePlayer(target.getName());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.sendMessage(target, "&aYou are now unmuted.");
                    plugin.sendMessage(sender, "&a" + target.getName() + " has been unmuted.");
                });

            });
        } else
            sendHelp(sender);

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
        PluginCommand command = Bukkit.getPluginCommand("unmute");

        if (command != null)
            command.setExecutor(this);
    }
}
